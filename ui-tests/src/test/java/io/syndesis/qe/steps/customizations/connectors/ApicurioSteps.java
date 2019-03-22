package io.syndesis.qe.steps.customizations.connectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThanOrEqual;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.executeJavaScript;

import org.openqa.selenium.By;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import java.util.concurrent.TimeoutException;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.syndesis.qe.utils.TestUtils;
import io.syndesis.qe.wait.OpenShiftWaitUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApicurioSteps {

    private static class Elements {
        public static final By SYNDESIS_ROOT = By.cssSelector("syndesis-root");

        //apicurio gui elements
        public static By WARNING_ICON = By.className("validation-icon");
        public static By PROBLEMS_CONTAINER = By.className("editor-problem-drawer");
        public static By OPERATIONS_CONTAINER = By.className("editor-outline");
        public static By PATH_SECTION = By.className("path-section");

        public static By VALIDATION_PROBLEM = By.className("drawer-pf-notification");
        public static By INFO_SECTION = By.cssSelector("operation-info-section");
        public static By ADD_OPERATION = By.cssSelector("button.icon-button");
        public static By OPERATION_KEBAB = By.id("dropdownKebab");
        public static By OPERATION = By.className("api-path");
        public static By OPERATION_KEBAB_MENU = By.className("detail-actions");
        public static By OPERATION_KEBAB_MENU_DELETE = By.xpath("//span[contains(text(), \"Delete Path\")]");

        public static By RESPONSE_SECTION = By.className("responses-section");
        public static By MARKDOWN_EDITOR = By.className("inline-markdown-editor-label");

        //security elements
        public static By SECURITY_SECTION = By.className("security-section");
        public static By BUTTON_ADD_SCHEME = By.cssSelector("button[title*='Add a security scheme']");
        public static By BUTTON_ADD_REQUIREMENT = By.cssSelector("button[title*='Add a security requirement']");

        //modal dialog elements
        public static By MODAL_DIALOG = By.className("modal-dialog");
        public static By MODAL_FOOTER = By.className("modal-footer");
        public static By MODAL_SUBMIT_ADD = By.xpath("//button[contains(text(), \"Add\")]");
        public static By MODAL_PATH_INPUT = By.id("path");

        //syndesis apicurio-review page elements
        public static By WARNINGS = By.className("openapi-review-actions__label--warning");
        public static By ERRORS = By.className("openapi-review-actions__label--error");
        public static By OPERATIONS = By.className("openapi-review-actions");
        public static By OPERATION_ITEM = By.tagName("li");

        //specify security elements
        public static By AUTHENTICATION_CONTAINER = By.tagName("syndesis-api-connector-auth");
    }

    private static class TextFormElements {
        //apicurio text editing elements
        public static By INPUT_TEXT = By.cssSelector("input.form-control");
        public static By SUMMARY = By.className("summary");
        public static By SAVE = By.cssSelector("button[title*='Save changes.']");
    }

    private static class SecurityPageElements {
        public static By NAME = By.id("securitySchemeName");
        public static By SAVE = By.xpath("//button[contains(text(), 'Save')]");
        public static By EDITOR = By.tagName("security-scheme-editor");
        public static By SECURITY_TYPE_DROPDOWN = By.className("dropdown-toggle");
        public static By SECURITY_DROPDOWN_MENU = By.className("dropdown-menu");
        public static By ACTION_HEADER = By.className("action-header");

        //security requirements page
        public static By SECURITY_REQUIREMENT = By.xpath("//*[contains(.,'ImmovableName')]");
        public static By SECURITY_REQUIREMENT_ITEMS = By.className("list-group-item-heading");
    }

    @Then("^check that apicurio shows (\\d+) imported operations$")
    public void verifyOperations(int expectedCount) {
        SelenideElement operations = $(Elements.OPERATIONS).shouldBe(visible).$$(Elements.OPERATION_ITEM).get(0);
        assertThat(operations).isNotNull();
        assertThat(operations.getText())
                .containsIgnoringCase(Integer.toString(expectedCount))
                .containsIgnoringCase("operations");
    }

    @When("^check that apicurio imported operations number is loaded$")
    public void verifyOperationsAreVisible() {
        try {
            OpenShiftWaitUtils.waitFor(() -> !$(Elements.OPERATIONS).shouldBe(visible).$$(Elements.OPERATION_ITEM).get(0)
                    .getText().equalsIgnoreCase("{{0}} operations"), 1000 * 60);
        } catch (InterruptedException | TimeoutException e) {
            fail("Operations number was not loaded in 60s.", e);
        }
    }

    @Then("^check that apicurio shows (\\d+) warnings$")
    public void verifyWarnings(int expectedCount) {
        SelenideElement operations = $(Elements.WARNINGS).shouldBe(visible);
        assertThat(operations).isNotNull();
        assertThat(operations.getText())
                .containsIgnoringCase(Integer.toString(expectedCount));
    }

    @Then("^check that apicurio shows (\\d+) errors?$")
    public void verifyErrors(int expectedCount) {
        SelenideElement operations = $(Elements.ERRORS).shouldBe(visible);
        assertThat(operations).isNotNull();
        assertThat(operations.getText())
                .containsIgnoringCase(Integer.toString(expectedCount));
    }

    @When("^remove warning via apicurio gui$")
    public void removeWarning() {
        $(Elements.WARNING_ICON).shouldBe(visible).click();
        //there isn't really a nice way how to wait as the box is there all the time, we are waiting for different items to show
        TestUtils.sleepForJenkinsDelayIfHigher(10);
        SelenideElement firstProblemElement = $(Elements.PROBLEMS_CONTAINER).shouldBe(visible)
                .$$(Elements.VALIDATION_PROBLEM).get(0);
        assertThat(firstProblemElement).isNotNull();
        assertThat(firstProblemElement.text()).containsIgnoringCase("Operation Summary should be less than 120 characters");

        try {
            firstProblemElement.shouldBe(visible).$(By.tagName("a")).shouldBe(visible).click();
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            $(Elements.PROBLEMS_CONTAINER).shouldBe(visible)
                    .$$(Elements.VALIDATION_PROBLEM).get(0).shouldBe(visible).$(By.tagName("a")).shouldBe(visible).click();
        }

        $(Elements.WARNING_ICON).shouldBe(visible).click();
        $(Elements.INFO_SECTION).shouldBe(visible).$(TextFormElements.SUMMARY).shouldBe(visible).click();

        SelenideElement input = $(Elements.INFO_SECTION).$(TextFormElements.INPUT_TEXT).shouldBe(visible);

        try {
            input.clear();
            input.sendKeys("Short description");
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            $(Elements.INFO_SECTION).$(TextFormElements.INPUT_TEXT).shouldBe(visible).clear();
            $(Elements.INFO_SECTION).$(TextFormElements.INPUT_TEXT).shouldBe(visible)
                    .sendKeys("Short description");
        }

        $(Elements.INFO_SECTION).$(TextFormElements.SAVE).shouldBe(visible).click();
        $(Elements.WARNING_ICON).shouldBe(visible).getText();
        assertThat($(Elements.WARNING_ICON).shouldBe(visible).getText()).containsIgnoringCase("57");
    }

    @When("^add an operation via apicurio gui$")
    public void addOperation() {
        doAddOperation(false);
    }

    @When("^add an operation with error via apicurio gui$")
    public void addOperationWithError() {
        doAddOperation(true);
    }

    /**
     * Add an operation for apicurito path.
     * If we want to see an error in review, we do not fill operation description
     *
     * @param withError - true to invoke an error in syndesis apicurito review due to empty description field
     */
    public void doAddOperation(boolean withError) {
        $(Elements.OPERATIONS_CONTAINER).shouldBe(visible).$(Elements.ADD_OPERATION).shouldBe(visible).click();
        SelenideElement pathInput = $(Elements.MODAL_DIALOG).shouldBe(visible).$(Elements.MODAL_PATH_INPUT).shouldBe(visible);

        try {
            pathInput.clear();
            pathInput.sendKeys("/syndesistestpath");
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            $(Elements.MODAL_DIALOG).shouldBe(visible).$(Elements.MODAL_PATH_INPUT).shouldBe(visible).clear();
            $(Elements.MODAL_DIALOG).shouldBe(visible).$(Elements.MODAL_PATH_INPUT).shouldBe(visible)
                    .sendKeys("/syndesistestpath");
        }

        $(Elements.MODAL_FOOTER).shouldBe(visible).$(Elements.MODAL_SUBMIT_ADD).shouldBe(visible).click();
        clickOnButtonInApicurio("Add Operation");

        $(Elements.RESPONSE_SECTION).shouldBe(visible).scrollIntoView(true)
                .$$(By.tagName("button")).filter(Condition.attribute("title", "Add a response to the operation."))
                .shouldHaveSize(1).first().click();

        $(Elements.MODAL_SUBMIT_ADD).shouldBe(visible).click();

        if (!withError) {
            $(Elements.RESPONSE_SECTION).shouldBe(visible).scrollIntoView(true)
                    .$(Elements.MARKDOWN_EDITOR).shouldBe(visible)
                    .click();

            executeJavaScript(
                    "document.getElementsByTagName(\"ace-editor\").item(0).setAttribute(\"id\", \"editor\");" +
                            "ace.edit(\"editor\").setValue('description here');"
            );

            $(Elements.RESPONSE_SECTION).shouldBe(visible).scrollIntoView(true)
                    .$$(By.tagName("button")).filter(Condition.attribute("title", "Save changes."))
                    .shouldHaveSize(1).first().click();
        }
    }

    @When("^remove an operation via apicurio gui$")
    public void removeOperation() {
        $(Elements.PATH_SECTION).shouldBe(visible).$(Elements.OPERATION).shouldBe(visible).click();
        $(Elements.OPERATION_KEBAB).shouldBe(visible).click();
        SelenideElement kebabMenu = $(Elements.OPERATION_KEBAB_MENU);
        try {
            kebabMenu.$(Elements.OPERATION_KEBAB_MENU_DELETE).shouldBe(visible).click();
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            $(Elements.OPERATION_KEBAB_MENU)
                    .$(Elements.OPERATION_KEBAB_MENU_DELETE).shouldBe(visible)
                    .click();
        }
    }

    @When("^add security schema (BASIC|API Key|OAuth 2) via apicurio gui$")
    public void addSecuritySchema(String schemeType) {
        //child span element selected so the click is inside of the element
        $(Elements.SECURITY_SECTION).shouldBe(visible).$(By.tagName("span")).click();
        $(Elements.SECURITY_SECTION).shouldBe(visible).$(Elements.BUTTON_ADD_SCHEME).shouldBe(visible).click();

        SelenideElement nameInput = $(SecurityPageElements.NAME).shouldBe(visible);

        try {
            nameInput.sendKeys("ImmovableName");
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            $(SecurityPageElements.NAME).shouldBe(visible)
                    .sendKeys("ImmovableName");
        }

        //select security option
        $(SecurityPageElements.EDITOR).shouldBe(visible).$(SecurityPageElements.SECURITY_TYPE_DROPDOWN).shouldBe(visible).click();
        $(SecurityPageElements.EDITOR).shouldBe(visible).$(SecurityPageElements.SECURITY_DROPDOWN_MENU).shouldBe(visible)
                .$(By.xpath("//a[contains(text(), \"" + schemeType + "\")]")).shouldBe(visible).click();

        if (schemeType.equalsIgnoreCase("API Key")) {
            $(By.id("name20")).sendKeys("headerName");
            //TODO: not supported from syndesis yet and needs more settings here
        }

        if (schemeType.equalsIgnoreCase("OAuth 2")) {
            $(By.id("flow")).shouldBe(visible).click();
            $(By.id("flow")).shouldBe(visible).parent().$(By.xpath("//a[contains(text(), \"Password\")]")).shouldBe(visible).click();
            $(By.id("tokenUrl")).sendKeys("https://hihi");
        }

        ElementsCollection saveButtons = $(SecurityPageElements.ACTION_HEADER).shouldBe(visible).$$(SecurityPageElements.SAVE);
        assertThat(saveButtons.size()).isGreaterThan(1);
        saveButtons.get(2).shouldBe(visible).click();

        $(Elements.BUTTON_ADD_REQUIREMENT).shouldBe(visible).click();

        try {
            OpenShiftWaitUtils.waitFor(() -> $(SecurityPageElements.SECURITY_REQUIREMENT).exists(), 60 * 1000L);
        } catch (InterruptedException | TimeoutException e) {
            fail("Security requirement was not found.");
        }

        ElementsCollection items = $$(SecurityPageElements.SECURITY_REQUIREMENT_ITEMS)
                .shouldBe(sizeGreaterThanOrEqual(1)).filterBy(text("ImmovableName"));
        assertThat(items).hasSize(1);
        items.first().click();

        saveButtons = $(SecurityPageElements.ACTION_HEADER).shouldBe(visible).$$(SecurityPageElements.SAVE);
        assertThat(saveButtons.size()).isGreaterThan(1);
        saveButtons.get(2).shouldBe(visible).click();
    }

    @Then("^check that api connector authentication section contains text \"([^\"]*)\"$")
    public void verifySelectedSecurity(String expectedText) {
        assertThat($(Elements.AUTHENTICATION_CONTAINER).shouldBe(visible).text())
                .containsIgnoringCase(expectedText);
    }

    @When("^click on button \"([^\"]*)\" while in apicurio studio page$")
    public void clickOnButtonInApicurio(String buttonTitle) {
        TestUtils.sleepForJenkinsDelayIfHigher(2);
        getButton(buttonTitle).shouldBe(visible, enabled).shouldNotHave(attribute("disabled")).click();
        TestUtils.sleepForJenkinsDelayIfHigher(2);
    }

    public SelenideElement getButton(String buttonTitle) {
        log.info("searching for button {}", buttonTitle);
        return $(Elements.SYNDESIS_ROOT).shouldBe(visible).findAll(By.tagName("button"))
                .filter(Condition.matchText("(\\s*)" + buttonTitle + "(\\s*)")).shouldHave(sizeGreaterThanOrEqual(1)).first();
    }
}
