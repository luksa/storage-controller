package enmasse.rc.openshift;

import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IDeploymentConfig;
import enmasse.rc.model.LabelKeys;
import enmasse.rc.model.Roles;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author lulf
 */
public class OpenshiftClient {
    private static final Logger log = Logger.getLogger(OpenshiftClient.class.getName());
    private final IClient client;
    private final String namespace;

    public OpenshiftClient(IClient client, String namespace) {
        this.client = client;
        this.namespace = namespace;
    }

    public void createBroker(IDeploymentConfig controller) {
        log.log(Level.INFO, "Adding controller " + controller.getName());
        client.create(controller, namespace);
    }

    public void deleteBroker(IDeploymentConfig controller) {
        log.log(Level.INFO, "Deleting controller " + controller.getName());
        client.delete(controller);
    }

    public void updateBroker(IDeploymentConfig controller) {
        log.log(Level.INFO, "Updating controller " + controller.getName());
        client.update(controller);
    }

    public List<IDeploymentConfig> listBrokers() {
        return client.list(ResourceKind.DEPLOYMENT_CONFIG, namespace, Collections.singletonMap(LabelKeys.ROLE, Roles.BROKER));
    }

    public IDeploymentConfig getBroker(String name) {
        return client.get(ResourceKind.DEPLOYMENT_CONFIG, name, namespace);
    }
}
