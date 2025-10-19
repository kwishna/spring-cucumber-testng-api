@api
Feature: API Testing with BDD
  As a test automation engineer
  I want to verify API endpoints
  So that I can ensure API reliability

  @smoke @api
  Scenario: Verify successful API response
    Given the API base URL is "https://httpbin.org"
    When I send a GET request to "/anything"
    Then the response status code should be 200

  @regression @api
  Scenario Outline: Verify different user endpoints
    Given the API base URL is "https://httpbin.org"
    When I send a GET request to <endpoint>
    Then the response status code should be <statusCode>

    Examples:
      | endpoint       | statusCode |
      | "/anything"    | 200        |
      | "/nothing"     | 404        |