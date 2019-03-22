package io.syndesis.qe.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.data.MapEntry.entry;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.matchText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import static io.syndesis.qe.wait.OpenShiftWaitUtils.waitFor;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.cucumber.datatable.DataTable;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.syndesis.qe.CustomWebDriverProvider;
import io.syndesis.qe.TestConfiguration;
import io.syndesis.qe.accounts.Account;
import io.syndesis.qe.accounts.AccountsDirectory;
import io.syndesis.qe.fragments.common.form.Form;
import io.syndesis.qe.pages.ModalDialogPage;
import io.syndesis.qe.pages.SyndesisPage;
import io.syndesis.qe.pages.SyndesisRootPage;
import io.syndesis.qe.pages.connections.Connections;
import io.syndesis.qe.pages.login.GitHubLogin;
import io.syndesis.qe.pages.login.MinishiftLogin;
import io.syndesis.qe.pages.login.RHDevLogin;
import io.syndesis.qe.steps.connections.wizard.phases.ConfigureConnectionSteps;
import io.syndesis.qe.steps.connections.wizard.phases.NameConnectionSteps;
import io.syndesis.qe.steps.connections.wizard.phases.SelectConnectionTypeSteps;
import io.syndesis.qe.utils.AccountUtils;
import io.syndesis.qe.utils.CalendarUtils;
import io.syndesis.qe.utils.GoogleAccount;
import io.syndesis.qe.utils.GoogleAccounts;
import io.syndesis.qe.utils.OpenShiftUtils;
import io.syndesis.qe.utils.RestUtils;
import io.syndesis.qe.utils.TestUtils;
import io.syndesis.qe.wait.OpenShiftWaitUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonSteps {

    private SyndesisRootPage syndesisRootPage = new SyndesisRootPage();
    private ModalDialogPage modalDialogPage = new ModalDialogPage();

    private enum Credentials {
        DROPBOX,
        SALESFORCE,
        TWITTER,
        OPENSHIFT,
        SYNDESIS,
        SLACK,
        S3,
        FTP,
        GOOGLE_MAIL,
        SAP_CONCUR
    }

    private static class Element {
        public static final By LOGOUT_MENU = By.id("userMenuDropdownButton");
        public static final By LOGOUT_MENU_ACTION_TAG = By.tagName("a");
        public static final By LOGIN_BUTTON = By.className("btn");
    }

    @Autowired
    private SelectConnectionTypeSteps selectConnectionTypeSteps = new SelectConnectionTypeSteps();

    @Autowired
    private GoogleAccounts googleAccounts;

    @Autowired
    private CalendarUtils calendarUtils;

    @When("^log out from Syndesis")
    public void logout() {
        $(Element.LOGOUT_MENU).shouldBe(visible).click();
        $(Element.LOGOUT_MENU).shouldBe(visible).parent()
                .$$(Element.LOGOUT_MENU_ACTION_TAG).filter(matchText("(\\s*)" + "Logout" + "(\\s*)"))
                .shouldHaveSize(1).first().click();

        try {
            OpenShiftWaitUtils.waitFor(() -> WebDriverRunner.getWebDriver().getCurrentUrl().contains("/logout"), 20 * 1000);
        } catch (InterruptedException | TimeoutException e) {
            fail("Log out did not go as expected.");
        }

        TestUtils.sleepForJenkinsDelayIfHigher(3);
        $(Element.LOGIN_BUTTON).shouldBe(visible).click();

        try {
            OpenShiftWaitUtils.waitFor(() -> WebDriverRunner.getWebDriver().getCurrentUrl().contains("login"), 20 * 1000);
        } catch (InterruptedException | TimeoutException e) {
            fail("Log in page was never loaded");
        }
    }

    @Given("^log into the Syndesis$")
    public void login() {
        doLogin(false);
    }

    @Given("^log into the Syndesis after logout$")
    public void loginAfterLogOut() {
        doLogin(true);
    }

    /**
     * If you want to log in for the first time (browser is not opened) set afterLogout = false
     * If you want to log in again e.g. after logout (browser is already opened) set afterLogout = true
     *
     * @param afterLogout flag to indicate that browser is already open
     */
    private void doLogin(boolean afterLogout) {
        if (!afterLogout) {
            Selenide.open(TestConfiguration.syndesisUrl());
        }

        String currentUrl = WebDriverRunner.getWebDriver().getCurrentUrl();

        if (currentUrl.contains("api.fuse-ignite.openshift.com")) {

            //click only if there is Ignite cluster login page
            SelenideElement login = $(By.className("login-redhat"));
            if (login.isDisplayed()) {
                login.click();
            }

            RHDevLogin rhDevLogin = new RHDevLogin();
            rhDevLogin.login(TestConfiguration.syndesisUsername(), TestConfiguration.syndesisPassword());
        } else if (currentUrl.contains(":8443/login")) {
            log.info("Minishift cluster login page");

            MinishiftLogin minishiftLogin = new MinishiftLogin();
            minishiftLogin.login(TestConfiguration.syndesisUsername(), TestConfiguration.syndesisPassword());
        } else if (currentUrl.contains("github.com/login")) {

            GitHubLogin gitHubLogin = new GitHubLogin();
            gitHubLogin.login(TestConfiguration.syndesisUsername(), TestConfiguration.syndesisPassword());
        }

        currentUrl = WebDriverRunner.getWebDriver().getCurrentUrl();

        if (currentUrl.contains("oauth/authorize/approve")) {
            log.info("Authorize access login page");
            $("input[name=\"approve\"]").shouldBe(visible).click();
        }
    }

    @Given("^created connections$")
    public void createConnections(DataTable connectionsData) {
        Connections connectionsPage = new Connections();

        ConfigureConnectionSteps configureConnectionSteps = new ConfigureConnectionSteps();
        NameConnectionSteps nameConnectionSteps = new NameConnectionSteps();

        List<List<String>> dataTable = connectionsData.cells();

        for (List<String> dataRow : dataTable) {
            String connectionType = validateConnectorName(dataRow.get(0));
            String connectionCredentialsName = dataRow.get(1);
            String connectionName = dataRow.get(2);
            String connectionDescription = dataRow.get(3);

            if (connectionType.equalsIgnoreCase("Gmail") ||
                    connectionType.equalsIgnoreCase("Google Calendar")) {
                Account a = AccountUtils.get(connectionCredentialsName);
                GoogleAccount googleAccount = googleAccounts.getGoogleAccountForTestAccount(connectionCredentialsName);
                a.getProperties().put("accessToken", googleAccount.getCredential().getAccessToken());
            }

            navigateTo("Connections");
            validatePage("Connections");

            ElementsCollection connections = connectionsPage.getAllConnections();
            connections = connections.filter(exactText(connectionName));

            try {
                if (connections.size() != 0) {
                    log.warn("Connection {} already exists!", connectionName);
                    continue;
                }
            } catch (org.openqa.selenium.StaleElementReferenceException e) {
                //this may happen if page was "reloaded" before connections.size was processed, give it second try
                connections = connectionsPage.getAllConnections();
                connections = connections.filter(exactText(connectionName));
                if (connections.size() != 0) {
                    log.warn("Connection {} already exists!", connectionName);
                    continue;
                }
            }

            clickOnButton("Create Connection");

            log.info("Sleeping so jenkins has more time to load all connectors");
            TestUtils.sleepIgnoreInterrupt(TestConfiguration.getJenkinsDelay() * 1000);

            selectConnectionTypeSteps.selectConnectionType(connectionType);
            configureConnectionSteps.fillConnectionDetails(connectionCredentialsName);

            // do nothing if connection does not require any credentials
            if (!(connectionCredentialsName.equalsIgnoreCase("no credentials") ||
                    connectionDescription.equalsIgnoreCase("no validation"))) {

                clickOnButton("Validate");
                successNotificationIsPresentWithError(connectionType + " has been successfully validated.");
                scrollTo("top", "right");
                clickOnButton("Next");
            } else if (connectionDescription.equalsIgnoreCase("no validation")) {
                scrollTo("top", "right");
                clickOnButton("Next");
            }

            nameConnectionSteps.setConnectionName(connectionName);
            nameConnectionSteps.setConnectionDescription(connectionDescription);

            clickOnButton("Create");
        }
    }

    @And("^.*validate credentials$")
    public void validateCredentials() {
        Map<String, Account> accounts = AccountsDirectory.getInstance().getAccounts();
        List<List<String>> oneAccountList = new ArrayList<>();

        accounts.keySet().forEach(key -> {
            List<String> tmpList = new ArrayList<>();
            Optional<Account> currentAccount = AccountsDirectory.getInstance().getAccount(key);
            if (currentAccount.isPresent()) {

                String service = currentAccount.get().getService();
                Credentials current;
                try {
                    current = Credentials.valueOf(service.toUpperCase().replace(" ", "_"));
                } catch (IllegalArgumentException ex) {
                    log.error("Unable to find enum value for " + service.toUpperCase().replace(" ", "_") + " account. Skipping");
                    return;
                }

                switch (current) {
                    case DROPBOX:
                        service = "DropBox";
                        break;
                    case SALESFORCE:
                        //disable salesforce validation as it is not stable and smoke tests should be stable
                        return;
                    case TWITTER:
                        service = "Twitter";
                        break;
                    case S3:
                        return; //TODO: skip for now
                    case SLACK:
                        service = "Slack";
                        break;
                    case GOOGLE_MAIL:
                        service = "Gmail";
                        break;
                    case SAP_CONCUR:
                        //concur only works via OAUTH, there is another test fot it
                        return; //TODO: skip for now
                    default:
                        return; //skip for other cred
                }

                //type
                tmpList.add(service);
                //name
                tmpList.add(key);
                //connection name
                tmpList.add("my " + key + " connection");
                //description
                tmpList.add("some description");

                log.trace("Inserting: " + tmpList.toString());
                oneAccountList.add(new ArrayList<>(tmpList));
                log.trace("Current values in list list: " + oneAccountList.toString());
            }
        });

        log.debug("Final status of list: " + oneAccountList.toString());
        DataTable accountsTalbe = DataTable.create(oneAccountList);

        createConnections(accountsTalbe);
    }

    @When("^navigate to the \"([^\"]*)\" page$")
    public void navigateTo(String title) {
        try {
            OpenShiftWaitUtils.waitFor(() -> $(By.className("nav-pf-vertical")).exists(), 30 * 1000L);
        } catch (TimeoutException | InterruptedException e) {
            fail("Navigation panel was not found in 30s", e);
        }

        SelenideElement selenideElement = $(By.className("nav-pf-vertical")).shouldBe(visible);
        ElementsCollection allLinks = selenideElement.findAll(By.className("list-group-item-value"));
        allLinks.find(Condition.exactText(title)).shouldBe(visible).click();
    }

    @When("^.*navigates? to the \"([^\"]*)\" page in help menu$")
    public void navigateToHelp(String title) {
        SelenideElement helpDropdownMenu = $(By.id("helpDropdownButton")).shouldBe(visible);

        //open the help menu
        if (helpDropdownMenu.parent().$$(By.className("pf-c-dropdown__menu")).size() < 1) {
            helpDropdownMenu.click();
        }

        SelenideElement dropdownElementsTable = $(By.className("pf-c-dropdown__menu")).shouldBe(visible);
        ElementsCollection dropdownElements = dropdownElementsTable.findAll(By.tagName("a"))
                .shouldBe(CollectionCondition.sizeGreaterThanOrEqual(1));

        dropdownElements.filter(text(title)).shouldHaveSize(1).get(0).shouldBe(visible).click();

        //TODO: following if statement can be removed after
        //TODO: this issue gets fixed: https://github.com/syndesisio/syndesis/issues/4655
        //close the help menu
        if (helpDropdownMenu.parent().$$(By.className("pf-c-dropdown__menu")).size() >= 1) {
            helpDropdownMenu.click();
        }
    }

    @Then("^check visibility of Syndesis home page$")
    public void checkHomePageVisibility() {
        syndesisRootPage.getRootElement().shouldBe(visible);
    }

    @Then("^check visibility of the \"([^\"]*)\" link$")
    public void validateLink(String linkTitle) {
        new SyndesisRootPage().getLink(linkTitle).shouldBe(visible);
    }

    @When("^click? on the \"([^\"]*)\" button.*$")
    public void clickOnButton(String buttonTitle) {
        if (buttonTitle.equals("Done")) {
            // this is hack to replace Done with Next if not present
            try {
                syndesisRootPage.getRootElement().shouldBe(visible).findAll(By.tagName("button"))
                        .filter(matchText("(\\s*)" + buttonTitle + "(\\s*)")).first().waitUntil(visible, 10 * 1000);
            } catch (Throwable t) {
                buttonTitle = "Next";
            }
        }
        log.info(syndesisRootPage.getButton(buttonTitle).toString());
        TestUtils.sleepForJenkinsDelayIfHigher(2);
        syndesisRootPage.getButton(buttonTitle).shouldBe(visible, enabled).shouldNotHave(attribute("disabled")).click();
        TestUtils.sleepForJenkinsDelayIfHigher(2);
    }

    @When(".*clicks? on the modal dialog \"([^\"]*)\" button.*$")
    public void clickOnModalDialogButton(String buttonTitle) {
        modalDialogPage.getButton(buttonTitle).shouldBe(visible).click();
    }

    @When(".*clicks? on the \"([^\"]*)\" link.*$")
    public void clickOnLink(String linkTitle) {
        new SyndesisRootPage().getLink(linkTitle).shouldBe(visible).click();
    }

    // e.g. clicks on the "1". "Add a step" link.
    @When(".*clicks? on the \"(\\d+)\". \"([^\"]*)\" link.*$")
    public void clickOnLink(int position, String linkTitle) {
        int index = position - 1; // e.g. first position is 0 index
        new SyndesisRootPage().getLink(linkTitle, index).shouldBe(visible).click();
    }

    @When(".*clicks? on the random \"([^\"]*)\" link.*$")
    public void clickOnLinkRandom(String linkTitle) {
        new SyndesisRootPage().getLinkRandom(linkTitle);
    }

    @Then("^check visibility of the \"([^\"]*)\" button$")
    public void checkButtonIsVisible(String buttonTitle) {
        new SyndesisRootPage().getButton(buttonTitle).shouldBe(visible);
    }

    @Then("^check visibility of the \"([^\"]*)\" tables$")
    public void checkTableTitlesArePresent(String tableTitles) {

        String[] titles = tableTitles.split(",");

        for (String title : titles) {
            new SyndesisRootPage().getTitleByText(title).shouldBe(visible);
        }
    }

    public void expectTableTitlePresent(String tableTitle) {
        SelenideElement table = new SyndesisRootPage().getTitleByText(tableTitle);
        table.shouldBe(visible);
    }

    @Then("^check visibility of the \"([^\"]*)\" elements$")
    public void expectElementsPresent(String elementClassNames) {
        String[] elementClassNamesArray = elementClassNames.split(",");

        for (String elementClassName : elementClassNamesArray) {
            SelenideElement element = new SyndesisRootPage().getElementByClassName(elementClassName);
            element.shouldBe(visible);
        }
    }

    @Then("^check visibility of the \"([^\"]*)\"$")
    public void expectElementPresent(String elementClassName) {
        SelenideElement element = new SyndesisRootPage().getElementByClassName(elementClassName);
        element.shouldBe(visible);
    }

    @Then("^check visibility of success notification$")
    public void successNotificationIsPresent() {
        SelenideElement allertSucces = new SyndesisRootPage().getElementByClassName("alert-success");
        allertSucces.shouldBe(visible);
    }

    @Then("^check visibility of \"([^\"]*)\" in alert-success notification$")
    public void successNotificationIsPresentWithError(String textMessage) {
        SelenideElement successAlert = new SyndesisRootPage().getElementByClassName("alert-success");
        successAlert.shouldBe(visible);
        assertThat(successAlert.getText()).isEqualTo(textMessage);

        log.info("Text message {} was found.", textMessage);
    }

    @Then("^check visibility of alert notification$")
    public void checkSqlWarning() {
        SelenideElement allertSucces = new SyndesisRootPage().getElementByClassName("alert-warning");
        allertSucces.shouldBe(visible);
    }

    /**
     * Scroll the webpage.
     *
     * @param topBottom possible values: top, bottom
     * @param leftRight possible values: left, right
     */
    @When("^scroll \"([^\"]*)\" \"([^\"]*)\"$")
    public void scrollTo(String topBottom, String leftRight) {
        WebDriver driver = WebDriverRunner.getWebDriver();
        JavascriptExecutor jse = (JavascriptExecutor) driver;

        int x = 0;
        int y = 0;

        Long width = (Long) jse.executeScript("return $(document).width()");
        Long height = (Long) jse.executeScript("return $(document).height()");

        if (leftRight.equals("right")) {
            y = width.intValue();
        }

        if (topBottom.equals("bottom")) {
            x = height.intValue();
        }

        jse.executeScript("(browserX, browserY) => window.scrollTo(browserX, browserY)", x, y);
    }

    @Then("^check visibility of page \"([^\"]*)\"$")
    public void validatePage(String pageName) {
        SyndesisPage.get(pageName).validate();
    }

    @And("^select \"([^\"]*)\" from \"([^\"]*)\" dropdown$")
    public void selectsFromDropdown(String option, String selectId) {
        //search by name or by id because some dropdowns have only id
        SelenideElement selectElement = $(String.format("select[name=\"%s\"], select[id=\"%s\"]", selectId, selectId))
                .shouldBe(visible);
        selectElement.selectOption(option);
    }

    @Then("^sleep for \"(\\w+)\" ms$")
    public void sleep(Integer ms) {
        Selenide.sleep(ms);
    }

    @Then("^check \"([^\"]*)\" button is \"([^\"]*)\"$")
    public void sheCheckButtonStatus(String buttonTitle, String status) {
        new SyndesisRootPage().checkButtonStatus(buttonTitle, status);
    }

    @Then("^check visibility of dialog page \"([^\"]*)\"$")
    public void isPresentedWithDialogPage(String title) {
        String titleText = new ModalDialogPage().getTitleText();
        assertThat(titleText).isEqualToIgnoringCase(title);
    }

    @Then("^.*removes? file \"([^\"]*)\" if it exists$")
    public void removeFileIfExists(String fileName) throws Throwable {
        Files.deleteIfExists(Paths.get(CustomWebDriverProvider.DOWNLOAD_DIR + File.separator + fileName));
    }

    /**
     * When connector name is changed in the product just create new case here instead of changing it in the whole
     * testsuite string by string.
     *
     * @param name connector name
     * @return connector name
     */
    private String validateConnectorName(String name) {
        String finalName = name;
        switch (name) {
            case "DropBox":
                finalName = "Dropbox";
                break;
        }
        return finalName;
    }

    /**
     * This is only general form step that may work in most cases but it's better to use specific form-filling steps for each page
     *
     * @param data data
     */
    @Then("^.*fills? in values$")
    public void fillForm(DataTable data) {
        new Form(new SyndesisRootPage().getRootElement()).fillByLabel(data.asMap(String.class, String.class));
    }

    @Then("^.*fill in values by element ID")
    public void fillFormViaID(DataTable data) {
        new Form(new SyndesisRootPage().getRootElement()).fillById(data.asMap(String.class, String.class));
    }

    @When("^.*create connections using oauth$")
    public void createConnectionsUsingOAuth(DataTable connectionsData) {

        List<List<String>> dataTable = connectionsData.cells();

        for (List<String> dataRow : dataTable) {
            createConnectionUsingOAuth(dataRow.get(0), dataRow.get(1));
        }
    }

    @When("^.*create connection \"([^\"]*)\" with name \"([^\"]*)\" using oauth$")
    public void createConnectionUsingOAuth(String connectorName, String newConnectionName) {
        Connections connectionsPage = new Connections();
        NameConnectionSteps nameConnectionSteps = new NameConnectionSteps();

        navigateTo("Connections");
        validatePage("Connections");

        ElementsCollection connections = connectionsPage.getAllConnections();
        connections = connections.filter(exactText(newConnectionName));

        assertThat(connections.size()).as("Connection with name " + newConnectionName + " already exists!")
                .isEqualTo(0);

        clickOnButton("Create Connection");

        //sometimes page is loaded but connections are not so we need to wait here a bit
        TestUtils.sleepIgnoreInterrupt(TestConfiguration.getJenkinsDelay());

        selectConnectionTypeSteps.selectConnectionType(connectorName);

        //slenide did validation before it reached correct page, but lets wait a second (it helps, trust me!)
        TestUtils.sleepForJenkinsDelayIfHigher(1);
        doOAuthValidation(connectorName);

        assertThat(WebDriverRunner.currentFrameUrl())
                .containsIgnoringCase("Successfully%20authorized")
                .containsIgnoringCase("connections/create");

        nameConnectionSteps.setConnectionName(newConnectionName);

        clickOnButton("Create");
    }

    @When("^go back in browser history$")
    public void clickBrowserBackButton() {
        Selenide.back();
    }

    private void doOAuthValidation(String type) {
        clickOnButton("Connect " + type);
        switch (type) {
            case "Twitter":
                waitForCallbackRedirect("twitter");
                fillAndValidateTwitter();
                break;
            case "Salesforce":
                waitForCallbackRedirect("salesforce");
                fillAndValidateSalesforce();
                break;
            case "Gmail":
                loginToGoogleIfNeeded("QE Google Mail");
                break;
            case "Google Calendar":
                loginToGoogleIfNeeded("QE Google Calendar");
                break;
            case "SAP Concur":
                waitForCallbackRedirect("concursolutions");
                fillAndValidateConcur();
                break;
            case "Google Sheets":
                loginToGoogleIfNeeded("QE Google Sheets");
                break;

            default:
                fail("Unknown oauth option: " + type);
        }
        waitForCallbackRedirect(TestConfiguration.syndesisUrl());
    }

    private void loginToGoogleIfNeeded(String s) {
        // if the browser has previously logged into google account syndesis will
        // immediately move to next screen and will have "Successfully%20authorized%20Syndesis's%20access" in the URL
        if (!isStringInUrl("Successfully%20authorized%20Syndesis's%20access", 5)) {
            waitForCallbackRedirect("google");
            fillAndValidateGoogleAccount(s);
        }
    }

    private void fillAndValidateTwitter() {
        Optional<Account> account = AccountsDirectory.getInstance().getAccount("Twitter Listener");

        if (account.isPresent()) {
            $(By.id("username_or_email")).shouldBe(visible).sendKeys(account.get().getProperty("login"));
            $(By.id("password")).shouldBe(visible).sendKeys(account.get().getProperty("password"));
            $(By.id("allow")).shouldBe(visible).click();
        } else {
            fail("Credentials for Twitter were not found.");
        }
    }

    private void fillAndValidateSalesforce() {
        Optional<Account> account = AccountsDirectory.getInstance().getAccount("QE Salesforce");

        if (account.isPresent()) {

            $(By.id("username")).shouldBe(visible).sendKeys(account.get().getProperty("userName"));
            $(By.id("password")).shouldBe(visible).sendKeys(account.get().getProperty("password"));
            $(By.id("Login")).shouldBe(visible).click();
            //give it time to log in
            TestUtils.sleepForJenkinsDelayIfHigher(10);
            log.info(WebDriverRunner.currentFrameUrl());
            if (!WebDriverRunner.currentFrameUrl().contains("connections/create/review")) {
                $(By.id("oaapprove")).shouldBe(visible).click();
            }
        } else {
            fail("Credentials for Salesforce were not found.");
        }
    }

    private void fillAndValidateGoogleAccount(String googleAccount) {
        Optional<Account> account = AccountsDirectory.getInstance().getAccount(googleAccount);

        if (account.isPresent()) {

            $(By.id("identifierId")).shouldBe(visible).sendKeys(account.get().getProperty("email"));
            $(By.id("identifierNext")).shouldBe(visible).click();

            $(By.id("password")).shouldBe(visible).find(By.tagName("input")).sendKeys(account.get().getProperty("password"));
            $(By.id("passwordNext")).shouldBe(visible).click();
        } else {
            fail("Credentials for " + googleAccount + " were not found.");
        }
    }

    private void fillAndValidateConcur() {
        Optional<Account> account = AccountsDirectory.getInstance().getAccount("QE Concur");

        if (account.isPresent()) {

            $$(By.tagName("input")).stream().
                    filter((e) ->
                            e.getAttribute("name").equalsIgnoreCase("type") &&
                                    e.getAttribute("value").equalsIgnoreCase("username"))
                    .findFirst().get().click();
            $(By.id("userid")).shouldBe(visible).sendKeys(account.get().getProperty("userId"));
            $(By.xpath("//*[@type='submit']")).shouldBe(visible).click();
            TestUtils.sleepForJenkinsDelayIfHigher(3);
            $(By.id("password")).shouldBe(visible).sendKeys(account.get().getProperty("password"));
            $(By.xpath("//*[@type='submit']")).shouldBe(visible).click();
        } else {
            fail("Credentials for QE Concur were not found.");
        }
    }

    private void waitForCallbackRedirect(String expectedPartOfUrl) {
        waitForCallbackRedirect(expectedPartOfUrl, 60);
    }

    private void waitForCallbackRedirect(String expectedPartOfUrl, int timeoutSeconds) {
        try {
            waitFor(() -> WebDriverRunner.currentFrameUrl().contains(expectedPartOfUrl.toLowerCase()), 60 * 1000);
        } catch (InterruptedException | TimeoutException e) {
            fail("Error while redirecting to " + expectedPartOfUrl, e);
        }
    }

    private void waitForStringInUrl(String expectedPartOfUrl, int timeoutSeconds) throws TimeoutException, InterruptedException {
        waitFor(() -> {
            String frameUrl = WebDriverRunner.currentFrameUrl().toLowerCase();
            return frameUrl.contains(expectedPartOfUrl.toLowerCase());
        }, timeoutSeconds * 1000);
    }

    private boolean isStringInUrl(String toFind, int timeoutSeconds) {
        boolean result = false;
        try {
            waitForStringInUrl(toFind, timeoutSeconds);
            result = true;
        } catch (InterruptedException | TimeoutException e) {
        }
        return result;
    }

    @When("^set 3scale discovery variable to \"([^\"]*)\"")
    public void set3scaleEnvVar(String value) {
        DeploymentConfig dc = OpenShiftUtils.getInstance().getDeploymentConfig("syndesis-server");

        List<EnvVar> vars = dc.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();

        Optional<EnvVar> exposeVia3Scale = vars.stream().filter(a -> a.getName().equalsIgnoreCase("CONTROLLERS_EXPOSE_VIA3SCALE")).findFirst();
        if (exposeVia3Scale.isPresent()) {
            exposeVia3Scale.get().setValue(value);
        } else {
            fail("variable CONTROLLERS_EXPOSE_VIA3SCALE not found in deployment config of syndesis-server");
        }

        dc.getSpec().getTemplate().getSpec().getContainers().get(0).setEnv(vars);
        OpenShiftUtils.getInstance().updateDeploymentconfig(dc);

        try {
            OpenShiftWaitUtils.waitForPodIsReloaded("server");
            OpenShiftWaitUtils.waitFor(() -> OpenShiftUtils.getPodByPartialName("server").isPresent(), 300 * 1000L);
            OpenShiftWaitUtils.waitFor(() -> OpenShiftWaitUtils.isPodReady(OpenShiftUtils.getPodByPartialName("server").get()), 60 * 1000 * 10L);
        } catch (InterruptedException | TimeoutException e) {
            fail("Server was not reloaded after deployment config change", e);
        }
        // even though server is in ready state, inside app is still starting so we have to wait a lot just to be sure
        try {
            OpenShiftWaitUtils.waitFor(() -> OpenShiftUtils.getPodLogs("server").contains("Started Application in"), 1000 * 300L);
        } catch (TimeoutException | InterruptedException e) {
            fail("Syndesis server did not start in 300s with new variable", e);
        }

        RestUtils.reset();
        Selenide.refresh();
    }

    @Then("^check that 3scale annotations are present on integration \"([^\"]*)\"")
    public void check3scaleAnnotations(String integrationName) {
        Map<String, String> annotations = OpenShiftUtils.getInstance().getService(("i-" + integrationName).toLowerCase()).getMetadata().getAnnotations();

        assertThat(annotations)
                .contains(entry("discovery.3scale.net/description-path", "/openapi.json"))
                .contains(entry("discovery.3scale.net/port", "8080"))
                .contains(entry("discovery.3scale.net/scheme", "http"));
    }

    //TODO: should be refactored after asmigala is done with api provider tests
    @When("^select first api provider operation$")
    public void clickFirstOperation() {
        $$(By.className("list-pf-title")).first().shouldBe(visible).click();
    }

    /**
     * Save current time to the singleton class
     */
    @Then("^save time before request$")
    public void saveBeforeTime() {
        calendarUtils.setBeforeRequest(Calendar.getInstance());
        log.info("Time before request was saved: "
                + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(calendarUtils.getBeforeRequest().getTime()));
        sleep(1000);
    }

    /**
     * Save current time to the singleton class
     */
    @Then("^save time after request$")
    public void saveAfterTime() {
        calendarUtils.setAfterRequest(Calendar.getInstance());
        log.info("Time after request was saved: "
                + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(calendarUtils.getAfterRequest().getTime()));
    }
}

