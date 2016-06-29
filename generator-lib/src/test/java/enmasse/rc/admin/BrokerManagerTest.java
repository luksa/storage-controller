package enmasse.rc.admin;

import com.openshift.internal.restclient.model.DeploymentConfig;
import com.openshift.restclient.model.IDeploymentConfig;
import enmasse.rc.generator.ConfigGenerator;
import enmasse.rc.model.BrokerProperties;
import enmasse.rc.model.Destination;
import enmasse.rc.openshift.OpenshiftClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author lulf
 */
public class BrokerManagerTest {
    private OpenshiftClient mockClient;
    private BrokerManager manager;

    @Before
    public void setUp() {
        mockClient = mock(OpenshiftClient.class);
        manager = new BrokerManager(mockClient, new ConfigGenerator(null, new BrokerProperties.Builder().build()));
    }

    @Test
    public void testModifiedBrokerDoesNotResetReplicaCount() {
        // Create simple queue and capture generated deployment
        ArgumentCaptor<IDeploymentConfig> arg = ArgumentCaptor.forClass(IDeploymentConfig.class);

        Destination queue = new Destination("myqueue", true, false);
        manager.destinationsUpdated(Collections.singletonList(queue));
        verify(mockClient).createBroker(arg.capture());

        IDeploymentConfig deployment = arg.getValue();
        when(mockClient.getBroker("deployment-myqueue")).thenReturn(deployment);
        when(mockClient.listBrokers()).thenReturn(Collections.singletonList(deployment));

        // Modify replicas and update deployment
        deployment.setReplicas(3);
        Destination modifiedQueue = new Destination("myqueue", true, true);
        manager.destinationsUpdated(Collections.singletonList(modifiedQueue));

        verify(mockClient).updateBroker(arg.capture());
        assertThat(arg.getValue().getReplicas(), is(3));
    }
}
