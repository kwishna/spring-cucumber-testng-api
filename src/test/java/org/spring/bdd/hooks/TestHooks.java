package org.spring.bdd.hooks;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.spring.bdd.envs.WebDriverManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.spring.bdd.extent.ExtentReporting;

import java.io.ByteArrayInputStream;

public class TestHooks {

    private static final Logger log = LogManager.getLogger(TestHooks.class);

    @Autowired(required = false)
    private WebDriver driver;

    @Before(order = 1)
    public void beforeScenario(Scenario scenario) {
        log.info("Starting scenario: {}", scenario.getName());
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
                byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                scenario.attach(screenshot, "image/png", scenario.getName());
                Allure.addAttachment(scenario.getName(), new ByteArrayInputStream(screenshot));
            }
        } catch (Exception ignored) {
            // avoid failing API scenarios due to missing driver
        }
    }

    @After(order = 2)
    public void tearDown() {
        // WebDriver cleanup is handled automatically by Spring Bean @Bean(destroyMethod = "quit")
        ExtentReporting.flush();
    }
}