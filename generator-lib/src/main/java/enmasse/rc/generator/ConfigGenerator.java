package enmasse.rc.generator;

import com.openshift.restclient.IClient;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IResource;
import enmasse.rc.model.BrokerProperties;
import enmasse.rc.model.Destination;
import enmasse.rc.model.Config;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lulf
 */
public class ConfigGenerator {

    private final BrokerGenerator brokerGenerator;

    public ConfigGenerator(IClient osClient, BrokerProperties properties) {
        this.brokerGenerator = new BrokerGenerator(osClient, properties);
    }

    public List<IResource> generate(Config config) {
        return config.destinations().stream()
                .filter(Destination::storeAndForward)
                .map(brokerGenerator::generate)
                .collect(Collectors.toList());
    }

    public IDeploymentConfig generateBroker(Destination destination) {
        return brokerGenerator.generate(destination);
    }
}
