package io.syndesis.qe.pages.integrations.fragments;

import static org.assertj.core.api.Assertions.fail;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import io.syndesis.qe.fragments.common.list.RowList;
import io.syndesis.qe.utils.ByUtils;
import io.syndesis.qe.wait.OpenShiftWaitUtils;

import org.openqa.selenium.By;

import java.util.List;
import java.util.concurrent.TimeoutException;

public class OperationsList extends RowList {
    public OperationsList(By rootElement) {
        super(rootElement);
    }

    private static final class Element {
        public static final By VERB = ByUtils.dataTestId("operation-method");
        public static final By STATUS = By.cssSelector(".list-pf-additional-content div");
        public static final By DESCRIPTION = ByUtils.dataTestId("operation-description");
        public static final By PATH = ByUtils.dataTestId("operation-path");
    }

    public String getStatus(String operationName) {
        return getItem(operationName).$(Element.STATUS).shouldBe(visible).getText().trim();
    }

    public String getVerb(String operationName) {
        return getItem(operationName).$(Element.VERB).shouldBe(visible).getText().trim();
    }


    public String getUrl(String operationName) {
        return getItem(operationName).$(Element.PATH).shouldBe(visible).getText().trim();
    }

    public void apiOperationCreateFlow(String title) {
        try {
            OpenShiftWaitUtils.waitFor(() -> $(getItem(title)).is(visible), 15 * 1000L);
        } catch (TimeoutException | InterruptedException e) {
            fail("API provider operation in list was not found in 15s.", e);
        }

        $(getItem(title)).$(ByUtils.dataTestId("api-provider-operations-create-flow")).shouldBe(visible).click();
    }

    public List<String> getOperations() {
        return getItemsCollection().texts();
    }
}
