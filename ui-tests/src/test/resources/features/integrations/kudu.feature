# @sustainer: mcada@redhat.com

# Kudu server docker image:   https://hub.docker.com/r/lgarciaac/kudu-docker
# Kudu server github:         https://github.com/lgarciaaco/kudu-docker
# Kudu rest API docker image: https://hub.docker.com/r/mcada/syndesis-kudu-rest-api
# Kudu rest API github:       https://github.com/mcada/kudu-rest-app


# I was not able to connect both syndesis instance and testsuite to single kudu server
# due to kudu behavior, TCP communication and  DNS issues - tservers were always unavailable
# from either syndesis or tests. This issue was resolved with following communication schema:
#
# localhost           |     openshift
#
# syndesis tests .......  kudu rest api
#                             :
#                             :
#                         kudu server

@ui
@kudu
@database
@datamapper
@integrations-kudu
Feature: Kudu connector

  Background: Clean application state
    Given clean application state
    And deploy Kudu server
    And deploy Kudu rest API
    And create table in Kudu server
    And clean "todo" table
    And set Kudu credentials
    And log into the Syndesis
    And created connections
      | Apache Kudu | kudu | KuduConnector | description |
    And navigate to the "Home" page

#
#  1. Insert data into kudu table
#
  @integrations-kudu-insert
  Scenario: Insert data into kudu table
    When inserts into "todo with id" table
      | 1 | FirstValue |

    And click on the "Create Integration" button to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

    When select the "PostgresDB" connection
    And select "Periodic SQL Invocation" integration action
    And fill in periodic query input with "SELECT * FROM todo where task = 'FirstValue'" value
    And fill in period input with "60" value
    And select "Seconds" from sql dropdown
    And click on the "Done" button
    Then check that position of connection to fill is "Finish"

    When select the "KuduConnector" connection
    And select "Insert a row in a kudu table" integration action
    And fill in values
      | Table | my-table |
    Then click on the "Done" button

    When add integration step on position "0"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    And create data mapper mappings
      | id   | key   |
      | task | value |
    And click on the "Done" button

    When click on the "Save" button
    And set integration name "Integration_kudu_insert"
    And publish integration
    And wait until integration "Integration_kudu_insert" gets into "Running" state

    Then check that Kudu server table contains inserted data
    And delete table from Kudu server

#
#  2. Scan kudu table
#
  @integrations-kudu-scan
  Scenario: Scan kudu table
    When insert a row into Kudu server table
    And click on the "Create Integration" button to create a new integration.
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

    When select the "KuduConnector" connection
    And select "Scan a kudu table" integration action
    And fill in values
      | Table | my-table |
    And click on the "Done" button
    Then check that position of connection to fill is "Finish"

    When select the "Log" connection
    And fill in values
      | Message Context | true |
      | Message Body    | true |

    Then click on the "Done" button

    When click on the "Save" button
    And set integration name "Integration_kudu_scan"
    And publish integration

    When wait until integration "Integration_kudu_scan" gets into "Running" state
    Then validate that logs of integration "Integration_kudu_scan" contains string "FirstValue"
    And delete table from Kudu server


#
#  2. Scan kudu table
#
  @mqtt
  @integrations-kudu-mqtt
  Scenario: Kudu integration with mqtt
    When deploy ActiveMQ broker
    And created connections
      | MQTT Message Broker | QE MQTT | MQTT test connection | some description |

    When navigate to the "Home" page
    And click on the "Create Integration" button to create a new integration.
    Then check visibility of visual integration editor

    # select connection as 'start' point
    And check that position of connection to fill is "Start"
    When select the "MQTT test connection" connection
    And select "Subscribe" integration action
    And fill in "topic" action configure component input with "news" value
    And click on the "Next" button
    And fill in values
      | Select Type | JSON Instance |
    #only available after type is selected
    And fill in values by element ID
      | specification | {"key" : 1,"value" : "FirstValue"} |
    And click on the "Done" button
    Then check that position of connection to fill is "Finish"

    # select connection as 'finish' point
    When select the "KuduConnector" connection
    And select "Insert a row in a kudu table" integration action
    And fill in values
      | Table | my-table |
    Then click on the "Done" button

    When add integration step on position "0"
    And select "Data Mapper" integration step
    Then check visibility of data mapper ui
    And create data mapper mappings
      | key   | key   |
      | value | value |
    And click on the "Done" button

    When click on the "Save" button
    And set integration name "Integration_kudu_insert"
    And publish integration
    And wait until integration "Integration_kudu_insert" gets into "Running" state

    When send mqtt message to "news" topic
    Then check that Kudu server table contains inserted data
