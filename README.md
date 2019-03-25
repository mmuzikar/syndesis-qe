# Syndesis QE
Updated: 31.01.2019

##### Table of Contents

* [Structure](#structure)
* [Prepare minishift instance](#prepare-minishift-instance)
* [Scenarios](#scenarios)
* [Configuration](#configuration)
* [Execution](#execution)
  * [Most common problems](#most-common-problems)
* [Running on remote OpenShift instance](#running-on-remote-openshift-instance)
* [Contributing](#contributing)
  * [Checkstyle](#checkstyle)
  * [Creating a Pull Request](#creating-a-pull-request)
* [Advanced debugging](#advanced-debugging)
* [List of scenarios](#list-of-scenarios)

### Structure

```bash
├── docs
├── rest-tests
├── ui-tests
└── utilities
```

#### docs
On-going initiative to provide comprehensive guide to current code structure.
Contains list of all used steps in this project in [index.md](docs/index.md)

#### rest-tests
Java based tests that use Cucumber scenarios.
Test actions are executed directly to `syndesis-server` backend.

#### ui-tests
Java based tests that use Selenide and Cucumber BDD scenarios.
Test actions are mainly UI driven with additional 3rd party validation like Salesforce, Twitter etc.

### Prepare minishift instance

#### Prerequisites:
- Installed [Minishift](https://www.openshift.org/minishift/)

- Cloned [Syndesis](https://github.com/syndesisio/syndesis)

- Cloned this repo with [syndesis-extension](https://github.com/syndesisio/syndesis) submodule (*git clone --recurse-submodules*)

- Correctly set test.properties and credentials.json (described later)

- Added Fuse repositories to maven due to dependencies

Before you import maven project to the IDE, you have to install 
**Lombok** and 
**Cucumber** plugins to your IDE. 

For IntelliJ Idea you can use these plugins:

- [Lombok](https://plugins.jetbrains.com/plugin/6317-lombok-plugin)
- [Cucumber](https://plugins.jetbrains.com/plugin/7212-cucumber-for-java)
 
For more information ask `mcada@redhat.com` or `avano@redhat.com` or `tplevko@redhat.com`

#### Create a minishift instance
For minishift version 23+
    
    minishift start 
    
For version 22 and below 

    minishift start --memory 4912 --cpus 2 --disk-size 20GB --openshift-version v3.9.0

#### Add minishift ip to the hosts
Following lines in /etc/hosts file, insert your minishift ip:

	${minishift_ip} syndesis.my-minishift.syndesis.io
	${minishift_ip} todo-syndesis.my-minishift.syndesis.io

Due to --route option when installing syndesis and updated /etc/hosts file we don't
have to update all third party applications and their callbacks for every minishift/openshift IP.

#### Add admin rights to developer user
    oc adm policy --as system:admin add-cluster-role-to-user cluster-admin developer

### Scenarios
Test scenarios are provided in Gherkin language in a BDD fashion. Located in `./resources`
directory of `*-tests` module, e.g. [UI scenarios](https://github.com/syndesisio/syndesis-qe/tree/master/ui-tests/src/test/resources/features).

Every scenario is wrapped with appropriate tags to target specific execution on demand.

If you want to execute tests for specific component - both UI and REST, you can use components tags, for example `@salesforce`, otherwise you can specify multiple tags using cucumber notation like `'@datamapper and @rest'` .
The list of all scenarios(tags) is in the [List of scenarios](#list-of-scenarios) chapter.


#### GitHub issue tags

A scenario may optionally be tagged with one or more tags in the form `@gh-<issue-number>`. 
If such a scenario fails, the relevant GitHub issues are checked and a summary is written in the cucumber report.
See javadoc of the OnFailHooks class for more info.

##### Skipping tests with open issues
In some cases you might want to skip tests which you know are going to fail, because they are tagged with open issues.
You can do that by setting the property `syndesis.skip.open.issues` to `true` (defaults to `false`).

### Configuration
NOTE: Successful execution of tests requires fully configured credentials.
All the callback URLs, Oauth tokens, etc. for Salesforce and Twitter accounts.

#### Example of test.properties to run on minishift
File `test.properties` should be located in the root of syndesis-qe folder.
Working example can be found in jenkins nightly build run logs.

For more information ask `mcada@redhat.com` or `avano@redhat.com` or `tplevko@redhat.com`

test.properties (update *syndesis.config.openshift.url* property according to your minishift ip)
```
syndesis.config.openshift.url=https://192.168.64.2:8443
syndesis.config.openshift.namespace=syndesis
syndesis.config.openshift.route.suffix=my-minishift.syndesis.io
# namespace for the subject access review (if not specified, will use the same namespace as for the deployment)
syndesis.config.openshift.sar_namespace=syndesis

syndesis.dballocator.url=<dballocator url> 

#timeout in seconds, if not set default is 300
syndesis.config.timeout=300

#delay in seconds, if not set default is 1
jenkins.delay=7

syndesis.config.ui.url=https://syndesis.my-minishift.syndesis.io
syndesis.config.ui.username=developer
syndesis.config.ui.password=developer
syndesis.config.ui.browser=firefox
```

#### Example of credentials.json
File `credentials.json` should be located in root of syndesis-qe folder.
Working example on demand.

For more information ask `mcada@redhat.com` or `avano@redhat.com` or `tplevko@redhat.com`

credentials.json
```json
{
  "twitter_listener": {
    "service": "twitter",
    "properties": {
      "screenName": "****",
      "accessToken": "****",
      "accessTokenSecret": "****",
      "consumerKey": "****",
      "consumerSecret": "*****",
      "login": "****",
      "password": "****"
    }
  },
  "twitter_talky": {
    "service": "twitter",
    "properties": {
      "screenName": "****",
      "consumerKey": "****",
      "consumerSecret": "****",
      "accessToken": "****",
      "accessTokenSecret": "****",
      "login": "****",
      "password": "****"
    }
  },
  "salesforce": {
    "service": "salesforce",
    "properties": {
      "instanceUrl": "https://developer.salesforce.com",
      "loginUrl": "https://login.salesforce.com",
      "clientId": "****",
      "clientSecret": "****",
      "userName": "****",
      "password": "****"
    }
  },
  "QE Salesforce": {
    "service": "salesforce",
    "properties": {
      "instanceUrl": "https://developer.salesforce.com",
      "loginUrl": "https://login.salesforce.com",
      "clientId": "****",
      "clientSecret": "****",
      "userName": "****",
      "password": "****"
    }
  },
  "Twitter Listener": {
    "service": "twitter",
    "properties": {
      "screenName": "****",
      "accessToken": "****",
      "accessTokenSecret": "****",
      "consumerKey": "****",
      "consumerSecret": "****"
    }
  },
  "s3": {
    "service": "s3",
    "properties": {
      "region": "****",
      "accessKey": "****",
      "secretKey": "****"
    }
  },
  "syndesis": {
    "service": "syndesis",
    "properties": {
      "instanceUrl": "****",
      "login": "****",
      "password": "****"
    }
  },
  "QE Dropbox": {
    "service": "dropbox",
    "properties": {
      "accessToken": "****",
      "clientIdentifier": "****"
    }
  },
  "ftp": {
    "service": "ftp",
    "properties": {
      "binary": "Yes",
      "connectTimeout": "10000",
      "disconnect": "No",
      "host": "ftpd",
      "maximumReconnectAttempts": "3",
      "passiveMode": "Yes",
      "password": "****",
      "port": "****",
      "reconnectDelay": "1000",
      "timeout": "30000",
      "username": "****"
    }
  },
  "QE Slack": {
    "service": "slack",
    "properties": {
      "webhookUrl": "****",
      "Token": "****"
    }
  },
  "QE Google Mail": {
    "service": "Google Mail",
    "properties": {
      "clientId": "****",
      "clientSecret": "****",
      "applicationName": "****",
      "refreshToken": "****",
      "userId": "****",
      "email": "****",
      "password": "****"
    }
  },
  "telegram": {
    "service": "telegram",
    "properties": {
      "authorizationToken": "****"
    }
  },
  "GitHub": {
    "service": "GitHub",
    "properties": {
      "PersonalAccessToken": "****"
    }
  }
}
```
for ftp connection credentials:
All values are just examples / proposals. Need to be updated in accordance with 

### Execution

#### Before execution
For the test execution at least `io.syndesis.common:common-model` and `io.syndesis.server:server-endpoint` modules are required in current version.

```
cd <syndesis-project-dir>
./syndesis/tools/bin/syndesis build --init --batch-mode --backend --flash
```

Working with extensions requires syndesis-extensions submodule compiled. 
**NOTE** If you didn't clone syndesis-qe repository with *--recurse-submodules* option as mentioned before, you have run following commands:
```
cd <syndesis-qe-project-dir>
git submodule update --init --recursive
cd syndesis-extensions
mvn clean install
```

#### Test suite execution

There're three Maven profiles: `all, rest, ui` to target the specific test suite.

```
mvn clean test // default all profile
mvn clean test -P ui
mvn clean test -P rest
```

#### Particular test execution

When you want to run only the particular tests or scenarios, just use their **tags**. The following example runs @integration-ftp-ftp scenario from the rest suite

```
mvn clean test -P rest -Dcucumber.options="--tags @integration-ftp-ftp"
```

#### Additional parameters

```
mvn clean test -P rest -Dcucumber.options="--tags @integration-ftp-ftp" \
        -Dsyndesis.config.openshift.namespace.lock=true \
        -Dsyndesis.config.openshift.namespace.cleanup=true \
        -Dsyndesis.config.openshift.namespace.cleanup.after=false
```
You can use various parameters. From the previous command, parameter:
* *syndesis.config.openshift.namespace.lock* - lock the namespace
* *syndesis.config.openshift.namespace.cleanup* - cleanup namespace before the tests
* *syndesis.config.openshift.namespace.cleanup.after* - cleanup namespace after the tests (it can be useful to set this to false during debugging phase)

You can use profile `-P deploy` that sets all 3 parameters to lock the namespace, clean the namespace and don't clean the namespace after tests.

To select syndesis version, add another maven parameter:

	-Dsyndesis.config.template.version=<version>

To install syndesis from operator template, add maven parameter:

	-Dsyndesis.config.operator.url=<url-to-operator.yml>


##### Using maven central proxy
Maven central is used when building integrations in minishift. That can occasionally fail, because a request limit is reached.
In such a case (and perhaps in any case), you can add the following test property to use your own maven central proxy:
```
syndesis.config.upstream.repository=<your maven central proxy>
```


##### Most common problems
* If you set *syndesis.config.openshift.namespace.lock* parameter to true and you stop tests during running, the lock will not be released! 
It causes that the next tests stuck for the 60 minutes on ***Waiting to obtain namespace lock***. If you don't want to
wait, open terminal and just delete *test-lock* secret from *syndesis* project. At the moment, tests should continue.

```
oc login -u developer -p developer
oc project syndesis
oc delete secret test-lock
```

*  If tests failed at ***java.lang.IllegalArgumentException: bound must be positive***, 
just add -Dsyndesis.config.openshift.namespace.lock=true parameter to the command.

#### Debugging

When you want to debug code, just add following command
```
"-Dmaven.surefire.debug=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 -Xnoagent -Djava.compiler=NONE"
```
to the maven run command.

E.g. for debugging slack tests:
```
mvn "-Dmaven.surefire.debug=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 -Xnoagent -Djava.compiler=NONE" \
        clean test -P ui "-Dcucumber.options=--tags @slack" \
        -Dsyndesis.config.openshift.namespace.lock=false \
        -Dsyndesis.config.openshift.namespace.cleanup=true \
        -Dsyndesis.config.openshift.namespace.cleanup.after=false
```

After that, the project will be waiting for a connection. After that, you can connect to remote debug in IDE. For more information [look here](http://jtuts.com/2016/07/29/how-to-set-up-remote-debugging-in-intellij-idea-for-a-webapp-that-is-run-by-tomcat-maven-plugin/).

### Running on remote OpenShift instance
In some cases, you want to run the tests on the remote OpenShift instance instead of the local Minishift instance.

Lets say, you have a OpenShift instance on *https://myFancyOpenshiftInstance.mydomain.com* and you have user 
**remoteuser** with password **RemoteUserPassword**.

If you want to run the tests on the existing remote OpenShift instance, follow this steps.

* Connect to OpenShift instance. E.g.
  ```
  oc login https://myFancyOpenshiftInstance.mydomain.com:8443 -u remoteuser -p RemoteUserPassword
  ```
* First, create a new namespace for testing. E.g. **testingnamespace**
  ```
  oc new-project testingnamespace
  ```
* After that, update *test.properties* according to the remote instance. E.g.
    ```
    syndesis.config.openshift.url=https://myFancyOpenshiftInstance.mydomain.com:8443
    syndesis.config.openshift.namespace=testingnamespace
    syndesis.config.openshift.route.suffix=my-minishift.syndesis.io
    # namespace for the subject access review (if not specified, will use the same namespace as for the deployment)
    syndesis.config.openshift.sar_namespace=testingnamespace

    syndesis.dballocator.url=http://dballocator.mw.lab.eng.bos.redhat.com:8080
    
    #timeout in seconds, if not set default is 30
    syndesis.config.timeout=15
    
    #delay in seconds, if not set default is 1
    jenkins.delay=7
    
    syndesis.config.ui.url=https://testingnamespace.my-minishift.syndesis.io
    syndesis.config.ui.username=remoteuser
    syndesis.config.ui.password=RemoteUserPassword
    syndesis.config.ui.browser=firefox
    ```
* You have to also update */etc/hosts* according to the remote instance. E.g.
    ```
	${remote_openshift_ip} testingnamespace.my-minishift.syndesis.io
	${remote_openshift_ip} todo-testingnamespace.my-minishift.syndesis.io
	```
After that, you can run the tests on the remote OpenShift instances. 
Remember to delete project after testing.
### Contributing

#### Checkstyle
Before you start contributing, please set up checkstyle in your IDE.
For IntelliJ Idea, you have to install **[CheckStyle-IDEA](https://plugins.jetbrains.com/plugin/1065-checkstyle-idea)* plugin.
After that, open Settings > Editor > CodeStyle and click on cogwheel, select Import Scheme > CheckStyle configuration and
choose checkstyle.xml file from this repository.

**NOTE** IntelliJ Idea doesn't provide auto-format code after saving (because it provides auto-saving) by default. When you want to
reformat your code, you can use shortcut **CTRL+ALT+L** or you can install and configure **[Save Actions](https://plugins.jetbrains.com/plugin/7642-save-actions)**

You can also use *Reformat Code* checkbox in *Commit Changes*
dialog before you commit changes.  

**Commit changes** dialog provides a useful view of your changes before commit. There, you can see the diff between 
original and changed file. So you can make sure that you commit only changes which you want. Also, you can set commit message 
and commit changes via this dialog. 

![Commit changes](docs/readme_images/idea_commit_changes.png)

#### Creating a Pull Request
When you create a PR on GitHub a new Jenkins job is scheduled. This job runs on Fuse QE Jenkins instance and runs a basic subset of tests (annotated with @smoke tag).

If you want to run a different subset of tests, you can use **//test: \`@mytag1 or @mytag2\`** in your PR description to trigger specific tests annotated by given tags.

If you don't want to run the job for your PR at all (for example when you are changing small things in README file), you can use `//skip-ci` in your PR description.

When the PR job fails because of test failures and you believe that you didn't cause the error, you can try to trigger the job once again. For this just comment `retest this please` in the PR and a new build will be triggered in few minutes.

Please remember that each time you push something new (or amend something old) in the PR, a new build is triggered automatically, so you don't need to do anything else to get your PR rebuilt.

### Advanced debugging

#### HotSwap
HotSwap is a very useful technique on how to change the code during debugging. It saves a lot of times. 
Especially in UI testing. You can stop debugger to the point where the test fails due to 
test mistakes (e.g. label was changed), look into the debbuger for the correct value, drop frame and run method again 
without restart all tests.

The following steps show how to debug and use it in the IntelliJ Idea.

* In the UI project, right-click on the CucumberTest class and click Run. It fails, but it created JUnit configuration
* Edit JUnit configuration and add parameters to the VM Options.
 *cucumber.options* and *syndesis.version* are mandatory!
    e.g.
    ```
    -ea
    "-Dcucumber.options=--tags @slack-to-db"
    -Dsyndesis.config.openshift.namespace.lock=true
    -Dsyndesis.config.openshift.namespace.cleanup=true
    -Dsyndesis.config.openshift.namespace.cleanup.after=false
    -Dsyndesis.version=master
    ```
    As you can see, the running scenario is specified in the cucumber.options tag.
* Add breakpoint where you want and run debug (Shift-F9).
* When you change something, you have to recompile the particular class. *(Build -> Recompile Ctrl+Shift+F9)*
* After that, you have to drop frame. In the Debugger view, right-click on the top frame and select *Drop Frame*. 
    It causes that last frame (e.g. function) will be running again with the changed code.
    
[Video with example](https://drive.google.com/file/d/16G-UDrRGLE-YvuRJUMlVpGi1z5vkjc4r/view?usp=sharing)

* In some cases, after step to the next line, the code throws exception and you lose the frame stack. When you want to 
ignore exception, in the debugger view click on *View Breakpoints* (Ctrl+Shift+F8) and select that you want to stop program 
on Java Exception Breakpoints and specify particular exception. After that, program stop before
throwing an exception and you just drop frames which are on the frame with your method.

* In some cases, you want to try find UI label with **Trial and Error** method. 
It means that you are changing the name unless you find the correct one. But, of course, you don't want to 
throw exception every time when you set incorrect name. For this, just stop debugger in code, open debugger 
and in the variables view you can add and edit any variables you want. 
When variables throws exception, it will not affect tests (main) executions.

[Video with example of *Exception Breakpoints* and *Trial and Error*](https://drive.google.com/open?id=1baPJx7YncTn36B6N-VFy85lZciskC1NE)


For more information see [Altering the program's execution flow](https://www.jetbrains.com/help/idea/altering-the-program-s-execution-flow.html#reload_classes)

### List of scenarios

```
------MAIN CATEGORIES------
@doc-tutorial (documentation tutorials tests)
@quickstart (tests for quickstarts: https://github.com/syndesisio/syndesis-quickstarts)
@reproducer (tests which are related to the particular issue)
@rest
@ui
------TECHNOLOGIES------
@activemq
@amqp
@api-provider
@apicurio
@concur
@dropbox
@ftp
@gmail
@google-calendar
@http
@kafka
@mqtt
@mysql
@oracle12
@s3
@salesforce
@servicenow
@slack
@twitter
@webhook
------FUNCTIONS------
@3scale
@activity
@amqbroker
@api-connector
@database
@datamapper
@export
@extension
@import
@log
@metrics
@swagger
@timer
@todo-app
------QUICKSTARTS------
@quickstart-solution (tests for importing and testing the quickstart's solutions)
@quickstart-video (tests for testing process which is described in the quckstart's videos)
```
