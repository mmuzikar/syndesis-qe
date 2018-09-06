package io.syndesis.qe.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.openshift.api.model.Build;
import io.syndesis.common.model.connection.Connection;
import io.syndesis.qe.Component;
import io.syndesis.qe.TestConfiguration;
import io.syndesis.qe.endpoints.ConnectionsEndpoint;
import io.syndesis.qe.endpoints.TestSupport;
import io.syndesis.qe.templates.SyndesisTemplate;
import io.syndesis.qe.utils.LogCheckerUtils;
import io.syndesis.qe.utils.OpenShiftUtils;
import io.syndesis.qe.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonSteps {
    @Autowired
    private ConnectionsEndpoint connectionsEndpoint;

    @Given("^clean default namespace")
    public void cleanNamespace() {
        OpenShiftUtils.client().apps().statefulSets().inNamespace(TestConfiguration.openShiftNamespace()).delete();
        OpenShiftUtils.client().extensions().deployments().inNamespace(TestConfiguration.openShiftNamespace()).delete();
        OpenShiftUtils.client().serviceAccounts().withName("syndesis-oauth-client").delete();
        OpenShiftUtils.getInstance().cleanAndAssert();
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

    @When("^deploy Syndesis from template$")
    public void deploySyndesisFromTemplate() {
        SyndesisTemplate.deployUsingTemplate();
    }

    @Then("^wait for Syndesis to become ready")
    public void waitForSyndesis() {
        EnumSet<Component> components = EnumSet.allOf(Component.class);

        ExecutorService executorService = Executors.newFixedThreadPool(components.size());
        components.forEach(c -> {
            Runnable runnable = () ->
                    OpenShiftUtils.xtf().waiters()
                            .areExactlyNPodsReady(1, "syndesis.io/component", c.getName())
                            .interval(TimeUnit.SECONDS, 20)
                            .timeout(TimeUnit.MINUTES, 12)
                            .assertEventually();
            executorService.submit(runnable);
        });

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(20, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
                fail("Syndesis wasn't initilized in time");
            }
        } catch (InterruptedException e) {
            fail("Syndesis wasn't initilized in time");
        }
    }

    @Then("^verify s2i build of integration \"([^\"]*)\" was finished in duration (\\d+) min$")
    public void verifyBuild(String integrationName, int duration) {
        String sanitizedName = integrationName.toLowerCase().replaceAll(" ", "-");

        Optional<Build> s2iBuild = OpenShiftUtils.getInstance().getBuilds().stream().filter(b -> b.getMetadata().getName().contains(sanitizedName)).findFirst();

        if (s2iBuild.isPresent()) {
            Build build = s2iBuild.get();
            String buildPodName = build.getMetadata().getAnnotations().get("openshift.io/build.pod-name");
            Optional<Pod> buildPod = OpenShiftUtils.getInstance().getPods().stream().filter(p -> p.getMetadata().getName().equals(buildPodName)).findFirst();
            if (buildPod.isPresent()) {
                try {
                    boolean[] patternsInLogs = LogCheckerUtils.findPatternsInLogs(buildPod.get(), Pattern.compile(".*Downloading: \\b.*"));
                    assertThat(patternsInLogs).containsOnly(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            assertThat(build.getStatus().getPhase()).isEqualTo("Complete");
            // % 1_000L is there to parse OpenShift ms format
            assertThat(build.getStatus().getDuration() % 1_000L).isLessThan(duration * 60 * 1000);
        } else {
            fail("No build found for integration with name " + sanitizedName);
        }
    }

    @Given("^clean application state")
    public void resetState() {
        //check that postgreSQl connection has been created
        int i = 0;
        while (i < 10) {
            TestSupport.getInstance().resetDB();
            Optional<Connection> optConnection = connectionsEndpoint.list().stream().filter(s -> s.getName().equals("PostgresDB")).findFirst();
            if (optConnection.isPresent()) {
                return;
            }
            i++;
        }
        fail("Default PostgresDB connection has not been created, please contact engineering!");
    }

    @Then("^sleep for jenkins delay or \"([^\"]*)\" seconds")
    public void sleepForJenkinsDelay(int secs) {
        TestUtils.sleepForJenkinsDelayIfHigher(secs);
    }
}
