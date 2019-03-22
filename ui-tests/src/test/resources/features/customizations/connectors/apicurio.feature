# @sustainer: mcada@redhat.com

@ui
@apicurio
@apicurio-customization
Feature: Customization - API Connector - ApicurIO GUI

  Background:
    Given log into the Syndesis
    And clean application state

    When navigate to the "Customizations" page
    Then check visibility of page "Customizations"

    When click on the "API Client Connectors" link
    Then check visibility of page "API Client Connectors"

    When open new Api Connector wizard
    And check visibility of page "Upload Swagger Specification"
    And upload swagger file src/test/resources/swagger/connectors/invalid/kie-server-swagger.json
    And navigate to the next Api Connector wizard step "Review Actions"
    Then check visibility of page "Review Actions"
    And check that apicurio imported operations number is loaded
    And check that apicurio shows 218 imported operations
    And check that apicurio shows 58 warnings

    When click on the "Review/Edit" button

  @apicurio-check-warnings-change @gh-3459
  Scenario: Check if warnings change is propagated into connector review page from ApicurIO GUI
    When remove warning via apicurio gui
    And click on button "Save" while in apicurio studio page
    Then check visibility of page "Review Actions"
    And check that apicurio imported operations number is loaded
    And check that apicurio shows 218 imported operations
    And check that apicurio shows 57 warnings

  @apicurio-check-operations-change-add
  Scenario: Check if operations change is propagated into connector review page from ApicurIO GUI
    When add an operation via apicurio gui
    And click on button "Save" while in apicurio studio page
    And check visibility of page "Review Actions"
    And check that apicurio imported operations number is loaded
    Then check that apicurio shows 219 imported operations

  @gh-4910
  @apicurio-check-operations-change-add-with-error
  Scenario: Check if operations change is propagated into connector review page from ApicurIO GUI
    When add an operation with error via apicurio gui
    And click on button "Save" while in apicurio studio page
    And check visibility of page "Review Actions"
    And check that apicurio imported operations number is loaded
    Then check that apicurio shows 219 imported operations
    And check that apicurio shows 1 errors

  @apicurio-check-operations-change-remove
  Scenario: Check if removing an operations is reflected in connector review page from ApicurIO GUI
    When remove an operation via apicurio gui
    And click on button "Save" while in apicurio studio page
    And check visibility of page "Review Actions"
    And check that apicurio imported operations number is loaded
    Then check that apicurio shows 217 imported operations

  @apicurio-check-operations-change-but-no-save
  Scenario: Check if operations change is correctly not propagated into connector when changes from ApicurIO GUI are not saved
    When add an operation via apicurio gui
    And click on button "Cancel" while in apicurio studio page
    And click on the modal dialog "Yes" button
    Then check visibility of page "Review Actions"
    And check that apicurio imported operations number is loaded
    And check that apicurio shows 218 imported operations

  @apicurio-check-security-settings-no-security
  Scenario: Check apicurio security settings - no security
    When click on the "Cancel" button
    And click on the modal dialog "Yes" button
    And click on the "Next" button
    Then check that api connector authentication section contains text "No Security"

    When click on the "Next" button
    And click on the "Create API Connector" button
    And navigate to the "Connections" page
    And click on the "Create Connection" button
    And select "Kie Server API" connection type
    Then check that connection authentication type has 1 options and contains text "No Security"

  @apicurio-check-security-settings-basic
  Scenario: Check apicurio security settings - basic security
    When add security schema BASIC via apicurio gui
    And click on button "Save" while in apicurio studio page
    Then check visibility of page "Review Actions"
    And check that apicurio imported operations number is loaded
    And check that apicurio shows 218 imported operations

    When click on the "Next" button
    Then check that api connector authentication section contains text "HTTP Basic Authentication"

    When click on the "Next" button
    And click on the "Create API Connector" button
    And navigate to the "Connections" page
    And click on the "Create Connection" button
    And select "Kie Server API" connection type
    Then check that connection authentication type has 1 option and contains text "HTTP Basic Authentication"

  #this feature is not supported yet and does not work, but test is ready :)
  @disabled
  @apicurio-check-security-settings-api-key
  Scenario: Check apicurio security settings - API key
    When add security schema API Key via apicurio gui
    And click on button "Save" while in apicurio studio page
    Then check visibility of page "Review Actions"
    And check that apicurio imported operations number is loaded
    And check that apicurio shows 218 imported operations

    When click on the "Next" button
    Then check that api connector authentication section contains text "API Key"

    When click on the "Next" button
    And click on the "Create API Connector" button
    And navigate to the "Connections" page
    And click on the "Create Connection" button
    And select "Kie Server API" connection type
    Then check that connection authentication type has 1 option and contains text "API Key Authentication"

  @apicurio-check-security-settings-oauth-2
  Scenario: Check apicurio security settings - OAuth 2
    When add security schema OAuth 2 via apicurio gui
    And click on button "Save" while in apicurio studio page
    Then check visibility of page "Review Actions"
    # we need to give time to UI to fetch the changes
    And check that apicurio imported operations number is loaded
    And check that apicurio shows 218 imported operations

    When click on the "Next" button
    Then check that api connector authentication section contains text "OAuth 2.0"
    And fill in values
      | Access Token URL  | https://hehe                            |
      | Authorization URL | http://petstore.swagger.io/oauth/dialog |

    When click on the "Next" button
    And click on the "Create API Connector" button
    And navigate to the "Connections" page
    And click on the "Create Connection" button
    And select "Kie Server API" connection type
    Then check that connection authentication type has 1 option and contains text "OAuth 2.0"
