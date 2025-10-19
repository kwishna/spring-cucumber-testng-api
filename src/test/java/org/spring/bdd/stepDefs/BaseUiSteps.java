package org.spring.bdd.stepDefs;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseUiSteps extends BaseSteps {

    @Autowired
    protected WebDriver driver;

    @Autowired
    protected WebDriverWait wdWait;

}
