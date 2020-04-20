package io.syndesis.qe.pages.integrations.fragments;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThanOrEqual;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import io.syndesis.qe.pages.SyndesisPageObject;
import io.syndesis.qe.pages.integrations.editor.add.steps.getridof.StepFactory;
import io.syndesis.qe.utils.ByUtils;

import org.openqa.selenium.By;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IntegrationFlowView extends SyndesisPageObject {

    private static final class Link {
        public static final By ADD_STEP = By.linkText("Add a step");
        public static final By ADD_CONNECTION = By.linkText("Add a connection");
    }

    private static final class Element {
        public static final By ROOT = By.cssSelector(".integration-vertical-flow__body");

        public static final By NAME = By.cssSelector("input.form-control.integration-name");
        public static final By STEP_TITLE = By.cssSelector("div.step-name.syn-truncate__ellipsis");
        public static final By ACTIVE_STEP_ICON = By.cssSelector(".integration-flow-step-details.is-active");
        public static final By STEP_INSERT = ByUtils.dataTestId("integration-flow-add-step-add-step-link");

        public static final By POPOVER_CLASS = By.className("pf-c-popover__body");
        public static final By STEP_DETAILS = By.className("pf-l-split__item");

        public static final By DATA_WARNING_BUTTON = ByUtils.containsDataTestId("button", "warning-button");

        public static final By FLOW_TITLE = By.cssSelector(".step.start .step-name");

        public static final By TRASH = By.className("fa-trash");

        public static final By DELETE_BUTTON = ByUtils.dataTestId("integration-editor-step-adder-delete-button");

        public static final By STEP = ByUtils.containsDataTestId("integration-editor-steps-list-item");
    }

    private static final class Button {
        public static final By EXPAND = By.cssSelector("button.btn.btn-default.toggle-collapsed.collapsed");
        public static final By COLLAPSE = By.cssSelector("button.btn.btn-default.toggle-collapsed:not(.collapsed)");
    }

    private StepFactory stepComponentFactory = new StepFactory();

    @Override
    public SelenideElement getRootElement() {
        SelenideElement elementRoot = $(Element.ROOT).shouldBe(visible);
        return elementRoot;
    }

    @Override
    public boolean validate() {
        return getRootElement().is(visible);
    }

    public String getIntegrationName() {
        return this.getRootElement().find(Element.NAME).shouldBe(visible).getAttribute("value");
    }

    /**
     * Check if there's an icon in active state goToNextWizardPhase to the position in the integration flow
     *
     * @param position (start|finish)
     */
    public boolean verifyActivePosition(String position) {
        SelenideElement selenideElement = $(Element.ACTIVE_STEP_ICON).$(By.className("integration-flow-step-details__title"));
        return selenideElement.shouldBe(visible).getText().contains(position);
    }

    public List<String> getStepsTitlesArray() {
        if (isCollapsed()) {
            $(Button.EXPAND).click();
        }

        ElementsCollection steps = this.getRootElement().findAll(Element.STEP);

        List<String> stepsArray = new ArrayList<String>();

        for (int i = 0; i < steps.size(); i++) {
            SelenideElement step = steps.get(i);
            SelenideElement title = step.find(Element.STEP_TITLE);
            stepsArray.add(title.getAttribute("title"));
        }
        if (isExpanded()) {
            $(Button.COLLAPSE).click();
        }
        return stepsArray;
    }

    public ElementsCollection getAllTrashes() {
        return this.getRootElement().findAll(Element.DELETE_BUTTON);
    }

    public void clickRandomTrash() {
        this.getElementRandom(Element.DELETE_BUTTON).shouldBe(visible).click();
    }

    public void clickAddStepLink(int pos) {

        List<SelenideElement> allStepInserts = $$(Element.STEP_INSERT)
            .shouldHave(sizeGreaterThanOrEqual(pos));
        SelenideElement stepElement = allStepInserts.get(pos);
        stepElement.shouldBe(visible).click();
    }

    public void clickAddConnectionLink(int pos) {
        List<SelenideElement> allStepInserts = getRootElement().$$(Element.STEP_INSERT)
            .shouldHave(sizeGreaterThanOrEqual(pos));
        SelenideElement stepElement = allStepInserts.get(pos);

        stepElement.scrollIntoView(true).hover();

        getRootElement().$(Link.ADD_CONNECTION).shouldBe(visible).click();
    }

    /**
     * If you only have 3 steps - start, finish and a step in the middle to get the middle step
     * set stepPosition to "middle". Otherwise set it to start or finish to get first/last step.
     *
     * @param stepPosition
     * @return text in popover element
     */
    public String checkTextInHoverTable(String stepPosition) {
        String text;
        if ("middle".equalsIgnoreCase(stepPosition)) {
            // This is ugly but for now it works - it has only one usage: for getting our data-mapper step
            // which is between start and finish step.
            // Explanation: we have 3 steps and between them 2 elements with insert step option --> 5 total
            // with class "step" and we want the third element
            $$(By.className("step")).shouldHaveSize(5).get(2)
                .shouldBe(visible).find(By.className("icon")).shouldBe(visible).hover();
            text = $(By.className("popover")).shouldBe(visible).getText();
        } else {
            $(By.className(stepPosition)).shouldBe(visible).find(By.className("icon")).shouldBe(visible).hover();
            text = $(By.className("popover")).shouldBe(visible).getText();
        }
        return text;
    }

    public String getWarningTextFromStep(int stepPosition) {
        SelenideElement warningIcon = getStepWarningElement(stepPosition).shouldBe(visible);
        //open popup
        warningIcon.click();
        String text = getPopoverText();
        //hide popup
        warningIcon.click();
        return text;
    }

    /**
     * Always follow with shouldBe(visible) as it is needed also for the case when it returns nothing
     *
     * @param stepPosition
     * @return
     */
    public SelenideElement getStepWarningElement(int stepPosition) {
        return getStepOnPosition(stepPosition).$(Element.DATA_WARNING_BUTTON);
    }

    public SelenideElement getStepOnPosition(int position) {
        return $$(Element.STEP).shouldBe(sizeGreaterThanOrEqual(position))
            .get(position - 1).shouldBe(visible);
    }

    public void deleteStepOnPostion(int stepPosition) {
        SelenideElement step = getStepOnPosition(stepPosition);
        step.$(Element.TRASH).shouldBe(visible).click();
        $(Element.DELETE_BUTTON).shouldBe(enabled, visible).click();
    }

    public String getPopoverText() {
        String text = $(Element.POPOVER_CLASS).shouldBe(visible).text();
        log.info("Text found: " + text);
        return text;
    }

    public String getConnectionPropertiesText(SelenideElement connectionStep) {
        return connectionStep.$(Element.STEP_DETAILS).shouldBe(visible).getText();
    }

    public void addDatamapperStep(String action) {
        final String buttonDataTestid =
            String.format("integration-editor-steps-list-item-%s-warning-button", action.toLowerCase().replaceAll(" ", "-"));
        $(ByUtils.dataTestId("button", buttonDataTestid)).shouldBe(visible).click();
        $(ByUtils.dataTestId("a", "integration-editor-step-adder-add-step-before-connection-link")).shouldBe(visible).click();
    }

    public String getFlowTitle() {
        return getRootElement().$(Element.FLOW_TITLE).attr("title");
    }

    public int getNumberOfSteps() {
        return $$(By.className("integration-editor-steps-list-item__list-item")).size();
    }

    public boolean isCollapsed() {
        return $(Button.EXPAND).exists();
    }

    public boolean isExpanded() {
        return $(Button.COLLAPSE).exists();
    }
}
