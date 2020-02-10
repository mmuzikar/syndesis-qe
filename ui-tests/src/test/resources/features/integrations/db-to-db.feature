# @sustainer: mastepan@redhat.com

@ui
@database
@datamapper
@integrations-db-to-db
Feature: Integration - DB to DB

  Background: Clean application state
    Given clean application state
    And log into the Syndesis
    And reset content of "todo" table
    And reset content of "CONTACT" table
    And inserts into "CONTACT" table
      | Joe | Jackson | Red Hat | db |

#
#  1. select - update
#
  @smoke
  @db-connection-crud-1-read-update
  Scenario: Read and update operations

    Then inserts into "todo" table
      | Joe |

    When navigate to the "Home" page
    And click on the "Create Integration" link to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

    When select the "PostgresDB" connection
    And select "Periodic SQL invocation" integration action
    #Then check visibility of page "Periodic SQL Invocation"
    Then check "Next" button is "Disabled"
    Then fill in periodic query input with "SELECT * FROM CONTACT" value
    Then fill in period input with "5" value
    Then select "Seconds" from sql dropdown
    And click on the "Next" button

    # select postgresDB connection as 'to' point
    Then check visibility of page "Choose a Finish Connection"
    When select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    #wip this query doesnt work ftb #698
    Then fill in invoke query input with "UPDATE TODO SET completed=1 WHERE TASK = :#TASK" value
    And click on the "Next" button


    # add split step

    When add integration step on position "0"
    And select "Split" integration step
    And click on the "Next" button

    # add data mapper step
    When add integration step on position "1"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    And create data mapper mappings
      | first_name | TASK |

    And click on the "Done" button
    And publish integration
    And set integration name "CRUD1-read-update E2E"
    And publish integration
    Then wait until integration "CRUD1-read-update E2E" gets into "Running" state

    Then check that query "SELECT * FROM TODO WHERE completed = 1" has some output


#
#  2. select - insert
#
  @db-connection-crud-2-read-create
  Scenario: Read and create operations
    When navigate to the "Home" page
    And click on the "Create Integration" link to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

      # select salesforce connection as 'from' point
    When select the "PostgresDB" connection
    And select "Periodic SQL invocation" integration action
    #Then check visibility of page "Periodic SQL Invocation"
    #@wip this (disabled) functionality is not yet available
    Then check "Next" button is "Disabled"
    Then fill in periodic query input with "SELECT * FROM CONTACT" value
    Then fill in period input with "5" value
    Then select "Seconds" from sql dropdown
    And click on the "Next" button

    # select postgresDB connection as 'to' point
    Then check visibility of page "Choose a Finish Connection"
    When select the "PostgresDB" connection
    And select "Invoke SQL" integration action
#    @wip - bug to be reported, wrong is: (:#TASK, 2). (:#TASK, :#MASK) is OK
    Then fill in invoke query input with "INSERT INTO TODO(task, completed) VALUES (:#TASK, 2)" value
    And click on the "Next" button

      # add split step
    # Then check visibility of page "Add to Integration"
    When add integration step on position "0"
    And select "Split" integration step
    And click on the "Next" button

      # add data mapper step
    # Then check visibility of page "Add to Integration"
    When add integration step on position "1"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    And create data mapper mappings
      | first_name | TASK |

#    And scroll "top" "right"
    And click on the "Done" button
    And publish integration
    And set integration name "db-connection-crud-2-read-create"
    And publish integration
    Then wait until integration "db-connection-crud-2-read-create" gets into "Running" state

    Then validate that all todos with task "Joe" have value completed "2", period in ms: "5000"

