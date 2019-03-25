package io.syndesis.qe;

import static org.junit.Assume.assumeFalse;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import io.fabric8.kubernetes.api.model.Pod;
import io.syndesis.qe.bdd.storage.StepsStorage;
import io.syndesis.qe.utils.OpenShiftUtils;
import io.syndesis.qe.utils.SampleDbConnectionManager;
import io.syndesis.qe.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Mar 19, 2018 Red Hat
 *
 * @author tplevko@redhat.com
 */
@Slf4j
public class RestTestHooks {

    @Autowired
    private StepsStorage stepStorage;

    @Before("@syndesis-upgrade")
    public void skipProdUpgrade() {
        // Prod upgrade is tested differently, so skip these tests with prod version
        assumeFalse(System.getProperty("syndesis.version").contains("redhat"));
    }

    @After
    public void afterTest() {
        stepStorage.flushStepDefinitions();
        log.debug("Flushed steps from steps storage");
        SampleDbConnectionManager.closeConnections();
    }

    @After("@syndesis-upgrade")
    public void clearUpgrade() {
        // Restore syndesis version if it was changed by previous upgrade test
        if (System.getProperty("syndesis.upgrade.backup.version") != null) {
            System.setProperty("syndesis.version", System.getProperty("syndesis.upgrade.backup.version"));
        }
        System.clearProperty("syndesis.upgrade.version");
        System.clearProperty("syndesis.upgrade.rollback");
    }

    @After
    public void getLogs(Scenario scenario){
        if (scenario.isFailed()) {
            TestUtils.printPods();
            log.warn("Scenario {} failed, saving server logs and integration logs to scenario", scenario.getName());
            scenario.embed(OpenShiftUtils.getInstance().getPodLog(OpenShiftUtils.getPodByPartialName("syndesis-server").get()).getBytes(), "text/plain");
            // There can be multiple integration pods for one test
            List<Pod> integrationPods = OpenShiftUtils.client().pods().list().getItems().stream().filter(
                    p -> p.getMetadata().getName().startsWith("i-")
                    && !p.getMetadata().getName().contains("deploy")
                    && !p.getMetadata().getName().contains("build")
            ).collect(Collectors.toList());
            for (Pod integrationPod : integrationPods) {
                scenario.embed(String.format("%s\n\n%s", integrationPod.getMetadata().getName(),
                        OpenShiftUtils.getInstance().getPodLog(integrationPod)).getBytes(), "text/plain");
            }
        }
    }
}
