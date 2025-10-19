package org.spring.bdd.configs;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.spring.bdd.envs.DriverFactory;
import org.spring.bdd.envs.WebDriverManager;

//@ActiveProfiles("dev")
//@ContextConfiguration(classes = {
//        WebDriverConfig.class,
//        DriverFactory.class,
//        WebDriverManager.class
//})
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {
            WebDriverConfig.class,
            DriverFactory.class,
            WebDriverManager.class
        }
)
@ComponentScan(
        basePackages = { "org.spring.bdd" }
)
@CucumberContextConfiguration
public class CucumberSpringConfiguration {
}
