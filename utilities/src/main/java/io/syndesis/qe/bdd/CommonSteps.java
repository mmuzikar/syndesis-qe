package io.syndesis.qe.bdd;

import static org.assertj.core.api.Assertions.fail;

import io.syndesis.common.model.connection.Connection;
import io.syndesis.qe.Component;
import io.syndesis.qe.TestConfiguration;
import io.syndesis.qe.camelk.CamelKTemplate;
import io.syndesis.qe.endpoints.ConnectionsEndpoint;
import io.syndesis.qe.endpoints.TestSupport;
import io.syndesis.qe.templates.SyndesisTemplate;
import io.syndesis.qe.utils.HttpUtils;
import io.syndesis.qe.utils.OpenShiftUtils;
import io.syndesis.qe.utils.PublicApiUtils;
import io.syndesis.qe.utils.RestUtils;
import io.syndesis.qe.utils.TestUtils;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import cz.xtf.core.waiting.WaiterException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonSteps {
    @Autowired
    private ConnectionsEndpoint connectionsEndpoint;

    // Flag is the cluster reachability test failed, if it does, all remaining tests will be failed
    private static boolean isClusterReachable = true;

    @Given("^clean default namespace")
    public void cleanNamespace() {
        undeploySyndesis();
        //OCP4HACK - openshift-client 4.3.0 isn't supported with OCP4 and can't create/delete templates, following line can be removed later
        OpenShiftUtils.binary().execute("delete", "template", "--all");
        OpenShiftUtils.getInstance().apps().statefulSets().inNamespace(TestConfiguration.openShiftNamespace()).delete();
        OpenShiftUtils.getInstance().extensions().deployments().inNamespace(TestConfiguration.openShiftNamespace()).delete();
        OpenShiftUtils.getInstance().serviceAccounts().withName("syndesis-oauth-client").delete();
        try {
            OpenShiftUtils.getInstance().clean();
            OpenShiftUtils.getInstance().waiters().isProjectClean().waitFor();
        } catch (WaiterException e) {
            log.warn("Project was not clean after 20s, retrying once again");
            OpenShiftUtils.getInstance().clean();
            OpenShiftUtils.getInstance().waiters().isProjectClean().waitFor();
        }
        OpenShiftUtils.xtf().getTemplates().forEach(OpenShiftUtils.xtf()::deleteTemplate);
    }

    @Given("^clean all builds")
    public void cleanBuilds() {
        OpenShiftUtils.getInstance().getBuildConfigs().forEach(OpenShiftUtils.getInstance()::deleteBuildConfig);
        OpenShiftUtils.getInstance().getBuilds().forEach(OpenShiftUtils.getInstance()::deleteBuild);
    }

    @When("^deploy Syndesis$")
    public void deploySyndesis() {
        SyndesisTemplate.deploy();
    }

    @Then("^wait for Syndesis to become ready")
    public static void waitForSyndesis() {
        waitFor(true);
    }

    /**
     * Undeploys deployed syndesis resources.
     */
    public static void undeploySyndesis() {
        undeployCustomResources();
        if (TestUtils.isDcDeployed("syndesis-operator")) {
            CommonSteps.waitForUndeployment();
        }
    }

    public static void undeployCustomResources() {
        // if we don't have CRD, we can't have CRs
        if (SyndesisTemplate.getCrd() != null) {
            for (String s : SyndesisTemplate.getCrNames()) {
                undeployCustomResource(s);
            }
        }
    }

    /**
     * Undeploys syndesis custom resource using openshift API.
     *
     * @param name custom resource name
     */
    private static void undeployCustomResource(String name) {
        SyndesisTemplate.deleteCr();
    }

    /**
     * Waits for syndesis to be undeployed.
     */
    public static void waitForUndeployment() {
        waitFor(false);
    }

    /**
     * Waits for syndesis deployment / undeployment.
     *
     * @param deploy true if waiting for deploy, false otherwise
     */
    private static void waitFor(boolean deploy) {
        final int timeout = TestUtils.isJenkins() ? 20 : 12;
        EnumSet<Component> components = EnumSet.allOf(Component.class);

        ExecutorService executorService = Executors.newFixedThreadPool(components.size());
        components.forEach(c -> {
            Runnable runnable = () -> {
                if (deploy) {
                    OpenShiftUtils.xtf().waiters()
                        .areExactlyNPodsReady(1, "syndesis.io/component", c.getName())
                        .interval(TimeUnit.SECONDS, 20)
                        .timeout(TimeUnit.MINUTES, timeout)
                        .waitFor();
                } else {
                    OpenShiftUtils.xtf().waiters()
                        .areExactlyNPodsRunning(0, "syndesis.io/component", c.getName())
                        .interval(TimeUnit.SECONDS, 20)
                        .timeout(TimeUnit.MINUTES, timeout)
                        .waitFor();
                }
            };
            executorService.submit(runnable);
        });

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(timeout, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
                TestUtils.printPods();
                fail(deploy ? "Syndesis wasn't initialized in time" : "Syndesis wasn't undeployed in time");
            }
        } catch (InterruptedException e) {
            TestUtils.printPods();
            fail(deploy ? "Syndesis wasn't initialized in time" : "Syndesis wasn't undeployed in time");
        }
    }

    @Given("^clean application state")
    public void resetState() {
        waitUntilClusterIsReachable();
        //check that postgreSQl connection has been created
        int i = 0;
        while (i < 10) {
            TestSupport.getInstance().resetDB();
            Optional<Connection> optConnection = connectionsEndpoint.list().stream().filter(s -> s.getName().equals("PostgresDB")).findFirst();
            if (optConnection.isPresent()) {
                if (TestConfiguration.camelKEnabled()) {
                    CamelKTemplate.undeploy();
                }
                return;
            }
            i++;
        }
        fail("Default PostgresDB connection has not been created, please contact engineering!");
    }

    /**
     * Performs a simple reachability check in a loop.
     * <p>
     * Waits up to 30 minutes for the cluster to be reachable. The check is done using a simple HTTP GET to the cluster api endpoint
     */
    private void waitUntilClusterIsReachable() {
        if (!isClusterReachable) {
            fail("Previous reachability test failed, skipping remaining tests.");
        }
        final int maxRetries = 30;
        int retries = 0;
        boolean isReachable = false;
        log.info("Checking if OpenShift cluster at {} is reachable.", TestConfiguration.openShiftUrl());
        while (retries < maxRetries) {
            isReachable = HttpUtils.isReachable(TestConfiguration.openShiftUrl());
            if (isReachable) {
                log.info("  Cluster at {} is reachable.", TestConfiguration.openShiftUrl());
                break;
            } else {
                log.debug("  Cluster at {} is was not reachable. Retrying in 1 minute.", TestConfiguration.openShiftUrl());
                // The test takes 15 seconds when not available
                TestUtils.sleepIgnoreInterrupt(45000L);
                retries++;
            }
        }
        if (!isReachable) {
            isClusterReachable = false;
            fail("Unable to contact OpenShift cluster after " + maxRetries + " tries.");
        }
    }

    @Then("^sleep for jenkins delay or \"([^\"]*)\" seconds")
    public void sleepForJenkinsDelay(int secs) {
        TestUtils.sleepForJenkinsDelayIfHigher(secs);
    }

    @When("^refresh server port-forward")
    public void refreshPortForward() {
        RestUtils.reset();
        RestUtils.getRestUrl();
    }

    @Then("^wait for Todo to become ready$")
    public void waitForTodo() {
        log.info("Waiting for Todo to get ready");
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Runnable runnable = () ->
            OpenShiftUtils.xtf().waiters()
                .areExactlyNPodsReady(1, "syndesis.io/app", "todo")
                .interval(TimeUnit.SECONDS, 20)
                .timeout(TimeUnit.MINUTES, 12)
                .waitFor();
        executorService.submit(runnable);

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(20, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
                fail("Todo app wasn't initilized in time");
            }
        } catch (InterruptedException e) {
            fail("Waiting for Todo app was interrupted with exception: " + e.getMessage());
        }
    }

    @When("^set up ServiceAccount for Public API$")
    public void setUpServiceAccountForPublicAPI() {
        PublicApiUtils.createServiceAccount();
    }
}
