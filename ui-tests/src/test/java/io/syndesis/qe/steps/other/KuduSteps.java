package io.syndesis.qe.steps.other;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;

import cucumber.api.java.en.When;
import io.syndesis.qe.accounts.Account;
import io.syndesis.qe.accounts.AccountsDirectory;
import io.syndesis.qe.utils.HttpUtils;
import io.syndesis.qe.utils.OpenShiftUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KuduSteps {

    @When("^create table in Kudu server$")
    public void createTable() {
        String response = HttpUtils.doGetRequest("http://" + OpenShiftUtils.getInstance().getRoutes().stream()
                .filter(route -> "kudu-rest-api".equals(route.getMetadata().getName()))
                .findFirst().get().getSpec().getHost() + "/kudu/table/create").getBody();

        if (!(response.contains("Table created") || response.contains("Table my-table already exists"))) {
            fail("Table creation failed: " + response);
        }
    }

    @When("^delete table from Kudu server$")
    public void deleteTable() {
        String response = HttpUtils.doGetRequest("http://" + OpenShiftUtils.getInstance().getRoutes().stream()
                .filter(route -> "kudu-rest-api".equals(route.getMetadata().getName()))
                .findFirst().get().getSpec().getHost() + "/kudu/table/delete").getBody();
        log.debug("Table deletion log: " + response);
    }

    @When("^insert a row into Kudu server table$")
    public void insertIntoTable() {
        String response = HttpUtils.doGetRequest("http://" + OpenShiftUtils.getInstance().getRoutes().stream()
                .filter(route -> "kudu-rest-api".equals(route.getMetadata().getName()))
                .findFirst().get().getSpec().getHost() + "/kudu/table/insert").getBody();

        assertThat(response).isEqualToIgnoringCase("Insert successful");
    }

    @When("^check that Kudu server table contains inserted data$")
    public void validateKuduTableData() {
        String response = HttpUtils.doGetRequest("http://" + OpenShiftUtils.getInstance().getRoutes().stream()
                .filter(route -> "kudu-rest-api".equals(route.getMetadata().getName()))
                .findFirst().get().getSpec().getHost() + "/kudu/table/validate").getBody();

        assertThat(response).isEqualToIgnoringCase("Success");
    }

    @When("^set Kudu credentials$")
    public void createCredentials() {
        Account kudu = new Account();
        kudu.setService("Apache Kudu");
        Map<String, String> accountParameters = new HashMap<>();
        accountParameters.put("host", "syndesis-kudu");
        kudu.setProperties(accountParameters);
        AccountsDirectory.getInstance().addAccount("kudu", kudu);
    }
}
