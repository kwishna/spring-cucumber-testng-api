package org.spring.bdd.configs;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;

//@ActiveProfiles("dev")
//@ContextConfiguration
@SpringBootTest(classes = {
        TestConfig.class
})
@CucumberContextConfiguration
public class CucumberSpringConfiguration {
}

