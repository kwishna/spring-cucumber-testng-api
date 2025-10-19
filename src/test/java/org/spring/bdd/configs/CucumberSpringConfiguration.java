package org.spring.bdd.configs;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.spring.bdd.envs.DriverFactory;
import org.spring.bdd.envs.WebDriverManager;

//@SpringBootTest(
//        webEnvironment = SpringBootTest.WebEnvironment.NONE,
//        classes = {
//                WebDriverConfig.class
//        }
//)
//@ActiveProfiles("dev")
@ComponentScan(
        basePackages = { "org.spring.bdd" }
)
@ContextConfiguration(classes = {
        WebDriverConfig.class,
        DriverFactory.class,
        WebDriverManager.class
})
@CucumberContextConfiguration
public class CucumberSpringConfiguration {
}
