package org.spring.bdd.pages;

import org.openqa.selenium.WebElement;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.PostConstruct;
import java.time.Duration;

/**
 * The foundation for all Page Objects.
 *
 * It encapsulates common functionalities like waiting for elements and performing
 * actions, making individual page classes cleaner and the overall framework more robust.
 * It holds the WebDriver instance, which is injected by Spring.
 */
public abstract class BasePage {

    @Autowired
    protected WebDriver driver;

    @Autowired
    protected WebDriverWait wait; // 1. Inject a pre-configured WebDriverWait

    /**
     * This method is called by Spring after the bean has been created and
     * dependencies have been injected. It initializes the WebElements of the
     * page class that uses the @FindBy annotation.
     */
    @PostConstruct // 2. A Spring annotation for post-construction initialization
    private void initElements() {
        // This initializes WebElements declared with @FindBy in subclasses
        PageFactory.initElements(this.driver, this);
    }

    // 3. ROBUST WRAPPER METHODS FOR ACTIONS AND WAITS
    // =================================================================

    /**
     * A safe click method that waits for the element to be clickable first.
     * @param element The WebElement to click.
     */
    protected void waitAndClick(WebElement element) {
        wait.until(ExpectedConditions.elementToBeClickable(element)).click();
    }

    /**
     * A safe sendKeys method that waits for the element to be visible and clears it first.
     * @param element The WebElement to type into.
     * @param text The text to type.
     */
    protected void waitAndSendKeys(WebElement element, String text) {
        WebElement visibleElement = wait.until(ExpectedConditions.visibilityOf(element));
        visibleElement.clear();
        visibleElement.sendKeys(text);
    }

    /**
     * A safe method to get text from an element, waiting for it to be visible.
     * @param element The WebElement to get text from.
     * @return The text content of the element.
     */
    protected String getText(WebElement element) {
        return wait.until(ExpectedConditions.visibilityOf(element)).getText();
    }

    /**
     * A generic wait for an element to be visible.
     * @param locator The By locator of the element.
     * @return The found WebElement.
     */
    protected WebElement waitForVisibility(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * A method to check if an element is displayed, handling potential exceptions.
     * @param element The WebElement to check.
     * @return true if displayed, false otherwise.
     */
    protected boolean isElementDisplayed(WebElement element) {
        try {
            // Use a short wait to avoid immediate failure if the element is briefly absent
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(2));
            return shortWait.until(ExpectedConditions.visibilityOf(element)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}

