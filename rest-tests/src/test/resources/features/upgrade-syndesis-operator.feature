# @sustainer: avano@redhat.com

@syndesis-upgrade
@syndesis-upgrade-operator
@gh-4781
Feature: Syndesis Upgrade Using Operator

  Background:
    Given clean default namespace
      And get upgrade versions
      And clean upgrade modifications
      And deploy Syndesis
      And wait for Syndesis to become ready
      And verify syndesis "given" version
    When inserts into "contact" table
      | X | Y | Z | db |
      And create start DB periodic sql invocation action step with query "SELECT * FROM CONTACT" and period "5000" ms
      And add a split step
      And create basic filter step for "last_name" with word "Y" and operation "contains"
      And start mapper definition with name: "mapping 1"
      And MAP using Step 2 and field "/first_name" to "/task"
      And create finish DB invoke sql action step with query "INSERT INTO TODO (task, completed) VALUES (:#task, 0)"
    Then create integration with name: "upgrade"
      And wait for integration with name: "upgrade" to become active
      And verify integration with task "X"

  Scenario: Syndesis Upgrade Using Operator
    When perform syndesis upgrade to newer version using operator
    Then wait until upgrade pod is finished
      And wait for Syndesis to become ready
      And verify syndesis "upgraded" version
      And verify integration with task "X"
