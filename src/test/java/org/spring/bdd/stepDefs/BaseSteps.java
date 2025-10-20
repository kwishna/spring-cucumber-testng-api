package org.spring.bdd.stepDefs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spring.bdd.hooks.TestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * A base class for all step definition classes.
 *
 * It is managed by Spring within the "cucumber-glue" scope. This means a new
 * instance of BaseSteps (and any class that extends it) is created for each

 * scenario, providing a clean state and enabling safe parallel execution.
 *
 * It holds common dependencies and a mechanism for sharing state between steps.
 */
@Component
@Scope("cucumber-glue") // 1. CRITICAL: This makes the class and its state scenario-scoped.
public abstract class BaseSteps {

    Logger log = LogManager.getLogger(this);

    @Value("environment")
    protected String environment;

    @Autowired
    TestContext testContext;
}
