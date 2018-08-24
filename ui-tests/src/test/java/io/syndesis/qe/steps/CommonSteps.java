package io.syndesis.qe.steps;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import cucumber.api.DataTable;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.syndesis.qe.CustomWebDriverProvider;
import io.syndesis.qe.TestConfiguration;
import io.syndesis.qe.accounts.Account;
import io.syndesis.qe.accounts.AccountsDirectory;
import io.syndesis.qe.fragments.common.form.CoupledForm;
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
import io.syndesis.qe.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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
        S3
    }

    @Autowired
    private SelectConnectionTypeSteps selectConnectionTypeSteps = new SelectConnectionTypeSteps();

    @And("^.*logs? out from Syndesis")
    public void logout() {
        $(By.id("userDropdown")).shouldBe(visible).click();
        $(By.id("userDropdownMenu")).shouldBe(visible).click();

        TestUtils.sleepIgnoreInterrupt(2000);

        Assertions.assertThat(WebDriverRunner.getWebDriver().getCurrentUrl())
                .containsIgnoringCase("login");
    }


    @Given("^\"([^\"]*)\" logs into the Syndesis$")
    public void login(String username) throws Throwable {
        doLogin(username, false);
    }

    @Given("^\"([^\"]*)\" logs into the Syndesis after logout$")
    public void loginAfterLogOut(String username) throws Throwable {
        doLogin(username, true);
    }

    /**
     * If you want to log in for the first time (browser is not opened) set afterLogout = false
     * If you want to log in again e.g. after logout (browser is already opened) set afterLogout = true
     *
     * @param username
     * @param afterLogout
     * @throws Throwable
     */
    private void doLogin(String username, boolean afterLogout) throws Throwable {
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

        List<List<String>> dataTable = connectionsData.raw();

        for (List<String> dataRow : dataTable) {
            String connectionType = validateConnectorName(dataRow.get(0));
            String connectionCredentialsName = dataRow.get(1);
            String connectionName = dataRow.get(2);
            String connectionDescription = dataRow.get(3);

            navigateTo("", "Connections");
            validatePage("", "Connections");

            ElementsCollection connections = connectionsPage.getAllConnections();
            connections = connections.filter(exactText(connectionName));

            if (connections.size() != 0) {
                log.warn("Connection {} already exists!", connectionName);
            } else {
                clickOnButton("Create Connection");

                log.info("Sleeping so jenkins has more time to load all connectors");
                TestUtils.sleepIgnoreInterrupt(TestConfiguration.getJenkinsDelay() * 1000);

                selectConnectionTypeSteps.selectConnectionType(connectionType);
                configureConnectionSteps.fillConnectionDetails(connectionCredentialsName);

                // do nothing if connection does not require any credentials
                if (!connectionCredentialsName.equalsIgnoreCase("no credentials")) {

                    clickOnButton("Validate");
                    successNotificationIsPresentWithError(connectionType + " has been successfully validated");
                    scrollTo("top", "right");
                    clickOnButton("Next");
                }


                nameConnectionSteps.setConnectionName(connectionName);
                nameConnectionSteps.setConnectionDescription(connectionDescription);

                clickOnButton("Create");
            }
        }
    }

    @And("^.*validate credentials$")
    public void validateCredentials() {
        Map<String, Account> accounts = AccountsDirectory.getInstance().getAccounts();
        List<List<String>> oneAccountList = new ArrayList<>();
        List<String> tmpList = Arrays.asList("a", "s", "d", "f");

        accounts.keySet().forEach(key -> {
            Optional<Account> currentAccount = AccountsDirectory.getInstance().getAccount(key);
            if (currentAccount.isPresent()) {

                String service = currentAccount.get().getService();
                try {
                    Credentials current = Credentials.valueOf(service.toUpperCase());

                    switch (current) {
                        case DROPBOX:
                            service = "DropBox";
                            break;
                        case SALESFORCE:
                            service = "Salesforce";
                            break;
                        case TWITTER:
                            service = "Twitter";
                            break;
                        case S3:
                            return; //TODO: skip for now
                        case SLACK:
                            service = "Slack";
                            break;
                        default:
                            return; //skip for other cred
                    }


                //type
                tmpList.set(0, service);
                //name
                tmpList.set(1, key);
                //connection name
                tmpList.set(2, "my " + key + " connection");
                //description
                tmpList.set(3, "some description");

                log.trace("Inserting: " + tmpList.toString());
                oneAccountList.add(new ArrayList<>(tmpList));
                log.trace("Current values in list list: " + oneAccountList.toString());
                } catch (Exception ex) { ;
                }
            }
        });

        log.debug("Final status of list: " + oneAccountList.toString());
        DataTable accountsTalbe = DataTable.create(oneAccountList);

        createConnections(accountsTalbe);
    }

    @When("^\"([^\"]*)\" navigates? to the \"([^\"]*)\" page$")
    public void navigateTo(String username, String title) {
        SelenideElement selenideElement = $(By.className("nav-pf-vertical")).shouldBe(visible);
        ElementsCollection allLinks = selenideElement.findAll(By.className("list-group-item-value"));
        allLinks.find(Condition.exactText(title)).shouldBe(visible).click();
    }

    @When("^.*navigates? to the \"([^\"]*)\" page in help menu$")
    public void navigateToHelp(String title) {
        SelenideElement helpDropdownMenu = $(By.className("help")).shouldBe(visible);

        if (!helpDropdownMenu.getAttribute("class").contains("open")) {
            helpDropdownMenu.click();
        }

        SelenideElement dropdownElementsTable = $(By.className("dropdown-menu")).shouldBe(visible);
        ElementsCollection dropdownElements = dropdownElementsTable.findAll(By.tagName("a"))
                .shouldBe(CollectionCondition.sizeGreaterThanOrEqual(1));

        dropdownElements.filter(text(title)).shouldHaveSize(1).get(0).shouldBe(visible).click();
    }

    @Then("^\"(\\w+)\" is presented with the Syndesis home page$")
    public void checkHomePageVisibility(String username) {
        syndesisRootPage.getRootElement().shouldBe(visible);
    }

    @Then("^(\\w+)? is presented with the \"([^\"]*)\" link$")
    public void validateLink(String alias, String linkTitle) {
        new SyndesisRootPage().getLink(linkTitle).shouldBe(visible);
    }

    @When(".*clicks? on the \"([^\"]*)\" button.*$")
    public void clickOnButton(String buttonTitle) {
        if (buttonTitle.equals("Done")) {
            // this is hack to replace Done with Next if not present
            try {
                syndesisRootPage.getRootElement().shouldBe(visible).findAll(By.tagName("button"))
                        .filter(Condition.matchText("(\\s*)" + buttonTitle + "(\\s*)")).first().waitUntil(visible, 10 * 1000);
            } catch (Throwable t) {
                buttonTitle = "Next";
            }
        }
        log.info(syndesisRootPage.getButton(buttonTitle).toString());
        syndesisRootPage.getButton(buttonTitle).shouldBe(visible, enabled).shouldNotHave(attribute("disabled")).click();
    }

    @When(".*clicks? on the modal dialog \"([^\"]*)\" button.*$")
    public void clickOnModalDialogButton(String buttonTitle) {
        modalDialogPage.getButton(buttonTitle).shouldBe(visible).click();
    }

    @When(".*clicks? on the \"([^\"]*)\" link.*$")
    public void clickOnLink(String linkTitle) {
        new SyndesisRootPage().getLink(linkTitle).shouldBe(visible).click();
    }

    @When(".*clicks? on the random \"([^\"]*)\" link.*$")
    public void clickOnLinkRandom(String linkTitle) {
        new SyndesisRootPage().getLinkRandom(linkTitle);
    }

    @Then("^she is presented with the \"([^\"]*)\" button$")
    public void checkButtonIsVisible(String buttonTitle) {
        new SyndesisRootPage().getButton(buttonTitle).shouldBe(visible);
    }

    @Then("^she is presented with the \"([^\"]*)\" tables$")
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

    @Then("^she is presented with the \"([^\"]*)\" elements$")
    public void expectElementsPresent(String elementClassNames) {
        String[] elementClassNamesArray = elementClassNames.split(",");

        for (String elementClassName : elementClassNamesArray) {
            SelenideElement element = new SyndesisRootPage().getElementByClassName(elementClassName);
            element.shouldBe(visible);
        }
    }

    @Then("^she is presented with the \"([^\"]*)\"$")
    public void expectElementPresent(String elementClassName) {
        SelenideElement element = new SyndesisRootPage().getElementByClassName(elementClassName);
        element.shouldBe(visible);
    }

    @Then("^she can see success notification$")
    public void successNotificationIsPresent() {
        SelenideElement allertSucces = new SyndesisRootPage().getElementByClassName("alert-success");
        allertSucces.shouldBe(visible);
    }

    @Then("^she can see \"([^\"]*)\" in alert-success notification$")
    public void successNotificationIsPresentWithError(String textMessage) {
        SelenideElement allertSucces = new SyndesisRootPage().getElementByClassName("alert-success");
        allertSucces.shouldBe(visible);
        Assertions.assertThat(allertSucces.getText().equals(textMessage)).isTrue();

        log.info("Text message {} was found.", textMessage);
    }

    @Then("^she can see alert notification$")
    public void checkSqlWarning() throws Throwable {
        SelenideElement allertSucces = new SyndesisRootPage().getElementByClassName("alert-warning");
        allertSucces.shouldBe(visible);
    }

    /**
     * Scroll the webpage.
     *
     * @param topBottom possible values: top, bottom
     * @param leftRight possible values: left, right
     * @returns {Promise<any>}
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

    @Then("^(\\w+) is presented with the Syndesis page \"([^\"]*)\"$")
    public void validatePage(String userName, String pageName) {
        SyndesisPage.get(pageName).validate();
    }

    @And("^she selects \"([^\"]*)\" from \"([^\"]*)\" dropdown$")
    public void selectsFromDropdown(String option, String selectId) throws Throwable {
        SelenideElement selectElement = $(String.format("select[name=\"%s\"]", selectId)).shouldBe(visible);
        selectElement.selectOption(option);
    }

    @Then("^she stays there for \"(\\w+)\" ms$")
    public void sleep(Integer ms) {
        Selenide.sleep(ms);
    }

    @Then("^she checks \"([^\"]*)\" button is \"([^\"]*)\"$")
    public void sheCheckButtonStatus(String buttonTitle, String status) throws Throwable {
        new SyndesisRootPage().checkButtonStatus(buttonTitle, status);
    }

    @Then("^she is presented with dialog page \"([^\"]*)\"$")
    public void isPresentedWithDialogPage(String title) throws Throwable {
        String titleText = new ModalDialogPage().getTitleText();
        assertThat(titleText.equals(title), is(true));
    }


    @Then("^.*removes? file \"([^\"]*)\" if it exists$")
    public void removeFileIfExists(String fileName) throws Throwable {
        Files.deleteIfExists(Paths.get(CustomWebDriverProvider.DOWNLOAD_DIR + File.separator + fileName));
    }

    /**
     * When connector name is changed in the product just create new case here instead of changing it in the whole
     * testsuite string by string.
     *
     * @param name
     * @return
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

    @Then("^.*fills? in values$")
    public void fillForm(DataTable data) {
        new CoupledForm(new SyndesisRootPage().getRootElement()).fill(data.asMap(String.class, String.class));
    }
}
