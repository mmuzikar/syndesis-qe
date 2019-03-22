package io.syndesis.qe.pages.integrations.editor.add.steps.getridof;

import com.codeborne.selenide.SelenideElement;
import io.syndesis.qe.pages.SyndesisPageObject;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public abstract class AbstractStep extends SyndesisPageObject {

    private static final class Element {
        public static final By ROOT = By.cssSelector("syndesis-integration-step-configure");
    }

    public static final class StepType {
        public static final String LOG = "LOG";
        public static final String BASIC_FILTER = "BASIC FILTER";
        public static final String ADVANCED_FILTER = "ADVANCED FILTER";
        public static final String TEMPLATE = "TEMPLATE";
    }

    @Override
    public SelenideElement getRootElement() {
        SelenideElement elementRoot = $(Element.ROOT).shouldBe(visible);
        return elementRoot;
    }

    public abstract void fillConfiguration();

    public abstract boolean validate();

    public abstract void initialize();

    public abstract void setParameter(String parameter);

    public abstract String getParameter();
}
