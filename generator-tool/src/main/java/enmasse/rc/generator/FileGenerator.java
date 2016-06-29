package enmasse.rc.generator;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IResource;
import enmasse.rc.model.BrokerProperties;
import enmasse.rc.model.Config;
import enmasse.rc.model.LabelKeys;
import enmasse.rc.model.parser.ConfigParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * @author lulf
 */
public class FileGenerator {

    private final ConfigParser parser = new ConfigParser();
    private final ConfigGenerator generator = new ConfigGenerator(null, new BrokerProperties.Builder().build());

    private static final String BROKER_CLUSTER_PATTERN = "broker_cluster_%s.json";

    public void generate(String inputFilePath, String outputDirPath) throws IOException {
        File inputFile = new File(inputFilePath);
        File outputDir = new File(outputDirPath);

        try (FileReader reader = new FileReader(inputFile)) {
            Config config = parser.parse(reader);
            generateConfig(config, outputDir);
        }
    }

    private void generateConfig(Config config, File outputDir) throws IOException {
        List<IResource> resources = generator.generate(config);
        for (IResource resource : resources) {
            if (ResourceKind.DEPLOYMENT_CONFIG.equals(resource.getKind())) {
                generateBroker((IDeploymentConfig)resource, outputDir);
            }
        }
    }

    private void generateBroker(IDeploymentConfig resource, File outputDir) throws IOException {
        String address = resource.getLabels().get(LabelKeys.ADDRESS);
        File brokerFile = new File(outputDir, String.format(BROKER_CLUSTER_PATTERN, address));
        writeBrokerConfig(resource, brokerFile);
    }

    private void writeBrokerConfig(IDeploymentConfig replicationController, File brokerFile) throws IOException {
        try (FileWriter writer = new FileWriter(brokerFile)) {
            writer.write(replicationController.toJson(false));
        }
    }
}
