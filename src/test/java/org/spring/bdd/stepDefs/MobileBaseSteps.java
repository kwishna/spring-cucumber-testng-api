package org.spring.bdd.stepDefs;

import io.appium.java_client.AppiumDriver;
import org.spring.bdd.single_page_frameworks.mobile.AppiumClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.channels.IllegalSelectorException;

@Component
@Scope("cucumber-glue")
public abstract class MobileBaseSteps extends BaseSteps {

    @Autowired
    protected AppiumClient appiumClient;

    protected AppiumDriver driver;

    protected AppiumClient.AndroidDriverBuilder android() {
        return appiumClient.android();
    }

    protected AppiumClient.IOSDriverBuilder ios() {
        return appiumClient.ios();
    }

    protected AppiumClient.ElementFinder element() {
        ensureDriverInitialized();
        return appiumClient.element(driver);
    }

    protected AppiumClient.AppManager app() {
        ensureDriverInitialized();
        return appiumClient.app(driver);
    }

    protected AppiumClient.DeviceController device() {
        ensureDriverInitialized();
        return appiumClient.device(driver);
    }

    protected void quitDriver() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    protected void ensureDriverInitialized() {
        if (driver== null) {
            throw new IllegalSelectorException();
        }
    }
}
