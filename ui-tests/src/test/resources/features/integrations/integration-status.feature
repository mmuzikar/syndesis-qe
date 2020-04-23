# @sustainer: mmuzikar@redhat.com
@stage-smoke
@ui
@database
@integrations-check-starting-status
@ENTESB-13530
Feature: Integration - Status

  Background: Clean application state
    Given clean application state
    Given log into the Syndesis

  @ENTESB-12415
  @integration-check-starting-status-on-detail-page
  Scenario: Check starting integration status on detail page
    When navigate to the "Home" page
    And click on the "Create Integration" link to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

    When select the "PostgresDB" connection
    And select "Periodic stored procedure invocation" integration action
    And click on the "Next" button

    When select the "PostgresDB" connection
    And select "Invoke stored procedure" integration action
    And click on the "Next" button

    And publish integration
    And set integration name "integration-check-starting-status-on-detail-page"
    And publish integration

    Then check starting integration status on Integration Detail page

  @ENTESB-12415
  @integration-check-starting-status-on-integration-list
  Scenario: Check starting integration status on integration list
    When navigate to the "Home" page
    And click on the "Create Integration" link to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

    When select the "PostgresDB" connection
    And select "Periodic stored procedure invocation" integration action
    And click on the "Next" button

    When select the "PostgresDB" connection
    And select "Invoke stored procedure" integration action
    And click on the "Next" button

    And publish integration
    And set integration name "integration-check-starting-status-on-integration-list"
    And publish integration

    When navigate to the "Integrations" page

    Then check starting integration integration-check-starting-status-on-integration-list status on Integrations page

  @ENTESB-12415
  @integration-check-starting-status-on-home-page
  Scenario: Check starting integration status on home page
    When navigate to the "Home" page
    And click on the "Create Integration" link to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

    When select the "PostgresDB" connection
    And select "Periodic stored procedure invocation" integration action
    And click on the "Next" button

    When select the "PostgresDB" connection
    And select "Invoke stored procedure" integration action
    And click on the "Next" button

    And publish integration
    And set integration name "integration-check-starting-status-on-home-page"
    And publish integration

    When navigate to the "Home" page

    Then check starting integration integration-check-starting-status-on-home-page status on Home page
