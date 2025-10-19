package org.spring.bdd.configs;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import io.cucumber.testng.FeatureWrapper;
import io.cucumber.testng.PickleWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.DataProvider;

import java.util.List;

@CucumberOptions(
        features = "src/test/resources/features",
        plugin = {
                "pretty",
                "json:target/cucumber.json",
                "junit:target/junit.xml",
                "html:target/cucumber-html",
//                "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:",
                "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
        },
        glue = {
                "org.spring.bdd.configs",
                "org.spring.bdd.hooks",
                "org.spring.bdd.stepDefs"
        }
)
public class RunSpringCucumberTest extends AbstractTestNGCucumberTests {

    Logger log = LogManager.getLogger();

//    @BeforeSuite
//    public void setUp() {
//        SampleExtent.initExtent();
//    }

    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }

    @Override
    public void runScenario(PickleWrapper pickle, FeatureWrapper feature) {
        List<String> tags = pickle.getPickle().getTags();
        if (tags.contains("@ignore") || tags.contains("@skip") || tags.contains("@disabled")) {
            log.info(String.format("Disabled test: {}", pickle.getPickle().getName()));
            return;
        }
        super.runScenario(pickle, feature);
    }
}
