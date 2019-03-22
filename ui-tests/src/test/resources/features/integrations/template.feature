#@sustainer: mmuzikar@redhat.com

@ui
@data-mapper
@template
Feature: Templates

  Background: Clean application state
    Given clean application state
    And log into the Syndesis
    And delete emails from "jbossqa.fuse@gmail.com" with subject "syndesis-template-test"
    And created connections
      | Gmail | QE Google Mail | QE Google Mail | SyndesisQE Slack test |

  @db-template-mustache-send
  Scenario: Send an Email with text formatted by Mustache template
    # create integration
    When navigate to the "Home" page
    And click on the "Create Integration" button to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

      # select twitter connection as 'from' point
    When select the "PostgresDB" connection
    And select "Periodic SQL invocation" integration action
    Then fill in invoke query input with "SELECT * FROM contact" value
    And click on the "Done" button
    And check that position of connection to fill is "Finish"

    # select postgresDB connection as 'to' point
    Then check visibility of page "Choose a Finish Connection"
    When select the "QE Google Mail" connection
    And select "Send Email" integration action
    Then fill in values
      | Email to | jbossqa.fuse@gmail.com |
      | Email subject | syndesis-template-test |
    And click on the "Done" button

    #adding mustache template to format the mail
    Then check visibility of page "Add to Integration"
    When add integration step on position "0"
    Then select "Split" integration step

    #adding mustache template to format the mail
    Then check visibility of page "Add to Integration"
    When add integration step on position "1"
    Then select "Template" integration step
    And set the template type to "Mustache"
    And input template "{{firstname}} {{surname}} works at {{company}}"
    And click on the "Done" button

      # add data mapper step for template
    Then check visibility of page "Add to Integration"
    When add integration step on position "1"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    And create data mapper mappings
      | first_name | firstname |
      | last_name  | surname   | 
      | company     | company   |
    And scroll "top" "right"
    And click on the "Done" button

    #add mapping for email step
    Then check visibility of page "Add to Integration"
    When add integration step on position "3"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    And open data bucket "4 - Template JSON Schema"
    And create data mapper mappings
      | message      | text    |
    And scroll "top" "right"
    And click on the "Done" button

    # finish and save integration
    When click on the "Save" button
    And set integration name "DB to gmail (template)"
    And publish integration

    Then Integration "DB to gmail (template)" is present in integrations list
    # wait for integration to get in active state
    And wait until integration "DB to gmail (template)" gets into "Running" state
    
    #Wait for integration to really be running
    And sleep for "10000" ms

    Then check that email from "jbossqa.fuse@gmail.com" with subject "syndesis-template-test" and text "Joe Jackson works at Red Hat" exists

  @db-template-velocity-send
  Scenario: Send an Email with text formatted by Velocity template
    # create integration
    When navigate to the "Home" page
    And click on the "Create Integration" button to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

      # select twitter connection as 'from' point
    When select the "PostgresDB" connection
    And select "Periodic SQL invocation" integration action
    Then fill in invoke query input with "SELECT * FROM contact" value
    And click on the "Done" button
    And check that position of connection to fill is "Finish"

    # select postgresDB connection as 'to' point
    Then check visibility of page "Choose a Finish Connection"
    When select the "QE Google Mail" connection
    And select "Send Email" integration action
    Then fill in values
      | Email to | jbossqa.fuse@gmail.com |
      | Email subject | syndesis-template-test |
    And click on the "Done" button

    #adding mustache template to format the mail
    Then check visibility of page "Add to Integration"
    When add integration step on position "0"
    Then select "Split" integration step

    #adding mustache template to format the mail
    Then check visibility of page "Add to Integration"
    When add integration step on position "1"
    Then select "Template" integration step
    And set the template type to "Velocity"
    And input template "$firstname $surname works at $company"
    And click on the "Done" button

      # add data mapper step for template
    Then check visibility of page "Add to Integration"
    When add integration step on position "1"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    And create data mapper mappings
      | first_name | firstname |
      | last_name  | surname   | 
      | company     | company   |
    And scroll "top" "right"
    And click on the "Done" button

    #add mapping for email step
    Then check visibility of page "Add to Integration"
    When add integration step on position "3"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    And open data bucket "4 - Template JSON Schema"
    And create data mapper mappings
      | message      | text    |
    And scroll "top" "right"
    And click on the "Done" button

    # finish and save integration
    When click on the "Save" button
    And set integration name "DB to gmail (template)"
    And publish integration

    Then Integration "DB to gmail (template)" is present in integrations list
    # wait for integration to get in active state
    And wait until integration "DB to gmail (template)" gets into "Running" state
    
    #Wait for integration to really be running
    And sleep for "10000" ms

    Then check that email from "jbossqa.fuse@gmail.com" with subject "syndesis-template-test" and text "Joe Jackson works at Red Hat" exists

  @db-template-freemarker-send
  Scenario: Send an Email with text formatted by Freemarker template
    # create integration
    When navigate to the "Home" page
    And click on the "Create Integration" button to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

      # select twitter connection as 'from' point
    When select the "PostgresDB" connection
    And select "Periodic SQL invocation" integration action
    Then fill in invoke query input with "SELECT * FROM contact" value
    And click on the "Done" button
    And check that position of connection to fill is "Finish"

    # select postgresDB connection as 'to' point
    Then check visibility of page "Choose a Finish Connection"
    When select the "QE Google Mail" connection
    And select "Send Email" integration action
    Then fill in values
      | Email to | jbossqa.fuse@gmail.com |
      | Email subject | syndesis-template-test |
    And click on the "Done" button

    #adding mustache template to format the mail
    Then check visibility of page "Add to Integration"
    When add integration step on position "0"
    Then select "Split" integration step

    #adding freemarker template to format the mail
    Then check visibility of page "Add to Integration"
    When add integration step on position "1"
    Then select "Template" integration step
    And set the template type to "freemarker"
    And input template "${firstname} ${surname} works at ${company}"
    And click on the "Done" button

      # add data mapper step for template
    Then check visibility of page "Add to Integration"
    When add integration step on position "1"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    And create data mapper mappings
      | first_name | firstname |
      | last_name  | surname   | 
      | company     | company   |
    And scroll "top" "right"
    And click on the "Done" button

    #add mapping for email step
    Then check visibility of page "Add to Integration"
    When add integration step on position "3"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    And open data bucket "4 - Template JSON Schema"
    And create data mapper mappings
      | message      | text    |
    And scroll "top" "right"
    And click on the "Done" button

    # finish and save integration
    When click on the "Save" button
    And set integration name "DB to gmail (template)"
    And publish integration

    Then Integration "DB to gmail (template)" is present in integrations list
    # wait for integration to get in active state
    And wait until integration "DB to gmail (template)" gets into "Running" state
    
    #Wait for integration to really be running
    And sleep for "10000" ms

    Then check that email from "jbossqa.fuse@gmail.com" with subject "syndesis-template-test" and text "Joe Jackson works at Red Hat" exists

  @db-template-mustache-file-upload
  Scenario: Send an Email with text formatted by Mustache template uploaded from resources/templates/mustache.tpl
    # create integration
    When navigate to the "Home" page
    And click on the "Create Integration" button to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

      # select twitter connection as 'from' point
    When select the "PostgresDB" connection
    And select "Periodic SQL invocation" integration action
    Then fill in invoke query input with "SELECT * FROM contact" value
    And click on the "Done" button
    And check that position of connection to fill is "Finish"

    # select postgresDB connection as 'to' point
    Then check visibility of page "Choose a Finish Connection"
    When select the "QE Google Mail" connection
    And select "Send Email" integration action
    Then fill in values
      | Email to | jbossqa.fuse@gmail.com |
      | Email subject | syndesis-template-test |
    And click on the "Done" button

    #adding mustache template to format the mail
    Then check visibility of page "Add to Integration"
    When add integration step on position "0"
    Then select "Split" integration step

    #adding mustache template to format the mail
    Then check visibility of page "Add to Integration"
    When add integration step on position "1"
    Then select "Template" integration step
    And set the template type to "Mustache"
    And upload template from resource "templates/mustache.tpl"
    And click on the "Done" button

      # add data mapper step for template
    Then check visibility of page "Add to Integration"
    When add integration step on position "1"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    And create data mapper mappings
      | first_name | firstname |
      | last_name  | surname   | 
      | company     | company   |
    And scroll "top" "right"
    And click on the "Done" button

    #add mapping for email step
    Then check visibility of page "Add to Integration"
    When add integration step on position "3"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    And open data bucket "4 - Template JSON Schema"
    And create data mapper mappings
      | message      | text    |
    And scroll "top" "right"
    And click on the "Done" button

    # finish and save integration
    When click on the "Save" button
    And set integration name "DB to gmail (template)"
    And publish integration

    Then Integration "DB to gmail (template)" is present in integrations list
    # wait for integration to get in active state
    And wait until integration "DB to gmail (template)" gets into "Running" state
    
    #Wait for integration to really be running
    And sleep for "10000" ms

    Then check that email from "jbossqa.fuse@gmail.com" with subject "syndesis-template-test" and text "Joe Jackson works at Red Hat" exists