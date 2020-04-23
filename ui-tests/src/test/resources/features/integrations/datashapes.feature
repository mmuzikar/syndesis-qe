# @sustainer: mmuzikar@redhat.com

@ui
@database
@datamapper
@conditional-flow
@integrations-datashapes
Feature: Integration - data shapes propagation

  Background: Clean application state
    Given clean application state
    And log into the Syndesis
    And navigate to the "Home" page
    And click on the "Create Integration" link to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"


  @integrations-datashapes-input-change
  @ENTESB-13527
  Scenario: Data shapes propagation
    When select the "PostgresDB" connection
    And select "Periodic SQL invocation" integration action
    And fill in periodic query input with "SELECT * FROM CONTACT" value
    And fill in period input with "5" value
    And select "Seconds" from sql dropdown
    And click on the "Next" button
    Then check visibility of page "Choose a Finish Connection"

    When select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    Then fill in invoke query input with "UPDATE TODO SET completed=1 WHERE TASK = :#TASK" value
    And click on the "Next" button

    When add integration step on position "0"
    And select "Split" integration step
    Then click on the "Next" button

    When add integration step on position "1"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui

    And create data mapper mappings
      | first_name | TASK |
    Then click on the "Done" button

    Then validate that input datashape on step 1 contains "SQL Result (Collection)"
    And validate that input datashape on step 4 contains "SQL Parameter"

    When delete step on position 1
    And select the "Webhook" connection
    And select "Incoming Webhook" integration action
    And click on the "Next" button
    And fill in values by element data-testid
      | describe-data-shape-form-kind-input | JSON Instance |
    # wait for UI to open definition
    And sleep for jenkins delay or "5" seconds
    And fill text into text-editor
      | {"author":"New Author","title":"Book Title"} |
    And click on the "Next" button

    Then validate that input datashape on step 1 contains "json-instance"
    And validate that input datashape on step 4 contains "SQL Parameter"


  @integrations-datashapes-input-change-conditional-flow
  Scenario: Data shapes propagation into conditional flow
    When select the "Webhook" connection
    And select "Incoming Webhook" integration action
    And fill in values by element data-testid
      | contextpath | test-webhook |
    And click on the "Next" button
    And fill in values by element data-testid
      | describe-data-shape-form-kind-input | JSON Instance |
    And fill text into text-editor
      | {"message":"John"} |
    And click on the "Next" button
    Then check visibility of page "Choose a Finish Connection"

    When select the "Log" connection
    And fill in values by element data-testid
      | contextloggingenabled | true |
      | bodyloggingenabled    | true |
    And click on the "Next" button

    When add integration step on position "0"
    And select the "Conditional Flows" connection
    And select "Advanced expression builder" integration action

    When Add another condition
    And fill in values by element data-testid
      | flowconditions-0-condition | ${body.message} == 'Shaco' |
      | flowconditions-1-condition | ${body.message} == 'Clone' |
    And fill in values by element data-testid
      | usedefaultflow | true |
    And click on the "Next" button
    And click on the "Next" button

    Then validate that input datashape on step 1 contains "json-instance"
    And validate that input datashape on step 2 contains "json-instance"

    When delete step on position 1
    And select the "PostgresDB" connection
    And select "Periodic SQL invocation" integration action
    And fill in periodic query input with "SELECT * FROM CONTACT" value
    And fill in period input with "5" value
    And select "Seconds" from sql dropdown
    And click on the "Next" button

    Then validate that input datashape on step 1 contains "SQL Result (Collection)"
    And validate that input datashape on step 2 contains "SQL Result (Collection)"
