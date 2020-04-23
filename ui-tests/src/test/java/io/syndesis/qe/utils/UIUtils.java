package io.syndesis.qe.utils;

import static com.codeborne.selenide.Selenide.$;

import org.openqa.selenium.By;

public class UIUtils {

    private static class Elements {
        public static By LOADER = By.cssSelector(".Loader");
    }

    public static void enxureUILoaded(){
        TestUtils.waitFor(() -> !$(Elements.LOADER).exists(), 1, 10, "Syndesis UI is still loading");
    }

}
