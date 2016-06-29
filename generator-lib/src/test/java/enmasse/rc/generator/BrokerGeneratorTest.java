package enmasse.rc.generator;

import com.openshift.restclient.model.IContainer;
import com.openshift.restclient.model.IDeploymentConfig;
import org.junit.Before;
import org.junit.Test;
import enmasse.rc.model.BrokerProperties;
import enmasse.rc.model.Destination;
import enmasse.rc.model.EnvVars;
import enmasse.rc.model.LabelKeys;
import enmasse.rc.model.Roles;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author lulf
 */
public class BrokerGeneratorTest {
    private BrokerGenerator generator;

    @Before
    public void setup() {
        generator = new BrokerGenerator(null, new BrokerProperties.Builder().brokerPort(1234).build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowOnNoStore() {
        generator.generate(new Destination("testaddr", false, false));
    }

    @Test
    public void testGenerator() {
        IDeploymentConfig deployment = generator.generate(new Destination("testaddr", true, false));

        assertThat(deployment.getName(), is("deployment-testaddr"));
        assertThat(deployment.getLabels().get(LabelKeys.ROLE), is(Roles.BROKER));
        assertThat(deployment.getContainers().size(), is(2));

        IContainer broker = deployment.getContainer("broker");
        assertThat(broker.getPorts().size(), is(1));
        assertThat(broker.getPorts().iterator().next().getContainerPort(), is(1234));
        assertThat(broker.getEnvVars().get(EnvVars.QUEUE_NAME), is("testaddr"));
        assertThat(broker.getVolumeMounts().size(), is(1));

        IContainer router = deployment.getContainer("router");
        assertThat(router.getPorts().size(), is(1));
        assertThat(router.getPorts().iterator().next().getContainerPort(), is(5672));
    }

    @Test
    public void testGenerateTopic() {
        IDeploymentConfig deployment = generator.generate(new Destination("testaddr", true, true));
        IContainer broker = deployment.getContainer("broker");
        assertThat(broker.getEnvVars().get(EnvVars.TOPIC_NAME), is("testaddr"));
    }
}
