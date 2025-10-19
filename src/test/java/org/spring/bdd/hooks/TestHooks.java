package org.spring.bdd.hooks;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.spring.bdd.envs.WebDriverManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.spring.bdd.extent.SampleExtent;

import java.io.ByteArrayInputStream;

public class TestHooks {

    @Autowired
    private WebDriverManager webDriverManager;

    @Before(order = 1)
    public void beforeScenario(Scenario scenario) {
        System.out.println("Starting scenario: " + scenario.getName());
        SampleExtent.startTest(scenario.getName());
        SampleExtent.addInfo("Scenario Name: " + scenario.getName());
    }

    @After(order = 1)
    public void afterScenario(Scenario scenario) {
        if (scenario.isFailed()) {
            takeScreenshot(scenario);
            SampleExtent.addFail("Scenario failed: " + scenario.getName());
        } else {
            SampleExtent.addPass("Scenario passed: " + scenario.getName());
        }
        SampleExtent.endTest();
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
        SampleExtent.flush();
    }
}