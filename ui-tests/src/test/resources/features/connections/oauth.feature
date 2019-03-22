# @sustainer: mcada@redhat.com

@ui
@oauth
Feature: Connections - OAuth

  Background: Clean application state
    Given clean application state
    And log into the Syndesis

#
#  1. Create connectors which allow OAuth
#
  @oauth-validate-connectors
  Scenario: Create integration using connections with OAuth
    When navigate to the "Settings" page
    Then check that settings item "Salesforce" has button "Register"
    When fill all oauth settings
    Then create connections using oauth
#    Until the issue with @concur support is resolved, concur testing will be disabled.
#      | SAP Concur      | Test-Concur-connection          |
      | Gmail           | Test-Gmail-connection           |
      | Salesforce      | Test-Salesforce-connection      |
      | Google Sheets   | Test-Google-Sheets-connection   |
      | Google Calendar | Test-Google-Calendar-connection |
      | Twitter         | Test-Twitter-connection         |



#TODO: Necessary OAuth scenarios: Gmail, Salesforce, Twitter, Google Calendar
#TODO: because mentioned connectors do not use OAuth in their tests

