package io.syndesis.qe.steps.integrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import static com.codeborne.selenide.Condition.visible;

import org.assertj.core.api.Condition;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.SelenideElement;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.cucumber.datatable.DataTable;
import io.syndesis.qe.pages.ModalDialogPage;
import io.syndesis.qe.pages.integrations.IntegrationStartingStatus;
import io.syndesis.qe.pages.integrations.Integrations;
import io.syndesis.qe.pages.integrations.editor.add.steps.DataMapper;
import io.syndesis.qe.pages.integrations.importt.ImportIntegration;
import io.syndesis.qe.pages.integrations.summary.Details;
import io.syndesis.qe.steps.CommonSteps;
import io.syndesis.qe.utils.ExportedIntegrationJSONUtil;
import io.syndesis.qe.utils.OpenShiftUtils;
import io.syndesis.qe.utils.TestUtils;
import io.syndesis.qe.wait.OpenShiftWaitUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by sveres on 11/15/17.
 */
@Slf4j
public class IntegrationSteps {

    private Details detailPage = new Details();
    private Integrations integrations = new Integrations();
    private DataMapper dataMapper = new DataMapper();
    private ImportIntegration importIntegration = new ImportIntegration();

    @Autowired
    private CommonSteps commonSteps;

    @When("^select the \"([^\"]*)\" integration.*$")
    public void selectIntegration(String itegrationName) {
        integrations.goToIntegrationDetail(itegrationName);
    }

    @When("^delete the \"([^\"]*)\" integration.*$")
    public void deleteIntegration(String integrationName) {
        integrations.deleteIntegration(integrationName);
    }

    @Then("^Integration \"([^\"]*)\" is present in integrations list$")
    public void expectIntegrationPresent(String name) {
        commonSteps.navigateTo("Integrations");
        log.info("Verifying integration {} is present", name);
        TestUtils.sleepForJenkinsDelayIfHigher(4);
        assertThat(integrations.isIntegrationPresent(name)).isTrue();
    }

    @Then("^check that integration \"([^\"]*)\" is not visible$")
    public void expectIntegrationNotPresent(String name) {
        // DOES NOT WORK - THROWS ERROR AS getElement has assert that its > 0 and if it does not exists its 0 TODO
        log.info("Verifying if integration {} is present", name);
        assertThat(integrations.isIntegrationPresent(name)).isFalse();
    }

    /*
     * State != starting state
     * State is state of integration. e.g. "Running", "Stopped"
     */
    @Then("^wait until integration \"([^\"]*)\" gets into \"([^\"]*)\" state$")
    public void waitForIntegrationState(String integrationName, String integrationStatus) {
        commonSteps.navigateTo("Integrations");
        SelenideElement integration = integrations.getIntegration(integrationName);
        TestUtils.sleepForJenkinsDelayIfHigher(10);
        assertThat(TestUtils.waitForEvent(
                status -> status.equals(integrationStatus),
                () -> integrations.getIntegrationItemStatus(integration),
                TimeUnit.MINUTES, 10, TimeUnit.SECONDS, 20)
        ).isTrue();
    }

    /*
     * State != starting state
     * Starting state is state during publication of integration e.g.
     * "Assembling ( 1 / 4 )", "Building ( 2 / 4 )" ,"Deploying ( 3 / 4 )", "Starting ( 4 / 4 )"
     */
    @Then("^wait until integration \"([^\"]*)\" starting status gets into \"([^\"]*)\" state$")
    public void waitForIntegrationStartingState(String integrationName, String integrationStatus) {
        commonSteps.navigateTo("Integrations");
        SelenideElement integration = integrations.getIntegration(integrationName);
        TestUtils.sleepForJenkinsDelayIfHigher(10);
        assertThat(TestUtils.waitForEvent(
                status -> status.equals(integrationStatus),
                () -> integrations.getIntegrationItemStartingStatus(integration),
                TimeUnit.MINUTES, 10, TimeUnit.MILLISECONDS, 30)
        ).isTrue();
    }

    //Kebab menu test, #553 -> part #548, #549.
    @When("^click on the kebab menu icon of each available Integration and checks whether menu is visible and has appropriate actions$")
    public void clickOnAllKebabMenus() {
        integrations.checkAllIntegrationsKebabButtons();
    }

    @And("^export the integrat?ion$")
    public void exportIntegration() throws InterruptedException {
        File exportedIntegrationFile = detailPage.exportIntegration();
        assertThat(exportedIntegrationFile)
                .exists()
                .isFile()
                .has(new Condition<>(f -> f.length() > 0, "File size should be greater than 0"));
        ExportedIntegrationJSONUtil.testExportedFile(exportedIntegrationFile);
    }

    @And("^import the integration from file ([^\"]*)$")
    public void importIntegration(String filePath) {
        importIntegration.importIntegration(new File(getClass().getClassLoader().getResource(filePath).getFile()));
    }

    @And("^start integration \"([^\"]*)\"$")
    public void startIntegration(String integrationName) {
        detailPage.clickOnKebabMenuAction("Start");
        ModalDialogPage modal = new ModalDialogPage();
        modal.getButton("Start").shouldBe(visible).click();
    }

    @And("^Wait until there is no integration pod with name \"([^\"]*)\"$")
    public void waitForIntegrationPodShutdown(String integartionPodName) throws InterruptedException {
        OpenShiftWaitUtils.assertEventually("Pod with name " + integartionPodName + "is still running.",
                OpenShiftWaitUtils.areNoPodsPresent(integartionPodName), 1000, 5 * 60 * 1000);
    }

