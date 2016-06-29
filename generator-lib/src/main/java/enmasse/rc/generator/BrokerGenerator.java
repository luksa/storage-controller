package enmasse.rc.generator;

import com.openshift.internal.restclient.ResourceFactory;
import com.openshift.internal.restclient.model.DeploymentConfig;
import com.openshift.internal.restclient.model.Port;
import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IDeploymentConfig;
import enmasse.rc.model.*;
import org.jboss.dmr.ModelNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author lulf
 */
public class BrokerGenerator {
    private static final Logger log = Logger.getLogger(BrokerGenerator.class.getName());
    private final ResourceFactory factory;
    private final BrokerProperties properties;

    public BrokerGenerator(IClient osClient, BrokerProperties properties) {
        this.factory = new ResourceFactory(osClient);
        this.properties = properties;
    }

    public IDeploymentConfig generate(Destination destination) {
        if (!destination.storeAndForward()) {
            throw new IllegalArgumentException("Not generating broker for destination, storeAndForward = " + destination.storeAndForward());
        }

        DeploymentConfig deployment = factory.create("v1", ResourceKind.DEPLOYMENT_CONFIG);
        // TODO: sanitize address
        deployment.setName("controller-" + destination.address());
        deployment.setReplicas(1);
        deployment.addLabel(LabelKeys.ROLE, Roles.BROKER);
        deployment.addLabel(LabelKeys.ADDRESS, destination.address());
        deployment.addTemplateLabel(LabelKeys.ROLE, Roles.BROKER);
        deployment.addTemplateLabel(LabelKeys.CAPABILITY, Capabilities.ROUTER);
        deployment.setReplicaSelector(Collections.singletonMap(LabelKeys.ADDRESS, destination.address()));

        generateBroker(deployment, destination);
        generateDispatchRouter(deployment, destination);

        return deployment;
    }

    private void generateBroker(DeploymentConfig deployment, Destination destination) {

        Port amqpPort = new Port(new ModelNode());
        amqpPort.setContainerPort(properties.brokerPort());
        Map<String, String> env = new LinkedHashMap<>();
        env.put(destination.multicast() ? EnvVars.TOPIC_NAME : EnvVars.QUEUE_NAME, destination.address());
        env.put(EnvVars.BROKER_PORT, String.valueOf(properties.brokerPort()));

        deployment.addContainer(
                "broker",
                properties.brokerImage(),
                Collections.singleton(amqpPort),
                env,
                properties.brokerMounts());
    }

    private void generateDispatchRouter(DeploymentConfig deployment, Destination destination) {
        Port interRouterPort = new Port(new ModelNode());
        interRouterPort.setContainerPort(properties.routerPort());

        Map<String, String> env = new LinkedHashMap<>();
        env.put(destination.multicast() ? EnvVars.TOPIC_NAME : EnvVars.QUEUE_NAME, destination.address());
        env.put(EnvVars.INTERROUTER_PORT, String.valueOf(properties.routerPort()));
        deployment.addContainer(
                "router",
                properties.routerImage(),
                Collections.singleton(interRouterPort),
                env,
                Collections.emptyList());
    }
}