#
#  3. select - delete
#
  @db-connection-crud-3-read-delete
  Scenario: Read and delete operations
    Then inserts into "todo" table
      | Joe |
    Then inserts into "todo" table
      | Jimmy |

    When navigate to the "Home" page
    And click on the "Create Integration" link to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

      # select salesforce connection as 'from' point
    When select the "PostgresDB" connection
    And select "Periodic SQL invocation" integration action
    #Then check visibility of page "Periodic SQL Invocation"
    #@wip this (disabled) functionality is not yet available
    Then check "Next" button is "Disabled"
    Then fill in periodic query input with "SELECT * FROM CONTACT WHERE first_name = 'Joe'" value
    Then fill in period input with "10" value
    Then select "Seconds" from sql dropdown
    And click on the "Next" button

    # select postgresDB connection as 'to' point
    Then check visibility of page "Choose a Finish Connection"
    When select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    #wip this query doesnt work ftb #698
    Then fill in invoke query input with "DELETE FROM TODO WHERE task = :#TASK" value
    And click on the "Next" button

    # add split step
    # Then check visibility of page "Add to Integration"
    When add integration step on position "0"
    And select "Split" integration step
    And click on the "Next" button

    # add data mapper step
    # Then check visibility of page "Add to Integration"
    When add integration step on position "1"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
#    And sleep for "16000" ms
    And create data mapper mappings
      | first_name | TASK |

    And click on the "Done" button
    And publish integration
    And set integration name "CRUD3-read-delete E2E"
    And publish integration
    Then wait until integration "CRUD3-read-delete E2E" gets into "Running" state

    Then validate that number of all todos with task "Joe" is "0"
    Then validate that number of all todos with task "Jimmy" is "1"

#
#  4. select - create (via buildin procedure)
#
  @ENTESB-12415
  @db-connection-crud-4-read-update-inbuilt
  Scenario: Read and create operations on stored procedure
      # INSERT INTO CONTACT(first_name, last_name, company, lead_source) VALUES('Josef','Stieranka','Istrochem','db');


    When inserts into "contact" table
      | Josef | Stieranka | Istrochem | db |

    Then navigate to the "Home" page
    And click on the "Create Integration" link to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

      # select salesforce connection as 'from' point
    When select the "PostgresDB" connection
    And select "Periodic SQL invocation" integration action
    #Then check visibility of page "Periodic SQL Invocation"
    #@wip this (disabled) functionality is not yet available
    Then check "Next" button is "Disabled"
    Then fill in periodic query input with "SELECT * FROM CONTACT" value
    Then fill in period input with "10" value
    Then select "Seconds" from sql dropdown
    And click on the "Next" button

    # select postgresDB connection as 'to' point
    Then check visibility of page "Choose a Finish Connection"
    When select the "PostgresDB" connection
    And select "Invoke Stored Procedure" integration action
    And select "add_lead" from "procedureName" dropdown
    And click on the "Next" button

    # add split step
    # Then check visibility of page "Add to Integration"
    When add integration step on position "0"
    And select "Split" integration step
    And click on the "Next" button

      # add data mapper step
    # Then check visibility of page "Add to Integration"
    When add integration step on position "1"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui

    Then create data mapper mappings
      | company     | company             |
      | last_name   | first_and_last_name |
      | lead_source | lead_source         |

#    And scroll "top" "right"
    And click on the "Done" button
    And publish integration
    And set integration name "db-connection-crud-4-read-update-inbuilt"
    And publish integration
    Then wait until integration "db-connection-crud-4-read-update-inbuilt" gets into "Running" state
    Then validate add_lead procedure with last_name: "Stieranka", company: "Istrochem"



#
#  5. builtin sql query checker
#
  @db-connection-5-sqlquery-checker
  Scenario: Sql query checker
    Then inserts into "todo" table
      | Joe |
    Then inserts into "todo" table
      | Jimmy |

    When navigate to the "Home" page
    And click on the "Create Integration" link to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

      # select salesforce connection as 'from' point
    When select the "PostgresDB" connection
    And select "Periodic SQL invocation" integration action
    #Then check visibility of page "Periodic SQL Invocation"
    #@wip this (disabled) functionality is not yet available
    Then check "Next" button is "Disabled"
