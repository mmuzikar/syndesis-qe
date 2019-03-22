# @sustainer: jsafarik@redhat.com

@ui
@database
@log
@sql-connector
Feature: SQL Connector

  Background: Clean application state
    Given clean application state
    And log into the Syndesis
    And reset content of "todo" table
    And navigate to the "Home" page

  #
  # 1. Check that SQL connector pulls all rows on exchange when SQL connector is as middle step
  # (having SQL connector as 'from' or 'to' doesn't cause issue from GH-3856)
  #
  @reproducer
  @gh-3856
  @sql-connector-all-rows
  Scenario: Check SQL pulls all rows

    When inserts into "todo" table
      | ABCsampleTODO |
      | XYZsampleTODO |
      | KLMsampleTODO |

    And click on the "Create Integration" button to create a new integration
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

    # select Timer as 'from' point
    When select the "Timer" connection
    And select "Simple Timer" integration action
    And fill in values by element ID
      | period        | 1       |
      | select-period | Minutes |
    And click on the "Done" button
    Then check visibility of page "Choose a Finish Connection"

    # select Log as 'to' point
    When select the "Log" connection
    And fill in values
      | Message Context | true |
      | Message Body    | true |
    Then click on the "Done" button

    # select postgresDB as middle point
    When add integration step on position "0"
    And select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    Then check "Next" button is "Disabled"

    # fill in postgresDB connection
    When fill in invoke query input with "SELECT * FROM  todo" value
    And click on the "Next" button

    # save the integration with new name and publish it
    When click on the "Publish" button
    And set integration name "DB_2_Log"
    And publish integration
    Then Integration "DB_2_Log" is present in integrations list
    And wait until integration "DB_2_Log" gets into "Running" state

    # validate that all items from db are present
    When sleep for "10000" ms
    Then validate that logs of integration "DB_2_Log" contains string "ABCsampleTODO"
    And validate that logs of integration "DB_2_Log" contains string "XYZsampleTODO"
    And validate that logs of integration "DB_2_Log" contains string "KLMsampleTODO"
