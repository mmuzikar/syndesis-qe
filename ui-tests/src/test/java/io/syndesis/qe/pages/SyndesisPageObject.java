package io.syndesis.qe.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import io.syndesis.qe.wait.OpenShiftWaitUtils;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThanOrEqual;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@Slf4j
public abstract class SyndesisPageObject {

    public abstract SelenideElement getRootElement();

    public abstract boolean validate();

    public SelenideElement getButton(String buttonTitle) {
        return getButton(buttonTitle, getRootElement());
    }

    public SelenideElement getButton(String buttonTitle, SelenideElement differentRoot) {
        log.info("searching for button {}", buttonTitle);

        try {
            //ugly but necessary due to syndesis page refreshing periodically
            try {
                OpenShiftWaitUtils.waitFor(() -> differentRoot.shouldBe(visible).findAll(By.tagName("button"))
                        .filter(Condition.matchText("(\\s*)" + buttonTitle + "(\\s*)")).size() >= 1, (long) (60 * 1000.0));
            } catch (org.openqa.selenium.StaleElementReferenceException ex) {
                log.warn("Element was detached from the page, trying again to find a button but now within syndesis-root element");
                OpenShiftWaitUtils.waitFor(() -> $(By.tagName("syndesis-root")).shouldBe(visible).findAll(By.tagName("button"))
                        .filter(Condition.matchText("(\\s*)" + buttonTitle + "(\\s*)")).size() >= 1, (long) (60 * 1000.0));
            }

        } catch (TimeoutException | InterruptedException e1) {
            fail(buttonTitle + " not found", e1);
        }

        ElementsCollection foundButtons = differentRoot.shouldBe(visible).findAll(By.tagName("button"))
                .filter(Condition.matchText("(\\s*)" + buttonTitle + "(\\s*)")).shouldHave(sizeGreaterThanOrEqual(1));

        if (foundButtons.size() > 1) {
            fail("Ambiguous button title. Found more that 1 button with title " + buttonTitle);
        }

        return foundButtons.first();
    }

    public SelenideElement getFirstVisibleButton(String buttonTitle) {
        log.info("searching for first visible button {}", buttonTitle);
        ElementsCollection buttonElements = this.getRootElement().findAll(By.linkText(buttonTitle)).filter(visible);
        return buttonElements.get(0);
    }

    /**
     * Fill form with given data. It will look for ui element for every map entry.
     *
     * @param data           key,value data. Key is used for element lookup
     * @param parrentElement search inputs in child elements of this one
     */
    public void fillForm(Map<By, String> data, SelenideElement parrentElement) {
        String value;
        for (By locator : data.keySet()) {
            value = data.get(locator);
            SelenideElement inputElement = parrentElement.find(locator).shouldBe(visible);
            if ("select".equalsIgnoreCase(inputElement.getTagName())) {
                inputElement.selectOption(value);
            } else {
                inputElement.sendKeys(value);
            }
        }
    }

    public void selectFromDropDown(String selectId, String option) {
        SelenideElement select = this.getElementById(selectId).shouldBe(visible);
        assertThat(select.getTagName(), is("select"));
        select.selectOption(option);
    }

    public String getCurrentUrl() {
        return getWebDriver().getCurrentUrl();
    }

    public void goToUrl(String url) {
        getWebDriver().get(url);
    }

    public SelenideElement getLink(String linkTitle) {
        return this.getRootElement().find(By.linkText(linkTitle));
    }

    public SelenideElement getLink(String linkTitle, int index) {
        ElementsCollection links = this.getLinks(linkTitle);
        return links.get(index);
    }

    public ElementsCollection getLinks(String linkTitle) {
        return this.getRootElement().findAll(By.linkText(linkTitle));
    }

    public SelenideElement getLinkRandom(String linkTitle) {
        ElementsCollection links = this.getLinks(linkTitle);
        int index = (int) Math.floor(Math.random() * links.size());
        return links.get(index);
    }

    public SelenideElement getElementByCssSelector(String cssSelector) {
        return this.getRootElement().find(By.cssSelector(cssSelector));
    }

