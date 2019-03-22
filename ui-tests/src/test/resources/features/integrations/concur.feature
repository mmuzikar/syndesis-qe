# @sustainer: mcada@redhat.com
#
# Concur credentials callback can be only changed by concur support, to test it
# one must install syndesis with --route=syndesis.my-minishift.syndesis.io and
# redirect this route to correct minishift/openshift IP via /etc/hosts file.
#
# I am also not able to create verification steps, because communication would
# go through concur WS and we have different credentials for that and they point
# to another concur instance thus not able to validate what our oauth credentials
# created. For more information ask kstam@redhat.com as he said it is not possible
# to have oauth and ws credentials to point to the same developer instance.
#

@ui
# Until the issue with @concur support is resolved, concur testing will be disabled.
@disabled
@concur
@database
@datamapper
@integrations-concur
Feature: Concur Connector

  Background: Clean application state
    Given clean application state
    And reset content of "contact" table
    And log into the Syndesis
    And navigate to the "Settings" page
    And fill all oauth settings
    And create connections using oauth
      | SAP Concur | Test-Concur-connection |
    And invoke database query "insert into CONTACT values ('Akali' , 'Queen', '50' , 'some lead', '1999-01-01')"
    And insert into contact database randomized concur contact with name "Xerath" and list ID "gWnytPb$pHxNJMPz4yL0nosnCQ4r30gRWs4w"
    And navigate to the "Home" page

  @concur-list-get
  Scenario: Check message of concur get list action

    # create integration
    When click on the "Create Integration" button to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

    When select the "PostgresDB" connection
    And select "Periodic SQL Invocation" integration action
    Then check "Next" button is "Disabled"

    When fill in periodic query input with "SELECT * FROM CONTACT where first_name = 'Akali'" value
    And fill in period input with "60" value
    And select "Seconds" from sql dropdown
    And click on the "Done" button
    Then check that position of connection to fill is "Finish"

    When select the "Log" connection
    And fill in values
      | Message Context | true |
      | Message Body    | true |
    Then click on the "Done" button

    When add integration step on position "0"
    And select the "Test-Concur-connection" connection
    Then select "Gets all lists" integration action

    When add integration step on position "0"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    And create data mapper mappings
      | company | parameters.limit |
    And click on the "Done" button

    When add integration step on position "2"
    And select "Log" integration step
    And fill in values
      | Message Context | true |
      | Message Body    | true |
    Then click on the "Done" button

    # finish and save integration
    When click on the "Save" button
    And set integration name "Integration_with_concur"
    And publish integration
    Then Integration "Integration_with_concur" is present in integrations list
    And wait until integration "Integration_with_concur" gets into "Running" state
    And validate that logs of integration "Integration_with_concur" contains string "gWnytPb$pHxNJMPz4yL0nosnCQ4r30gRWs4w"

    Then reset content of "contact" table


  @concur-listitems-get
  Scenario: Check message of concur get listitems action

    # create integration
    When click on the "Create Integration" button to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

    When select the "PostgresDB" connection
    And select "Periodic SQL Invocation" integration action
    Then check "Next" button is "Disabled"

    When fill in periodic query input with "SELECT * FROM CONTACT where first_name = 'Akali'" value
    And fill in period input with "60" value
    And select "Seconds" from sql dropdown
    And click on the "Done" button
    Then check that position of connection to fill is "Finish"

    When select the "Log" connection
    And fill in values
      | Message Context | true |
      | Message Body    | true |
    Then click on the "Done" button

    When add integration step on position "0"
    And select the "Test-Concur-connection" connection
    Then select "Gets all listitems " integration action

    When add integration step on position "0"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    And create data mapper mappings
      | company | parameters.limit |
    And click on the "Done" button

    When add integration step on position "2"
    And select "Log" integration step
    And fill in values
      | Message Context | true |
      | Message Body    | true |
    Then click on the "Done" button

    # finish and save integration
    When click on the "Save" button
    And set integration name "Integration_with_concur"
    And publish integration

    When navigate to the "Integrations" page
    Then Integration "Integration_with_concur" is present in integrations list
    And wait until integration "Integration_with_concur" gets into "Running" state
    And validate that logs of integration "Integration_with_concur" contains string "gWnytPb$pHxNJMPz4yL0nosnCQ4r30gRWs4w"

    Then reset content of "contact" table

  @concur-listitem-create-delete
  Scenario: Check if create and delete listitems actions work together

    # create integration
    When click on the "Create Integration" button to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"
    # DB - nothing
    When select the "PostgresDB" connection
    And select "Periodic SQL Invocation" integration action
    Then check "Next" button is "Disabled"

    When fill in periodic query input with "SELECT * FROM CONTACT where first_name = 'Xerath'" value
    And fill in period input with "5" value
    And select "Minutes" from sql dropdown
    And click on the "Done" button
    Then check that position of connection to fill is "Finish"

    # DB - concur
    When select the "Test-Concur-connection" connection
    Then select "Delete listitem by ID" integration action

    # DB - concur - concur
    When add integration step on position "0"
    And select the "Test-Concur-connection" connection
    Then select "Create a new listitem" integration action
    #DB - mapper - concur - concur
    When add integration step on position "0"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    And create data mapper mappings
      | company     | body.ListID     |
      | last_name   | body.Name       |
      | lead_source | body.Level1Code |
    And click on the "Done" button
    # DB - mapper - concur - concur - concur
    When add integration step on position "2"
    And select the "Test-Concur-connection" connection
    Then select "Get a single listitem by ID" integration action
    # DB - mapper - concur - mapper - concur - concur
    When add integration step on position "2"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    And open data bucket "3 - Response"
    And create data mapper mappings
      | ID | parameters.id |

    And click on the "Done" button

    # DB - mapper - concur - mapper - concur - DB - concur
    When .*adds integration step on position "4"
    And select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    Then fill in invoke query input with "insert into CONTACT values ('Zilean' , 'Time', :#DESCRIPTION , 'some lead', '1999-01-01');" value
    And click on the "Done" button

    # DB - mapper - concur - mapper - concur - mapper - DB - concur
    When add integration step on position "4"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    When open data bucket "5 - Response"
    And create data mapper mappings
      | ID | DESCRIPTION |

    Then click on the "Done" button

    # DB - mapper - concur - mapper - concur - mapper - DB - concur
    When add integration step on position "6"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    When open data bucket "3 - Response"
    And .*open data bucket "1 - SQL Result"
    And create data mapper mappings
      | ID      | parameters.id     |
      | company | parameters.listId |


    Then click on the "Done" button

    # finish and save integration
    When click on the "Save" button
    And set integration name "Integration_with_concur"
    And publish integration
    Then Integration "Integration_with_concur" is present in integrations list
    And wait until integration "Integration_with_concur" gets into "Running" state

    Then check that query "select * from contact where first_name = 'Zilean'" has some output
