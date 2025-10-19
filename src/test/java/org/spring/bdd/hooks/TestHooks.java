package org.spring.bdd.hooks;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.spring.bdd.envs.WebDriverManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.spring.bdd.extent.ExtentReporting;

import java.io.ByteArrayInputStream;

public class TestHooks {

    @Autowired
    private WebDriverManager webDriverManager;

    @Before(order = 1)
    public void beforeScenario(Scenario scenario) {
        System.out.println("Starting scenario: " + scenario.getName());
        ExtentReporting.startTest(scenario.getName());
        ExtentReporting.addInfo("Scenario Name: " + scenario.getName());
    }

    @After(order = 1)
    public void afterScenario(Scenario scenario) {
        if (scenario.isFailed()) {
            takeScreenshot(scenario);
            ExtentReporting.addFail("Scenario failed: " + scenario.getName());
        } else {
            ExtentReporting.addPass("Scenario passed: " + scenario.getName());
        }
        ExtentReporting.endTest();
    }

    private void takeScreenshot(Scenario scenario) {
        try {
            // Only attempt UI screenshots if the scenario has @ui
            if (scenario.getSourceTagNames().contains("@ui")) {
                byte[] screenshot = ((TakesScreenshot) webDriverManager.getDriver()).getScreenshotAs(OutputType.BYTES);
                scenario.attach(screenshot, "image/png", scenario.getName());
                Allure.addAttachment(scenario.getName(), new ByteArrayInputStream(screenshot));
            }
        } catch (Exception ignored) {
            // avoid failing API scenarios due to missing driver
        }
    }

    @After(order = 2)
    public void tearDown() {
        webDriverManager.quitDriver();
        ExtentReporting.flush();
    }
}