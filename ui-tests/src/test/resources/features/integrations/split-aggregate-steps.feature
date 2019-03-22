# @sustainer: mastepan@redhat.com

@ui
@split
Feature: Integration - DB to DB with split/aggregate

  Background: Clean application state
    Given clean application state
    And log into the Syndesis
    And reset content of "todo" table

  @aggregate
  @split-aggregate-flow-1
  Scenario: Split and aggregate integration flow

    When inserts into "todo" table
      | task1 |
      | task1 |
      | task1 |

    When navigate to the "Home" page
    And click on the "Create Integration" button to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

        # select SQL connection as 'from' point
    When select the "PostgresDB" connection
    And select "Periodic SQL invocation" integration action
        #Then check visibility of page "Periodic SQL Invocation"
    Then check "Next" button is "Disabled"
    When fill in invoke query input with "DELETE FROM todo WHERE task='finalTask'" value
    And fill in period input with "10" value
    And select "Seconds" from sql dropdown
    And click on the "Next" button

        # select postgresDB connection as 'to' point
    Then check visibility of page "Choose a Finish Connection"
    When select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    And fill in invoke query input with "INSERT INTO todo(task) VALUES ('finalTask')" value
    And click on the "Next" button

        # add DB step Select
    Then check visibility of page "Add to Integration"
    When add integration step on position "0"
    And select "PostgresDB" integration step
    And select "Invoke SQL" integration action
    And fill in periodic query input with "SELECT * FROM todo WHERE task='task1'" value
    And click on the "Next" button

        # add Split step
    Then check visibility of page "Add to Integration"
    When add integration step on position "1"
    And select "Split" integration step

        # add DB step Insert
    Then check visibility of page "Add to Integration"
    When add integration step on position "2"
    And select "PostgresDB" integration step
    And select "Invoke SQL" integration action
    And fill in invoke query input with "INSERT INTO todo(task) VALUES ('startTask')" value
    And click on the "Next" button

        # add DB step Delete
    Then check visibility of page "Add to Integration"
    When add integration step on position "3"
    And select "PostgresDB" integration step
    And select "Invoke SQL" integration action
    And fill in invoke query input with "DELETE FROM todo WHERE task='task1'" value
    And click on the "Next" button

        # add Aggregate step
    Then check visibility of page "Add to Integration"
    When add integration step on position "4"
    And select "Aggregate" integration step
    Then check visibility of page "Add to Integration"

    When publish integration
    And set integration name "Split/Aggregate"
    And publish integration
    And wait until integration "Split/Aggregate" gets into "Running" state

    Then validate that number of all todos with task "startTask" is "3", period in ms: "15000"
    And validate that number of all todos with task "finalTask" is "1", period in ms: "1"
