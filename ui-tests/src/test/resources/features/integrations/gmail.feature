# @sustainer: mcada@redhat.com

@ui
@gmail
@database
@datamapper
@integrations-gmail
Feature: Google mail Connector

  Background: Clean application state
    Given clean application state
    And reset content of "contact" table
    And delete emails from "jbossqa.fuse@gmail.com" with subject "syndesis-test"
    And log into the Syndesis
    And created connections
      | Gmail | QE Google Mail | My GMail Connector | SyndesisQE Slack test |
    And navigate to the "Home" page

#
#  1. Send an e-mail
#
  @gmail-send
  Scenario: Send an e-mail

    # create integration
    When click on the "Create Integration" button to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

    When select the "PostgresDB" connection
    And select "Periodic SQL Invocation" integration action
    And fill in periodic query input with "select company from contact limit(1)" value
    And fill in period input with "10" value
    And select "Minutes" from sql dropdown
    And click on the "Done" button

    Then check that position of connection to fill is "Finish"

    When select the "My GMail Connector" connection
    And select "Send Email" integration action
    And fill in values
      | Email to      | jbossqa.fuse@gmail.com |
      | Email subject | syndesis-test          |
    And click on the "Done" button

    # add split step
    Then check visibility of page "Add to Integration"
    When add integration step on position "0"
    And select "Split" integration step

    # add data mapper step
    And add integration step on position "1"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    And create data mapper mappings
      | company | text |
    And scroll "top" "right"
    And click on the "Done" button

    # finish and save integration
    When click on the "Save" button
    And set integration name "Integration_gmail_send"
    And publish integration

    Then Integration "Integration_gmail_send" is present in integrations list
    # wait for integration to get in active state
    And wait until integration "Integration_gmail_send" gets into "Running" state

    #give gmail time to receive mail
    When sleep for "10000" ms
    Then check that email from "jbossqa.fuse@gmail.com" with subject "syndesis-test" and text "Red Hat" exists
    And delete emails from "jbossqa.fuse@gmail.com" with subject "syndesis-test"

#
#  2. Receive an e-mail
#
  @gmail-receive
  Scenario: Receive an e-mail

    # create integration
    When click on the "Create Integration" button to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

    When select the "My GMail Connector" connection
    And select "Receive Email" integration action
    And fill in values
      | labels | syndesis-test |
    And click on the "Done" button

    Then check that position of connection to fill is "Finish"

    When select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    And fill in invoke query input with "insert into CONTACT values ('Prokop' , 'Dvere', :#COMPANY , 'some lead', '1999-01-01')" value
    And click on the "Done" button

    # add data mapper step
    And add integration step on position "0"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    And create data mapper mappings
      | text | COMPANY |
    And scroll "top" "right"
    And click on the "Done" button


    # finish and save integration
    And click on the "Save" button
    And set integration name "Integration_gmail_receive"
    And publish integration

    # assert integration is present in list
    Then Integration "Integration_gmail_receive" is present in integrations list
    # wait for integration to get in active state
    And wait until integration "Integration_gmail_receive" gets into "Running" state

    #give gmail time to receive mail
    When send an e-mail
    #there is 30s pull time in gmail and delay when an e-mail is sent so we have to wait here
    And sleep for "60000" ms
    Then check that query "select * from contact where first_name = 'Prokop' AND last_name = 'Dvere'" has some output
    And delete emails from "jbossqa.fuse@gmail.com" with subject "syndesis-tests"
