package io.syndesis.qe.steps.other;

import static org.assertj.core.api.Assertions.assertThat;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.executeJavaScript;

import io.syndesis.qe.utils.ByUtils;
import io.syndesis.qe.utils.TestUtils;
import io.syndesis.qe.utils.UIUtils;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;

import java.util.List;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.cucumber.datatable.DataTable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConditionalFlowSteps {
    private static class EditFlowStepElements {
        private static By ICON_DOWN = By.className("fa-arrow-circle-o-down");
        private static By ICON_UP = By.className("fa-arrow-circle-o-up");
        private static By ICON_DELETE = By.className("fa-trash-o");
        private static By CONDITION_LIST_ITEM = ByUtils.containsDataTestId("input", "-condition");
        private static By OPEN_FLOW_CONDITION_BUTTON_PARENT = By.cssSelector("[data-testid*=\"choice-view\"][data-testid*=\"flow-button\"]");
        private static By FLOW_DROPDOWN = ByUtils.containsDataTestId("flows-dropdown");
        private static By FLOW_DROPDOWN_ITEM = ByUtils.dataTestId("conditions-dropdown-item-link");
        private static By FLOW_DROPDOWN_BODY = ByUtils.containsDataTestId("flows-dropdown-body");
        private static By FLOW_BACK_DROPDOWN_BUTTON = ByUtils.dataTestId("a", "editor-toolbar-dropdown-back-button-item-back-button");
        private static By FLOW_BACK_DIRECT_BUTTON = By.id("integration-editor-back-button");
        private static By ADD_ANOTHER_CONDITION = ByUtils.dataTestId("button", "form-array-control-add-another-item-button");
        private static By WARNING_ICON = ByUtils.dataTestId("button", "integration-editor-steps-list-item-warning-button");
        private static By ADD_DATA_MAPPING_STEP_WARNING_LINK =
            ByUtils.dataTestId("a", "integration-editor-step-adder-add-step-before-connection-link");
        private static By ADD_DEFAULT_FLOW_WARNING_LINK = ByUtils.dataTestId("a", "integration-editor-step-adder-add-default-flow-link");

        private static By getConditionOnPosition(String position) {
            final String dataTestid = String.format("flowconditions-%s-condition", position);
            return ByUtils.dataTestId(dataTestid);
        }

        private static By getConditionIconsOnPosition(String position) {
            return By.id("flowconditions-X-array-controls-control".replace("X", position));
        }
    }

    private static class EditIntegrationElements {
        private static By CONDITIONAL_FLOW_STEP = ByUtils.dataTestId("integration-editor-steps-list-item-conditional-flows-list-item");
        private static By CONDITIONAL_FLOW_STEP_INNER_FLOW_ITEM = By.className("list-group-item");
        private static By DEFAULT_DISABLED = By.className("fa-ban");

        private static By getNthConditionalFlowStep(int nthConditionalFlowElement) {
            return ByUtils.dataTestIdContainsSubstring("-conditional-flows-list-item", nthConditionalFlowElement);
        }
    }

    @When("^add a data mapping step - open datamapper$")
    public void addDataMappingStep() {
        $(EditFlowStepElements.WARNING_ICON).shouldBe(visible).click();
        $(EditFlowStepElements.ADD_DATA_MAPPING_STEP_WARNING_LINK).shouldBe(visible).click();
    }

    @When("^add a default flow through warning link - open flows configuration$")
    public void addDefaultFlowThroughWarningLink() {
        $(EditFlowStepElements.WARNING_ICON).shouldBe(visible).click();
        $(EditFlowStepElements.ADD_DEFAULT_FLOW_WARNING_LINK).shouldBe(visible).click();
    }

    @When("^Add another condition$")
    public void getAddAnotherConditionButton() {
        $(EditFlowStepElements.ADD_ANOTHER_CONDITION).shouldBe(visible).click();
    }

    //data:     |position   |value  |
    //example:  |0          |asdf   |
    @Then("^validate condition content in condition flow step$")
    public void validateConditionContent(DataTable data) {

        for (List<String> row : data.cells()) {
            assertThat($(EditFlowStepElements.getConditionOnPosition(row.get(0))).getValue())
                .isEqualToIgnoringCase(row.get(1));

            //selenide returns empty value in some cases even if the value is present, probably a driver bug
            // so second assert with usage of javascript is necessary
            assertThat((String) executeJavaScript("return document.getElementById('flowconditions-" + row.get(0) + "-condition').value"))
                .isEqualToIgnoringCase(row.get(1));
        }
    }

    //data:
    //  |position   |ACTION   |
    //  |1          |UP       |
    //  |0          |DOWN     |
    //  |1          |DELETE   |
    @When("^click on the condition icon$")
    public void clickOnConditionIcon(DataTable data) {
        for (List<String> row : data.cells()) {
            SelenideElement selectedCondition = $(EditFlowStepElements.getConditionIconsOnPosition(row.get(0))).shouldBe(visible);
            By iconSelector;
            switch (row.get(1)) {
                case "UP":
                    iconSelector = EditFlowStepElements.ICON_UP;
                    break;
                case "DOWN":
                    iconSelector = EditFlowStepElements.ICON_DOWN;
                    break;
                case "DELETE":
                    iconSelector = EditFlowStepElements.ICON_DELETE;
                    break;
                default:
                    throw new IllegalArgumentException("Incorrect data table value on position 1");
            }
            selectedCondition.$(iconSelector).shouldBe(visible).click();
        }
    }

    @Then("^validate that condition count is equal to (.)$")
    public void validateConditionCount(int count) {
        TestUtils.waitFor(() -> $$(EditFlowStepElements.CONDITION_LIST_ITEM).size() > 0,
            1, 20, "No condition found");

        assertThat($$(EditFlowStepElements.CONDITION_LIST_ITEM)).hasSize(count);
    }

    @Then("^configure condition on position (.)$")
    public void configureConditionOnPosition(int index) {
        TestUtils.waitFor(() -> $$(EditFlowStepElements.OPEN_FLOW_CONDITION_BUTTON_PARENT).size() >= index,
            3, 30, "Condition on position " + index + " was not found");

        $$(EditFlowStepElements.OPEN_FLOW_CONDITION_BUTTON_PARENT).get(index - 1).shouldBe(visible).click();
    }

    @When("^return to primary flow from integration flow from dropdown$")
    public void goBackToPrimaryFlowFromDropdown() {
        UIUtils.enxureUILoaded();
        openDropdownWithConditions();
        $(EditFlowStepElements.FLOW_BACK_DROPDOWN_BUTTON).shouldBe(visible).click();
    }

    @When("^return to primary flow from integration flow directly$")
    public void goBackToPrimaryFlowDirectly() {
        $(EditFlowStepElements.FLOW_BACK_DIRECT_BUTTON).shouldBe(visible).click();
    }

    @Then("^check that conditional flow step contains (\\d+) flows$")
    public void validateNumberOfFlowsInIntegrationEditView(int expectedNumberOfFlows) {
        int size = $(EditIntegrationElements.getNthConditionalFlowStep(1)).shouldBe(visible)
            .$$(EditIntegrationElements.CONDITIONAL_FLOW_STEP_INNER_FLOW_ITEM).size();
        assertThat(size).isEqualTo(expectedNumberOfFlows);
    }

    @Then("^check that conditional number (\\d+) flow step contains (\\d+) flows$")
    public void validateNthNumberOfFlowsInIntegrationEditView(int nthConditionalFlowconnection, int expectedNumberOfFlows) {
        int size = $(EditIntegrationElements.getNthConditionalFlowStep(nthConditionalFlowconnection)).shouldBe(visible)
            .$$(EditIntegrationElements.CONDITIONAL_FLOW_STEP_INNER_FLOW_ITEM).size();
        assertThat(size).isEqualTo(expectedNumberOfFlows);
    }

    @Then("^check that conditional flow default step is (disabled|enabled)$")
    public void validateStateOfDefaultFlow(String state) {
        assertThat(!$(EditIntegrationElements.DEFAULT_DISABLED).exists())
            .isEqualTo("enabled".equalsIgnoreCase(state));
    }

    @Then("^validate conditional flow dropdown content$")
    public void validateConditionalFlowDropdown(DataTable data) {
        openDropdownWithConditions();

        ElementsCollection dropdownElements = $$(EditFlowStepElements.FLOW_DROPDOWN_ITEM);
        int current = 0;
        for (List<String> row : data.cells()) {
            assertThat(dropdownElements.get(current).getText().replaceAll("\n", " ")).isEqualToIgnoringCase(row.get(0));
            current++;
        }
    }

    private void openDropdownWithConditions() {
        if ($(EditFlowStepElements.FLOW_DROPDOWN_BODY).exists()) {
            return;
        }

        TestUtils.waitFor(() -> $(EditFlowStepElements.FLOW_DROPDOWN).exists(),
            3,
            30,
            "Dropdown menu was not loaded in time");

        $(EditFlowStepElements.FLOW_DROPDOWN).click();
        WebDriverWait wait = new WebDriverWait(WebDriverRunner.getWebDriver(), 10);
        wait.until(ExpectedConditions.visibilityOf($(EditFlowStepElements.FLOW_DROPDOWN_BODY)));
    }
}
