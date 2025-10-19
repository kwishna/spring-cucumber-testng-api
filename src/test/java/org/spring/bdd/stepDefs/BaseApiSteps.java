package org.spring.bdd.stepDefs;

import io.restassured.response.Response;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("cucumber-glue")
public abstract class BaseApiSteps {
    protected Response response;
    protected String baseURL;
}