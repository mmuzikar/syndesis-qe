# @sustainer: alice.rum@redhat.com


@ui
@webhook
@database
@datamapper
@conditional-flow
@integrations-conditional-flows
Feature: Conditional flows - content base routing

  Background: Clean application state
    Given clean application state
    And reset content of "contact" table
    And log into the Syndesis
    And navigate to the "Home" page

    When click on the "Create Integration" link
    Then check visibility of visual integration editor
    And check that position of connection to fill is "Start"

    When select the "Webhook" connection
    And select "Incoming Webhook" integration action
    And fill in values by element data-testid
      | contextpath | test-webhook |
    And click on the "Next" button
    And force fill in values by element data-testid
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

  @ENTESB-13541
  @integrations-conditional-flows-icon-test-move-up
  Scenario: Conditional flows icon test - move up
    And select "Advanced expression builder" integration action
    When validate that condition count is equal to 1
    And fill in values by element data-testid
      | flowconditions-0-condition | first |
    Then validate condition content in condition flow step
      | 0 | first |

    When Add another condition
    Then validate that condition count is equal to 2
    And validate condition content in condition flow step
      | 1 |  |

    When click on the condition icon
      | 1 | UP |
    Then validate condition content in condition flow step
      | 0 |       |
      | 1 | first |

  @ENTESB-13541
  @integrations-conditional-flows-icon-test-delete
  Scenario: Conditional flows icon test - delete
    And select "Advanced expression builder" integration action
    When validate that condition count is equal to 1
    And fill in values by element data-testid
      | flowconditions-0-condition | first |
    Then validate condition content in condition flow step
      | 0 | first |

    When Add another condition
    Then validate that condition count is equal to 2

    When click on the condition icon
      | 0 | DELETE |
    When validate that condition count is equal to 1

    Then validate condition content in condition flow step
      | 0 |  |

  @ENTESB-13541
  @integrations-conditional-flows-icon-test-multiple-moves
  Scenario: Conditional flows icon test multiple moves
    And select "Advanced expression builder" integration action
    When validate that condition count is equal to 1
    And fill in values by element data-testid
      | flowconditions-0-condition | first |
    Then validate condition content in condition flow step
      | 0 | first |

    When Add another condition
    And Add another condition
    Then validate that condition count is equal to 3
    And validate condition content in condition flow step
      | 0 | first |
      | 1 |       |
      | 2 |       |

    When click on the condition icon
      | 1 | DOWN |
    Then validate condition content in condition flow step
      | 0 | first |
      | 1 |       |
      | 2 |       |

    When click on the condition icon
      | 0 | DOWN |
    Then validate condition content in condition flow step
      | 0 |       |
      | 1 | first |
      | 2 |       |

    When click on the condition icon
      | 1 | DOWN |
    Then validate condition content in condition flow step
      | 0 |       |
      | 1 |       |
      | 2 | first |

    When click on the condition icon
      | 2 | DELETE |
    Then validate condition content in condition flow step
      | 0 |  |
      | 1 |  |

  @integrations-conditional-flows-functional-test
  Scenario: Conditional flows - functional test
    And select "Advanced expression builder" integration action
    When Add another condition
    And fill in values by element data-testid
      | flowconditions-0-condition | ${body.message} == 'Shaco' |
      | flowconditions-1-condition | ${body.message} == 'Clone' |
    And fill in values by element data-testid
      | usedefaultflow | true |

    And click on the "Next" button
    And click on the "Next" button

    When configure condition on position 1
    And add integration step on position "0"
    And select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    And fill in invoke query input with "insert into contact(first_name) values ('Shaco was here')" value
    And click on the "Next" button
    Then check visibility of visual integration editor
    Then return to primary flow from integration flow from dropdown

    When configure condition on position 2
    And add integration step on position "0"
    And select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    And fill in invoke query input with "insert into contact(first_name) values ('Clone was here')" value
    And click on the "Next" button
    Then return to primary flow from integration flow from dropdown

    When configure condition on position 3
    And add integration step on position "0"
    And select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    And fill in invoke query input with "insert into contact(first_name) values ('Noone was here')" value
    And click on the "Next" button
    Then return to primary flow from integration flow from dropdown

    When click on the "Save" link
    And set integration name "integrations-conditional-flows-functional-test"
    And click on the "Save and publish" button
    And navigate to the "Integrations" page
    Then wait until integration "integrations-conditional-flows-functional-test" gets into "Running" state

    When select the "integrations-conditional-flows-functional-test" integration
    And invoke post request to webhook
      | integrations-conditional-flows-functional-test | test-webhook | {"message":"John"} | 204 |
    Then checks that query "select * from contact where first_name='Noone was here'" has 1 row output
    And checks that query "select * from contact where first_name='Shaco was here'" has 0 row output
    And checks that query "select * from contact where first_name='Clone was here'" has 0 row output

    When invoke post request to webhook
      | integrations-conditional-flows-functional-test | test-webhook | {"message":"Shaco"} | 204 |
    Then checks that query "select * from contact where first_name='Noone was here'" has 1 row output
    And checks that query "select * from contact where first_name='Shaco was here'" has 1 row output
    And checks that query "select * from contact where first_name='Clone was here'" has 0 row output

    When invoke post request to webhook
      | integrations-conditional-flows-functional-test | test-webhook | {"message":"Clone"} | 204 |
    Then checks that query "select * from contact where first_name='Noone was here'" has 1 row output
    And checks that query "select * from contact where first_name='Shaco was here'" has 1 row output
    And checks that query "select * from contact where first_name='Clone was here'" has 1 row output

    And invoke post request to webhook
      | integrations-conditional-flows-functional-test | test-webhook | {"message":"John"} | 204 |
    Then checks that query "select * from contact where first_name='Noone was here'" has 2 row output
    And checks that query "select * from contact where first_name='Shaco was here'" has 1 row output
    And checks that query "select * from contact where first_name='Clone was here'" has 1 row output

    When invoke post request to webhook
      | integrations-conditional-flows-functional-test | test-webhook | {"message":"Shaco"} | 204 |
    Then checks that query "select * from contact where first_name='Noone was here'" has 2 row output
    And checks that query "select * from contact where first_name='Shaco was here'" has 2 row output
    And checks that query "select * from contact where first_name='Clone was here'" has 1 row output

    When invoke post request to webhook
      | integrations-conditional-flows-functional-test | test-webhook | {"message":"Clone"} | 204 |
    Then checks that query "select * from contact where first_name='Noone was here'" has 2 row output
    And checks that query "select * from contact where first_name='Shaco was here'" has 2 row output
    And checks that query "select * from contact where first_name='Clone was here'" has 2 row output

  @integrations-conditional-flows-functional-test2
  Scenario: Conditional flows - functional test
    And select "Basic expression builder" integration action
    When Add another condition
    And fill in values by element data-testid
      | flowconditions-0-path  | message |
      | flowconditions-0-op    | equals  |
      | flowconditions-0-value | Shaco   |
    And fill in values by element data-testid
      | flowconditions-1-path  | message |
      | flowconditions-1-op    | equals  |
      | flowconditions-1-value | Clone   |
    And fill in values by element data-testid
      | usedefaultflow | true |

    And click on the "Next" button
    And click on the "Next" button

    When configure condition on position 1
    And add integration step on position "0"
    And select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    And fill in invoke query input with "insert into contact(first_name) values ('Shaco was here')" value
    And click on the "Next" button
    And return to primary flow from integration flow directly

    When configure condition on position 2
    And add integration step on position "0"
    And select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    And fill in invoke query input with "insert into contact(first_name) values ('Clone was here')" value
    And click on the "Next" button
    And return to primary flow from integration flow directly

    When configure condition on position 3
    And add integration step on position "0"
    And select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    And fill in invoke query input with "insert into contact(first_name) values ('Noone was here')" value
    And click on the "Next" button
    And return to primary flow from integration flow directly

    When click on the "Save" link
    And set integration name "integrations-conditional-flows-functional-test2"
    And click on the "Save and publish" button
    And navigate to the "Integrations" page
    And wait until integration "integrations-conditional-flows-functional-test2" gets into "Running" state

    When select the "integrations-conditional-flows-functional-test2" integration
    And invoke post request to webhook
      | integrations-conditional-flows-functional-test2 | test-webhook | {"message":"John"} | 204 |
    Then checks that query "select * from contact where first_name='Noone was here'" has 1 row output
    And checks that query "select * from contact where first_name='Shaco was here'" has 0 row output
    And checks that query "select * from contact where first_name='Clone was here'" has 0 row output

    When invoke post request to webhook
      | integrations-conditional-flows-functional-test2 | test-webhook | {"message":"Shaco"} | 204 |
    Then checks that query "select * from contact where first_name='Noone was here'" has 1 row output
    And checks that query "select * from contact where first_name='Shaco was here'" has 1 row output
    And checks that query "select * from contact where first_name='Clone was here'" has 0 row output

    When invoke post request to webhook
      | integrations-conditional-flows-functional-test2 | test-webhook | {"message":"Clone"} | 204 |
    Then checks that query "select * from contact where first_name='Noone was here'" has 1 row output
    And checks that query "select * from contact where first_name='Shaco was here'" has 1 row output
    And checks that query "select * from contact where first_name='Clone was here'" has 1 row output

    When invoke post request to webhook
      | integrations-conditional-flows-functional-test2 | test-webhook | {"message":"John"} | 204 |
    Then checks that query "select * from contact where first_name='Noone was here'" has 2 row output
    And checks that query "select * from contact where first_name='Shaco was here'" has 1 row output
    And checks that query "select * from contact where first_name='Clone was here'" has 1 row output

    When invoke post request to webhook
      | integrations-conditional-flows-functional-test2 | test-webhook | {"message":"Shaco"} | 204 |
    Then checks that query "select * from contact where first_name='Noone was here'" has 2 row output
    And checks that query "select * from contact where first_name='Shaco was here'" has 2 row output
    And checks that query "select * from contact where first_name='Clone was here'" has 1 row output

    When invoke post request to webhook
      | integrations-conditional-flows-functional-test2 | test-webhook | {"message":"Clone"} | 204 |
    Then checks that query "select * from contact where first_name='Noone was here'" has 2 row output
    And checks that query "select * from contact where first_name='Shaco was here'" has 2 row output
    And checks that query "select * from contact where first_name='Clone was here'" has 2 row output



  @integrations-conditional-flows-add-delete-step
  Scenario: Conditional flows - delete step
    And select "Advanced expression builder" integration action
    When Add another condition
    And fill in values by element data-testid
      | flowconditions-0-condition | ${body.message} == 'Shaco' |
      | flowconditions-1-condition | ${body.message} == 'Clone' |
    And fill in values by element data-testid
      | usedefaultflow | true |
    And click on the "Next" button
    And click on the "Next" button
    Then check that conditional flow step contains 3 flows
    And check that conditional flow default step is enabled

    When delete step on position 2
    Then check there are 2 integration steps

    When add integration step on position "0"
    And select the "Conditional Flows" connection
    And select "Advanced expression builder" integration action
    Then validate that condition count is equal to 1
    And validate condition content in condition flow step
      | 0 |  |

    When fill in values by element data-testid
      | flowconditions-0-condition | ${body.message} == 'Shaco' |
    And click on the "Next" button
    And click on the "Next" button
    Then check that conditional flow step contains 1 flows


  @integrations-conditional-flows-add-update-step
  Scenario: Conditional flows - edit and update step
    And select "Advanced expression builder" integration action
    When Add another condition
    And fill in values by element data-testid
      | flowconditions-0-condition | first  |
      | flowconditions-1-condition | second |
    And fill in values by element data-testid
      | usedefaultflow | true |
    And click on the "Next" button
    And click on the "Next" button
    Then check that conditional flow step contains 3 flows
    And check that conditional flow default step is enabled

    When edit integration step on position 2
    Then validate that condition count is equal to 2
    And validate condition content in condition flow step
      | 0 | first  |
      | 1 | second |

    When fill in values by element data-testid
      | usedefaultflow             | false   |
      | flowconditions-0-condition | changed |
    And validate condition content in condition flow step
      | 0 | changed |
      | 1 | second  |
    And click on the "Next" button
    And click on the "Next" button
    Then check that conditional flow step contains 2 flows

    When edit integration step on position 2
    Then validate that condition count is equal to 2
    And validate condition content in condition flow step
      | 0 | changed |
      | 1 | second  |


  @integrations-conditional-flows-multiple-flows
  Scenario: Conditional flows - multiple flows
    And select "Advanced expression builder" integration action
    When Add another condition
    And fill in values by element data-testid
      | flowconditions-0-condition | ${body.message} == 'Shaco' |
      | flowconditions-1-condition | ${body.message} == 'Clone' |
    And fill in values by element data-testid
      | usedefaultflow | true |
    And click on the "Next" button
    And click on the "Next" button

    When configure condition on position 1
    And add integration step on position "0"
    And select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    And fill in invoke query input with "insert into contact(first_name) values ('Shaco was here')" value
    And click on the "Next" button
    Then return to primary flow from integration flow from dropdown

    When configure condition on position 2
    And add integration step on position "0"
    And select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    And fill in invoke query input with "insert into contact(first_name) values ('Clone was here')" value
    And click on the "Next" button
    Then return to primary flow from integration flow from dropdown

    When configure condition on position 3
    And add integration step on position "0"
    And select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    And fill in invoke query input with "insert into contact(first_name) values ('Noone was here')" value
    And click on the "Next" button
    Then return to primary flow from integration flow from dropdown

    When add integration step on position "1"
    And select the "Conditional Flows" connection

    And select "Advanced expression builder" integration action
    When Add another condition
    And fill in values by element data-testid
      | flowconditions-0-condition | ${body.message} == 'Shaco'  |
      | flowconditions-1-condition | ${body.message} == 'Clone2' |
    And fill in values by element data-testid
      | usedefaultflow | true |
    And click on the "Next" button
    And click on the "Next" button

    When configure condition on position 5
    And add integration step on position "0"
    And select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    And fill in invoke query input with "insert into contact(first_name) values ('Shaco was here')" value
    And click on the "Next" button
    Then return to primary flow from integration flow from dropdown

    When configure condition on position 6
    And add integration step on position "0"
    And select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    And fill in invoke query input with "insert into contact(first_name) values ('Clone was here')" value
    And click on the "Next" button
    Then return to primary flow from integration flow from dropdown

    When configure condition on position 7
    And add integration step on position "0"
    And select the "PostgresDB" connection
    And select "Invoke SQL" integration action
    And fill in invoke query input with "insert into contact(first_name) values ('Noone was here')" value
    And click on the "Next" button
    Then return to primary flow from integration flow from dropdown

    When click on the "Save" link
    And set integration name "integrations-conditional-flows-add-delete-step"
    And click on the "Save and publish" button
    And navigate to the "Integrations" page
    Then wait until integration "integrations-conditional-flows-add-delete-step" gets into "Running" state

    When select the "integrations-conditional-flows-add-delete-step" integration
    And invoke post request to webhook
      | integrations-conditional-flows-add-delete-step | test-webhook | {"message":"John"} | 204 |
    Then checks that query "select * from contact where first_name='Noone was here'" has 2 row output
    And checks that query "select * from contact where first_name='Shaco was here'" has 0 row output
    And checks that query "select * from contact where first_name='Clone was here'" has 0 row output

    When invoke post request to webhook
      | integrations-conditional-flows-add-delete-step | test-webhook | {"message":"Shaco"} | 204 |
    Then checks that query "select * from contact where first_name='Noone was here'" has 2 row output
    And checks that query "select * from contact where first_name='Shaco was here'" has 2 row output
    And checks that query "select * from contact where first_name='Clone was here'" has 0 row output

    When invoke post request to webhook
      | integrations-conditional-flows-add-delete-step | test-webhook | {"message":"Clone"} | 204 |
    Then checks that query "select * from contact where first_name='Noone was here'" has 3 row output
    And checks that query "select * from contact where first_name='Shaco was here'" has 2 row output
    And checks that query "select * from contact where first_name='Clone was here'" has 1 row output

    When invoke post request to webhook
      | integrations-conditional-flows-add-delete-step | test-webhook | {"message":"Clone2"} | 204 |
    Then checks that query "select * from contact where first_name='Noone was here'" has 4 row output
    And checks that query "select * from contact where first_name='Shaco was here'" has 2 row output
    And checks that query "select * from contact where first_name='Clone was here'" has 2 row output

    And invoke post request to webhook
      | integrations-conditional-flows-add-delete-step | test-webhook | {"message":"John"} | 204 |
    Then checks that query "select * from contact where first_name='Noone was here'" has 6 row output
    And checks that query "select * from contact where first_name='Shaco was here'" has 2 row output
    And checks that query "select * from contact where first_name='Clone was here'" has 2 row output

    When invoke post request to webhook
      | integrations-conditional-flows-add-delete-step | test-webhook | {"message":"Shaco"} | 204 |
    Then checks that query "select * from contact where first_name='Noone was here'" has 6 row output
    And checks that query "select * from contact where first_name='Shaco was here'" has 4 row output
    And checks that query "select * from contact where first_name='Clone was here'" has 2 row output

    When invoke post request to webhook
      | integrations-conditional-flows-add-delete-step | test-webhook | {"message":"Clone"} | 204 |
    Then checks that query "select * from contact where first_name='Noone was here'" has 7 row output
    And checks that query "select * from contact where first_name='Shaco was here'" has 4 row output
    And checks that query "select * from contact where first_name='Clone was here'" has 3 row output

  @integrations-conditional-flows-dropdown-test
  Scenario: Conditional flows - dropdown test
    And select "Advanced expression builder" integration action
    When Add another condition
    And fill in values by element data-testid
      | flowconditions-0-condition | ${body.message} == 'Shaco' |
      | flowconditions-1-condition | ${body.message} == 'Clone' |
    And fill in values by element data-testid
      | usedefaultflow | true |
    And click on the "Next" button
    And click on the "Next" button

    When configure condition on position 1
    Then validate conditional flow dropdown content
      | Conditional WHEN ${body.message} == 'Shaco' |
      | Conditional WHEN ${body.message} == 'Clone' |
      | Default OTHERWISE Use this as default       |

    When return to primary flow from integration flow from dropdown
    And add integration step on position "1"
    And select the "Conditional Flows" connection
    And select "Advanced expression builder" integration action
    When Add another condition
    And fill in values by element data-testid
      | flowconditions-0-condition | ${body.message} == 'Shaco2' |
      | flowconditions-1-condition | ${body.message} == 'Clone2' |
    And fill in values by element data-testid
      | usedefaultflow | true |
    And click on the "Next" button
    And click on the "Next" button

    When configure condition on position 5
    Then validate conditional flow dropdown content
      | Conditional WHEN ${body.message} == 'Shaco'  |
      | Conditional WHEN ${body.message} == 'Clone'  |
      | Default OTHERWISE Use this as default        |
      | Conditional WHEN ${body.message} == 'Shaco2' |
      | Conditional WHEN ${body.message} == 'Clone2' |
      | Default OTHERWISE Use this as default        |

  @integrations-conditional-flows-output-type
  Scenario: Conditional flows - output type
    And select "Advanced expression builder" integration action
    And fill in values by element data-testid
      | flowconditions-0-condition | ${body.message} == 'Shaco' |
      | flowconditions-1-condition | ${body.message} == 'Clone' |
    And fill in values by element data-testid
      | usedefaultflow | false |
    And click on the "Next" button
    And force fill in values by element data-testid
      | describe-data-shape-form-kind-input | JSON Instance |
    And fill text into text-editor
      | {"greeting":"Hi"} |
    And click on the "Next" button

    When configure condition on position 1
    And add a data mapping step - open datamapper
    And create data mapper mappings
      | message | greeting |
    And click on the "Done" button
    Then check that there is no warning inside of step number "2"

    When return to primary flow from integration flow from dropdown

    When add a default flow through warning link - open flows configuration
    And fill in values by element data-testid
      | usedefaultflow | true |
    And click on the "Next" button
    And click on the "Next" button
    Then check that there is no warning inside of step number "2"



