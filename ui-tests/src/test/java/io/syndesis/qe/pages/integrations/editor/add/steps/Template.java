package io.syndesis.qe.pages.integrations.editor.add.steps;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import io.syndesis.qe.pages.integrations.editor.add.steps.getridof.AbstractStep;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;

import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class Template extends AbstractStep {

    private static final String UPLOAD_CLASS = "file-upload-input";

    @Setter
    @Getter
    private String templateDef;

    public Template(String templateDef) {
        super();
        setTemplateDef(templateDef);
    }

    @Override
    public void fillConfiguration() {
        String template = getTemplateDef();
        setTemplate(template);
    }

    @Override
    public boolean validate() {
        return getTextAreaElement().shouldBe(visible).isDisplayed();
    }

    @Override
    public void initialize() {
        String template = getTemplateTextAreaDef();
        setTemplateDef(template);
    }

    @Override
    public String getParameter() {
        return getTemplateDef();
    }

    @Override
    public void setParameter(String parameter) {
        setTemplateDef(parameter);
    }

    public String getTemplateTextAreaDef() {
        return getTextAreaElement().shouldBe(visible).getValue();
    }

    public void setTemplateTextAreaDef(String areaDef) {
        getTextAreaElement().sendKeys(areaDef);
    }

    public SelenideElement getTextAreaElement() {
        return this.getRootElement().find(Elements.TEMPLATE);
    }

    private void flipThroughOptions() {
        //The template in textarea validates when the template type is changed
        log.info("Clicking through all template options to validate the template");
        SelenideElement radioGroup = getRootElement().find(Elements.TEMPLATE_LANGUAGES);
        ElementsCollection options = radioGroup.findAll(By.cssSelector("input[type='radio']"));
        String validId = options.filterBy(Condition.checked).first().attr("id");
        options.forEach(SelenideElement::click);
        radioGroup.find(By.id(validId)).click();
        log.info("Template should be validated");
    }

    public void setTemplate(String template) {
        getTextAreaElement().sendKeys(template);
        //Should validate the textarea to enable the next button if the template is valid
        getRootElement().find(By.tagName("form")).click();
        flipThroughOptions();
    }

    public void setTemplatingEngine(String engine) {
        getRootElement().find(By.id(engine.toLowerCase() + "-choice")).shouldBe(visible).click();
    }

    public void uploadTemplate(String filePath) {
        File tplFile = new File(filePath);
        assertThat(tplFile).exists().canRead();
        //Make file input element visible
        SelenideElement upload = getRootElement().find(Elements.FILE_INPUT);
        WebDriver driver = WebDriverRunner.getWebDriver();
        JavascriptExecutor jse = (JavascriptExecutor) driver;
        jse.executeScript("arguments[0].className=arguments[1];", upload.parent(), Template.UPLOAD_CLASS);
        WebDriverWait wait = new WebDriverWait(driver, 20);
        wait.until(ExpectedConditions.elementToBeClickable(upload));
        log.info("uploading file " + tplFile.getAbsolutePath());
        upload.uploadFile(tplFile);
        log.info("done uploading");
        flipThroughOptions();
    }

    private static final class Elements {
        public static final By TEMPLATE = By.cssSelector(".CodeMirror textarea");
        public static final By FILE_INPUT = By.className("templater-form-file-chooser");
        public static final By TEMPLATE_LANGUAGES = By.className("template-language-choices");
    }
}