    @And("^.*check that data bucket \"([^\"]*)\" is available$")
    public void checkPreviousDataBuckets(String bucket) {
        //there is condition for element to be visible
        dataMapper.getDataBucketElement(bucket);
    }

    @And("^.*open data bucket \"([^\"]*)\"$")
    public void openDataBucket(String bucket) {
        //check if it exists included
        dataMapper.openBucket(bucket);
    }

    @And("^.*close data bucket \"([^\"]*)\"$")
    public void closeDataBucket(String bucket) {
        dataMapper.closeBucket(bucket);
    }

    @And("^.*perform action with data bucket")
    public void performActionWithBucket(DataTable table) {
        List<List<String>> rows = table.cells();
        String action;

        for (List<String> row : rows) {
            action = row.get(1);
            if (action.equalsIgnoreCase("open")) {
                dataMapper.openBucket(row.get(0));
            } else if (action.equalsIgnoreCase("close")) {
                dataMapper.closeBucket(row.get(0));
            } else {
                //check if exists, condition visible is in used method
                dataMapper.getDataBucketElement(row.get(0));
            }
        }
    }

    @Then("^.*validate that logs of integration \"([^\"]*)\" contains string \"(.*)\"$")
    public void checkThatLogsContain(final String integrationName, final String text) {
        try {
            OpenShiftWaitUtils.waitFor(() -> OpenShiftUtils.getIntegrationLogs(integrationName).contains(text), 60 * 1000L);
        } catch (TimeoutException | InterruptedException e) {
            assertThat(OpenShiftUtils.getIntegrationLogs(integrationName)).containsIgnoringCase(text);
        }
    }

    @Then("^.*validate that logs of integration \"([^\"]*)\" doesn't contain string \"(.*)\"$")
    public void checkThatLogsDoesNotContain(final String integrationName, final String text) {
        try {
            OpenShiftWaitUtils.waitFor(() -> OpenShiftUtils.getIntegrationLogs(integrationName).contains(text), 60 * 1000L);
            fail("Log for integration: " + integrationName + " contains string: " + text);
        } catch (TimeoutException | InterruptedException e) {
            assertThat(OpenShiftUtils.getIntegrationLogs(integrationName)).doesNotContain(text);
        }
    }

    @Then("^check starting integration ([^\"]*) status on Integrations page$")
    public void checkStartingStatusOnIntegrationsPage(String integrationName) {
        checkStartingStatus(integrationName, "Integrations");
    }

    //base starting status checking method used in step methods
    public void checkStartingStatus(String integrationName, String checkedPage) {
        List<IntegrationStartingStatus> statuses = new ArrayList<>();
        statuses.add(IntegrationStartingStatus.ASSEMBLING);
        statuses.add(IntegrationStartingStatus.BUILDING);
        statuses.add(IntegrationStartingStatus.DEPLOYING);
        statuses.add(IntegrationStartingStatus.STARTING);

        int lastStatusIndex = 0;
        int matchingStatesNumber = 0;
        StringBuilder statusesMessage = new StringBuilder("");
        String lastStatus = "";

        //polling every 200 ms for 10 minutes
        for (int i = 0; i < 5 * 60 * 10; i++) {
            if (lastStatusIndex == statuses.size() - 1) {
                switch (checkedPage) {
                    case "Home":
                    case "Integrations":
                        log.info("Status changed to: " + integrations.getIntegrationItemStatus(integrations.getIntegration(integrationName)).trim());
                        assertThat(integrations.getIntegrationItemStatus(integrations.getIntegration(integrationName)).trim()).isEqualToIgnoringWhitespace("Running");
                        break;
                    case "Integration detail":
                        log.info("Status changed to: " + detailPage.getPublishedVersion().getText().trim());
                        assertThat(detailPage.getPublishedVersion().getText()).isEqualToIgnoringWhitespace("Published version 1");
                        break;
                    default:
                        fail("Integration status can't be checked on <" + checkedPage + "> page. Only valid options are [Integrations, Integration detail, Home]");
                }
                break;
            }

            String status = "";
            try {
                switch (checkedPage) {
                    case "Home":
                    case "Integrations":
                        status = integrations.getIntegrationItemStartingStatus(integrations.getIntegration(integrationName));
                        break;
                    case "Integration detail":
                        status = detailPage.getStartingStatus();
                        break;
                    default:
                        fail("Integration status can't be checked on <" + checkedPage + "> page. Only valid options are [Integrations, Integration detail, Home]");
                }
            } catch (Throwable t) {
                lastStatusIndex = statuses.size() - 1;
                continue;
            }

            if (!lastStatus.equals(status)) {
                lastStatus = status;
                statusesMessage.append(" " + status);
            }

            for (int j = lastStatusIndex; j < statuses.size(); j++) {
                if (statuses.get(j).getStatus().equals(status)) {
                    if (matchingStatesNumber == 0) {
                        matchingStatesNumber++;
                        lastStatusIndex = j;
                        log.info("Status changed to: " + status);
                    } else {
                        if (lastStatusIndex < j) {
                            matchingStatesNumber++;
                            lastStatusIndex = j;
                            log.info("Status changed to: " + status);
                        }
                    }
                } else {
                }
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        assertThat(matchingStatesNumber).isGreaterThanOrEqualTo(2).withFailMessage("Spotted statuses' order:" + statusesMessage.toString());
    }

    @Then("^verify there are ([0-9]+) flows in the integration$")
    public void verifyThereAreNFlowsInTheIntegration(int numFlows) throws Throwable {
        assertThat(detailPage.getFlowCount()).isEqualTo(numFlows);
    }

    @When("^edit integration$")
    public void editIntegration() throws Throwable {
        detailPage.editIntegration();
    }
}
