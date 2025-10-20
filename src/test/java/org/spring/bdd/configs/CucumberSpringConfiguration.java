package org.spring.bdd.configs;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;

//@ActiveProfiles("dev")
//@ContextConfiguration
@SpringBootTest(classes = { // Loads Spring context from TestConfig.
        TestConfig.class
})
@CucumberContextConfiguration // Tells cucumber to use Spring.
public class CucumberSpringConfiguration {
    // Don't mark this as @Component, else, it will conflict when Spring registers as Bean
}

