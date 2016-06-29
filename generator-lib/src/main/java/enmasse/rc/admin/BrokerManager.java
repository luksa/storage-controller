package enmasse.rc.admin;

import com.openshift.restclient.model.IContainer;
import com.openshift.restclient.model.IPort;
import com.openshift.restclient.model.IDeploymentConfig;
import enmasse.rc.generator.ConfigGenerator;
import enmasse.rc.model.Destination;
import enmasse.rc.model.LabelKeys;
import enmasse.rc.openshift.OpenshiftClient;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The {@link BrokerManager} maintains the number of broker replication deployments to be consistent with the number of destinations in config that require store_and_forward.
 *
 * @author lulf
 */
public class BrokerManager {
    private static final Logger log = Logger.getLogger(BrokerManager.class.getName());

    private final OpenshiftClient openshiftClient;
    private final ConfigGenerator generator;

    public BrokerManager(OpenshiftClient openshiftClient, ConfigGenerator generator) {
        this.openshiftClient = openshiftClient;
        this.generator = generator;
    }

    public void destinationsUpdated(Collection<Destination> newDestinations) {
        List<IDeploymentConfig> currentBrokers = openshiftClient.listBrokers();
        Collection<Destination> destinations = newDestinations.stream()
                .filter(Destination::storeAndForward)
                .collect(Collectors.toList());
        log.log(Level.INFO, "Brokers got updated to " + destinations.size() + " destinations, we have " + currentBrokers.size() + " destinations: " + currentBrokers.stream().map(IDeploymentConfig::getName).toString());
        createBrokers(currentBrokers, destinations);
        deleteBrokers(currentBrokers, destinations);
        updateBrokers(currentBrokers, destinations);
    }

    private void createBrokers(Collection<IDeploymentConfig> currentBrokers, Collection<Destination> newDestinations) {
        newDestinations.stream()
                .filter(broker -> !currentBrokers.stream().filter(deployment -> broker.address().equals(deployment.getLabels().get(LabelKeys.ADDRESS))).findAny().isPresent())
                .map(generator::generateBroker)
                .forEach(openshiftClient::createBroker);
    }

    private void deleteBrokers(Collection<IDeploymentConfig> currentBrokers, Collection<Destination> newDestinations) {
        currentBrokers.stream()
                .filter(deployment -> !newDestinations.stream().filter(broker -> broker.address().equals(deployment.getLabels().get(LabelKeys.ADDRESS))).findAny().isPresent())
                .map(deployment -> {
                    deployment.setReplicas(0);
                    openshiftClient.updateBroker(deployment);
                    return deployment;
                })
                .forEach(openshiftClient::deleteBroker);
    }

    private void updateBrokers(Collection<IDeploymentConfig> currentBrokers, Collection<Destination> newDestinations) {
        newDestinations.stream()
                .filter(broker -> currentBrokers.stream().filter(deployment -> broker.address().equals(deployment.getLabels().get(LabelKeys.ADDRESS))).findAny().isPresent())
                .map(generator::generateBroker)
                .forEach(this::brokerModified);
    }

    private void brokerModified(IDeploymentConfig deployment) {
        IDeploymentConfig oldDeployment = openshiftClient.getBroker(deployment.getName());
        if (!equivalent(deployment, oldDeployment)) {
            log.log(Level.INFO, "Modifying replication deployment " + deployment.getName());
            oldDeployment.setContainers(deployment.getContainers());
            oldDeployment.setReplicaSelector(deployment.getReplicaSelector());

            for (Map.Entry<String, String> label : deployment.getLabels().entrySet()) {
                oldDeployment.addLabel(label.getKey(), label.getValue());
            }
            openshiftClient.updateBroker(oldDeployment);
        }
    }

    private static boolean equivalent(IDeploymentConfig a, IDeploymentConfig b) {
        return equivalent(a.getContainers(), b.getContainers())
            && a.getLabels().equals(b.getLabels())
            && a.getReplicaSelector().equals(b.getReplicaSelector());
    }

    private static boolean equivalent(Collection<IContainer> a, Collection<IContainer> b) {
        return a.size() == b.size() && equivalent(index(a), index(b));
    }

    private static boolean equivalent(Map<String, IContainer> a, Map<String, IContainer> b) {
        if (a.size() != b.size()) return false;
        for (String name : a.keySet()) {
            if (!equivalent(a.get(name), b.get(name))) {
                return false;
            }
        }
        return true;
    }

    private static boolean equivalent(IContainer a, IContainer b) {
        if (a == null) return b == null;
        else if (b != null) return false;
        else return a.getImage().equals(b.getImage())
                 && a.getEnvVars().equals(b.getEnvVars())
                 && equivalent(a.getPorts(), b.getPorts())
                 && a.getVolumeMounts().equals(b.getVolumeMounts());
    }

    private static boolean equivalent(Set<IPort> a, Set<IPort> b) {
        if (a.size() != b.size()) return false;
        else return index(a).equals(index(b));
    }

    private static Map<String, IContainer> index(Collection<IContainer> in) {
        return in.stream().collect(Collectors.toMap(c -> c.getName(), c -> c));
    }

    private static Map<Integer, String> index(Set<IPort> in) {
        return in.stream().collect(Collectors.toMap(p -> p.getContainerPort(), p -> p.getName()));
    }
}
