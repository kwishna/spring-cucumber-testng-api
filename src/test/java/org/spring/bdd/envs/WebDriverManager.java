package org.spring.bdd.envs;

import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WebDriverManager {

    private static final ThreadLocal<WebDriver> driverThread = new ThreadLocal<>();

    private final DriverFactory factory;
    private final String browser;

    public WebDriverManager(DriverFactory factory, @Value("${browser:chrome}") String browser) {
        this.factory = factory;
        this.browser = browser;
    }

    public WebDriver getDriver() {
        if (driverThread.get() == null) {
            WebDriver driver = factory.createWebDriver(browser);
            driverThread.set(driver);
        }
        return driverThread.get();
    }

    public void quitDriver() {
        if (driverThread.get() != null) {
            driverThread.get().quit();
            driverThread.remove();
        }
    }
}
