# @sustainer: tplevko@redhat.com

@rest
@twitter
@salesforce
@datamapper
Feature: Integration - Twitter

  @integration-tw-sf
  Scenario: Mention to Salesforce create object
    Given clean application state
    Given clean SF contacts related to TW account: "twitter_talky"
    And clean all tweets in twitter_talky account
    And create Twitter connection using "Twitter Listener" account
    And create SalesForce connection
    And create TW mention step with "twitter-mention-action" action
    And create basic TW to SF filter step

    And start mapper definition with name: "mapping 1"
    Then SEPARATE using Step 1 and strategy "Space" and source "//user/name" into targets
        | /FirstName | /LastName |
    Then MAP using Step 1 and field "//user/screenName" to "/TwitterScreenName__c"
    Then MAP using Step 1 and field "//text" to "/Description"

    And create SF "salesforce-create-sobject" action step on field: "Contact"
    When create integration with name: "Twitter to salesforce contact rest test"
    Then wait for integration with name: "Twitter to salesforce contact rest test" to become active
    Then check SF does not contain contact for tw account: "twitter_talky"
    Then tweet a message from twitter_talky to "Twitter Listener" with text "#backendTest Have you heard about Syndesis project? It is pretty amazing..."
    Then validate contact for TW account: "twitter_talky" is present in SF with description: "#backendTest Have you heard about Syndesis project? It is pretty amazing..."
    Given clean application state
    And clean SF contacts related to TW account: "twitter_talky"
    And clean all tweets in twitter_talky account