    public SelenideElement getElementByXpath(String xpathSelector) {
        return this.getRootElement().find(By.xpath(xpathSelector));
    }

    public SelenideElement getElementByClassName(String elementClassName) {
        return this.getRootElement().find(By.className(elementClassName));
    }

    public SelenideElement getElementByLocator(By elementLocator) {
        log.info("searching for element {}", elementLocator.toString());
        return this.getRootElement().$(elementLocator);
    }

    public SelenideElement getTitleByText(String text) {
        log.info("searching for title {}", text);
        return this.getRootElement().find(By.cssSelector(String.format("h2:contains('%s')", text)));
    }

    public ElementsCollection getElementsByClassName(String elementClassName) {
        return this.getRootElement().findAll(By.className(elementClassName));
    }

    public SelenideElement getElementRandom(String elementClassName) {
        ElementsCollection elements = this.getElementsByClassName(elementClassName);
        int index = (int) Math.floor(Math.random() * elements.size());
        return elements.get(index);
    }

    public SelenideElement getElementRandom(By locator) {
        ElementsCollection elements = this.getRootElement().findAll(locator);
        int index = (int) Math.floor(Math.random() * elements.size());
        return elements.get(index);
    }

    public void selectOption(SelenideElement selectElement, String option) {
        SelenideElement optionElement = getElementContainingText(By.tagName("option"), option);
        optionElement.shouldBe(visible).click();
    }

    private SelenideElement getElementById(String inputId) {
        return this.getRootElement().find(By.id(inputId));
    }

    public SelenideElement getInputBySelector(String selector) {
        return this.getRootElement().find(By.cssSelector(selector));
    }

    public void fillInput(String inputId, String value) {
        SelenideElement input = this.getElementById(inputId);
        assertThat(input.getTagName(), is("input"));
        if (input.getAttribute("type").equals("checkbox")) {
            input.setSelected(Boolean.valueOf(value));
        } else {
            input.shouldBe(visible).clear();
            input.shouldBe(visible).sendKeys(value);
        }
    }

    public void fillInput(SelenideElement input, String value) {
        if (input.getAttribute("type").equals("checkbox")) {
            input.setSelected(Boolean.valueOf(value));
        } else {
            input.shouldBe(visible).clear();
            input.shouldBe(visible).sendKeys(value);
        }
    }

    public void setElementValue(String value, SelenideElement element) {
        switch (element.getTagName()) {
            case "input":
                fillInput(element, value);
                break;
            case "select":
                element.selectOption(value);
                break;
        }
    }

    public SelenideElement getElementContainingText(By by, String text) {
        return getElementContainingText(by, text, getRootElement());
    }

    public SelenideElement getElementContainingText(By by, String text, SelenideElement differentRoot) {
        try {
            OpenShiftWaitUtils.waitFor(() -> differentRoot.shouldBe(visible).findAll(by).size() > 0, 30 * 1000L);
        } catch (TimeoutException | InterruptedException e) {
            fail("Element was not found in 30s", e);
        }

        ElementsCollection elements = differentRoot.shouldBe(visible).findAll(by).shouldBe(sizeGreaterThan(0));
        log.debug("Found " + elements.size() + " elements with selector, filtering...");

        elements = elements.filter(exactText(text));
        assertThat(elements.size()).isGreaterThanOrEqualTo(1);
        return elements.first();
    }

    public String getElementText(By locator) {
        SelenideElement element = this.getRootElement().shouldBe(visible).find(locator);
        return element.shouldBe(visible).getText();
    }

    public void checkButtonStatus(String buttonTitle, String status) {
        log.info("checking button {} status", buttonTitle);
        // this speed-up the check we don't need shoulBe here
        this.getButton(buttonTitle).waitUntil(this.conditionValueOf(status), 5 * 1000).is(this.conditionValueOf(status));
    }

    public Condition conditionValueOf(String status) {
        Condition condition;
        switch (status) {
            case "Active":
            case "Visible":
                condition = visible;
                break;
            case "Disabled":
            case "Inactive":
                condition = disabled;
                break;
            default:
                condition = visible;
                break;
        }
        return condition;
    }
}
