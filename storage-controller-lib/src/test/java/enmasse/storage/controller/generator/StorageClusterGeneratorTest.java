package enmasse.storage.controller.generator;

import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.volume.VolumeType;
import enmasse.storage.controller.model.FlavorConfig;
import enmasse.storage.controller.model.Destination;
import enmasse.storage.controller.model.StorageConfig;
import enmasse.storage.controller.openshift.OpenshiftClient;
import enmasse.storage.controller.openshift.StorageCluster;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author lulf
 */
public class StorageClusterGeneratorTest {
    private OpenshiftClient mockClient;
    private FlavorConfig flavorConfig;

    @Before
    public void setUp() {
        mockClient = mock(OpenshiftClient.class);
        flavorConfig = new FlavorConfig.Builder().build();
    }

    @Test
    public void testSkipNoStore() {
        StorageGenerator generator = new StorageGenerator(mockClient);
        List<StorageCluster> resources = generator.generate(Arrays.asList(new Destination("foo", true, false, flavorConfig), new Destination("bar", false, false, flavorConfig)));
        assertThat(resources.size(), is(1));
    }

    @Test
    public void testGenerate() {
        FlavorConfig properties = new FlavorConfig.Builder().brokerPort(1234).storage(new StorageConfig(VolumeType.PERSISTENT_VOLUME_CLAIM, "10Gi", "/mnt")).build();
        StorageGenerator generator = new StorageGenerator(mockClient);
        List<StorageCluster> clusterList = generator.generate(Arrays.asList(new Destination("foo", true, false, properties), new Destination("bar", false, false, properties)));
        assertThat(clusterList.size(), is(1));
        StorageCluster cluster = clusterList.get(0);
        assertThat(cluster.getAddress(), is("foo"));
        assertThat(cluster.getResources().size(), is(2));
        IReplicationController controller = getController(cluster.getResources());
        assertThat(controller.getContainer("broker").getPorts().iterator().next().getContainerPort(), is(1234));
    }

    private IReplicationController getController(List<IResource> resources) {
        return resources.stream()
                .filter(resource -> resource instanceof IReplicationController)
                .map(resource -> (IReplicationController)resource)
                .findAny()
                .get();
    }
}