#    wrong query:
    Then fill in periodic query input with "SELECT * FROM CONTACT-A" value
    Then fill in period input with "5" value
    Then select "Seconds" from sql dropdown
    And click on the "Next" button
    # Issue:https://github.com/syndesisio/syndesis/issues/2823
    And check visibility of alert notification
    #@wip time_unit_id is not yet available

    Then fill in periodic query input with "SELECT * FROM CONTACT" value
    And click on the "Next" button

    # select postgresDB connection as 'to' point
    #Then check visibility of page "Choose a Finish Connection"
    When select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    Then fill in invoke query input with "UPDATE TODO-A SET completed=1 WHERE task like '%:#TASK%'" value
    And click on the "Next" button
    And check visibility of alert notification
    Then fill in invoke query input with "DELETE FROM TODO-A WHERE task like '%:#TASK%'" value
    And click on the "Next" button
    And check visibility of alert notification
    Then fill in invoke query input with "INSERT INTO TODO-A(task) VALUES(:#TASK)" value
    And click on the "Next" button
    And check visibility of alert notification
#    correct one:
    #wip this query doesnt work ftb #698
    Then fill in invoke query input with "DELETE FROM TODO WHERE task = :#TASK" value
    And click on the "Next" button

      # add data mapper step
    # Then check visibility of page "Add to Integration"
    When add integration step on position "0"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    And open data mapper collection mappings
    And create data mapper mappings
      | first_name | TASK |

    And click on the "Done" button
    And publish integration
    And set integration name "DB Connection 5 SQL query checker E2E"
    And publish integration
    Then wait until integration "DB Connection 5 SQL query checker E2E" gets into "Running" state

    Then validate that number of all todos with task "Joe" is "0"
    Then validate that number of all todos with task "Jimmy" is "1"

  @db-insert-multiple-rows
  Scenario: Inserting multiple rows 
    Given Set Todo app credentials
    Then inserts into "todo" table
      | Joe |
    And inserts into "todo" table
      | Jimmy |
    When click on the "Customizations" link
    And navigate to the "API Client Connectors" page
    And click on the "Create API Connector" link
    And check visibility of page "Upload Swagger Specification"
    Then upload swagger file
      | file | swagger/connectors/todo.swagger.yaml |

    When click on the "Next" button
    Then check visibility of page "Review Actions"

    When click on the "Next" link
    Then check visibility of page "Specify Security"

    When set up api connector security
      | authType | HTTP Basic Authentication |
    And click on the "Next" button
    And fill in values by element data-testid
      | name     | Todo connector |
      | basepath | /api           |
    And fill in TODO API host URL
    And click on the "Save" button

    When created connections
      | Todo connector | todo | Todo connection | no validation |
    And navigate to the "Connections" page

    When navigate to the "Home" page
    And click on the "Create Integration" link

    Then check that position of connection to fill is "Start"
    When select the "Timer" connection
    And select "Simple" integration action
    And click on the "Done" button

    Then check that position of connection to fill is "Finish"
    When select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    And fill in invoke query input with "INSERT INTO contact (first_name) VALUES (:#task)" value
    And fill in values by element data-testid
      | batch | true |
    And click on the "Done" button

    When add integration step on position "0"
    And select the "Todo connection" connection
    And select "List all tasks" integration action
    And click on the "Next" button

    When add integration step on position "1"
    And select the "Data Mapper" connection
    And open data mapper collection mappings
    And create data mapper mappings
      | body.task | task |

    And click on the "Done" button
    And publish integration
    And set integration name "db-insert-multiple-rows"
    And publish integration
    Then wait until integration "db-insert-multiple-rows" gets into "Running" state

    And check that query "SELECT * FROM contact WHERE first_name = 'Jimmy'" has some output
    And check that query "SELECT * FROM contact WHERE first_name = 'Joe'" has some output

  @reproducer
  @ENTESB-11415
  Scenario: SQL statement doesn't contain text from the previous SQL step
    When navigate to the "Home" page
    And click on the "Create Integration" link to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

    When select the "Timer" connection
    And select "Simple" integration action
    And fill in values by element data-testid
      | period        | 1       |
      | select-period | Minutes |
    And click on the "Next" button

    Then check that position of connection to fill is "Finish"
    When select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    And fill in invoke query input with "INSERT INTO todo (task) VALUES ('buy milk')" value
    And fill in values by element data-testid
      | batch | true |
    And click on the "Done" button

    And add integration step on position "0"
    And select the "PostgresDB" connection
    And select "Invoke SQL" integration action

    Then check that sql query is ""