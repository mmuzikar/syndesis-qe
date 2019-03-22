package io.syndesis.qe.bdd.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.apache.commons.lang.RandomStringUtils;

import org.assertj.core.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.cucumber.datatable.DataTable;
import io.syndesis.qe.TestConfiguration;
import io.syndesis.qe.endpoints.TestSupport;
import io.syndesis.qe.utils.DbUtils;
import io.syndesis.qe.utils.SampleDbConnectionManager;
import io.syndesis.qe.utils.TestUtils;
import io.syndesis.qe.utils.dballoc.DBAllocatorClient;
import lombok.extern.slf4j.Slf4j;

/**
 * DB related validation steps.
 * <p>
 * Jan 17, 2018 Red Hat
 *
 * @author tplevko@redhat.com
 */
@Slf4j
public class DbValidationSteps {
    private DbUtils dbUtils;

    @Autowired
    private DBAllocatorClient dbAllocatorClient;

    public DbValidationSteps() {
        dbUtils = new DbUtils("postgresql");
    }

    @Given("^remove all records from table \"([^\"]*)\"")
    public void cleanupDb(String tableName) {
        TestSupport.getInstance().resetDB();
        dbUtils.deleteRecordsInTable(tableName);
    }

    @Then("^validate DB created new lead with first name: \"([^\"]*)\", last name: \"([^\"]*)\", email: \"([^\"]*)\"")
    public void validateSfDbIntegration(String firstName, String lastName, String emailAddress) throws InterruptedException {
        Thread.sleep(5000);
        final long start = System.currentTimeMillis();
        // We wait for exactly 1 record to appear in DB.
        final boolean contactCreated = TestUtils.waitForEvent(leadCount -> leadCount == 1, () -> dbUtils.getNumberOfRecordsInTable("todo"),
                TimeUnit.MINUTES,
                2,
                TimeUnit.SECONDS,
                5);
        assertThat(contactCreated).as("Lead record has appeard in db 1").isEqualTo(true);
        log.info("Lead record appeared in DB. It took {}s to create contact.", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start));
        // Now we verify, the created lead contains the correct personal information.
        assertThat(getLeadTaskFromDb(firstName + " " + lastName).toLowerCase()).contains(emailAddress);
    }

    @Then("^validate SF on delete to DB created new task.*$")
    public void validateLead() {
        final long start = System.currentTimeMillis();
        // We wait for exactly 1 record to appear in DB.
        final boolean contactCreated = TestUtils.waitForEvent(leadCount -> leadCount == 1, () -> dbUtils.getNumberOfRecordsInTable("todo"),
                TimeUnit.MINUTES,
                2,
                TimeUnit.SECONDS,
                5);
        assertThat(contactCreated).as("Lead record has appeard in db 2").isEqualTo(true);
        log.info("Lead record appeared in DB. It took {}s to create contact.", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start));
        // Now we verify, the created lead contains the correct personal information.
        assertThat(getLeadTaskFromDb().toLowerCase()).isNotEmpty();
    }

    @Then("^validate add_lead procedure with last_name: \"([^\"]*)\", company: \"([^\"]*)\", period in ms: \"(\\w+)\"")
    public void validateAddLeadProcedure(String lastName, String company, Integer ms) throws InterruptedException {
        //wait for period cycle:
        Thread.sleep(ms + 1000);
        // We wait for at least 1 record to appear in DB (procedure goes on every 5 seconds).
        final boolean contactCreated = TestUtils.waitForEvent(leadCount -> leadCount >= 1, () -> dbUtils.getNumberOfRecordsInTable("todo"),
                TimeUnit.MINUTES,
                2,
                TimeUnit.SECONDS,
                5);
        assertThat(contactCreated).as("Lead record has appeared in DB, TODO table").isEqualTo(true);
        assertThat(getLeadTaskFromDb(lastName).contains(company));
    }

    @Then("^inserts into \"([^\"]*)\" table on \"([^\"]*)\"$")
    public void insertsIntoTable(String tableName, String dbType, DataTable data) {
        dbUtils.setConnection(dbType);
        this.insertsIntoTable(tableName, data);
    }

    @Then("^inserts into \"([^\"]*)\" table$")
    public void insertsIntoTable(String tableName, DataTable data) {
        List<List<String>> dataTable = data.cells();

        String sql = null;

        Iterator it;
        String next;
        for (List<String> list : dataTable) {
            switch (tableName.toUpperCase()) {
                case "TODO":
                    sql = "INSERT INTO TODO(task) VALUES('%s'";
                    break;
                case "TODO WITH ID":
                    sql = "INSERT INTO TODO(id, task) VALUES('%s'";
                    break;
                case "CONTACT":
                    sql = "INSERT INTO CONTACT(first_name, last_name, company, lead_source) VALUES('%s'";
                    break;
            }
            it = list.iterator();
            while (it.hasNext()) {
                next = (String) it.next();
                if (it.hasNext()) {
                    sql = String.format(sql, next) + ", '%s'";
                } else {
                    sql = String.format(sql, next) + ")";
                }
            }
            log.debug("SQL query: *{}*", sql);
            int newId = dbUtils.executeSQLGetUpdateNumber(sql);
            //assert new row in database has been created:
            assertThat(newId).isEqualTo(1);
        }
    }

    @Then("^validate that all todos with task \"([^\"]*)\" have value completed \"(\\w+)\", period in ms: \"(\\w+)\" on \"(\\w+)\"$")
    public void checksThatAllTodosHaveCompletedValDb(String task, Integer val, Integer ms, String dbType) throws InterruptedException, SQLException {
        dbUtils.setConnection(dbType);
        this.checksThatAllTodosHaveCompletedVal(task, val, ms);
    }

    @Then("^validate that all todos with task \"([^\"]*)\" have value completed \"(\\w+)\", period in ms: \"(\\w+)\"$")
    public void checksThatAllTodosHaveCompletedVal(String task, Integer val, Integer ms) throws InterruptedException, SQLException {
        Thread.sleep(ms + 1000);

        ResultSet rs;
        String sql = String.format("SELECT completed FROM TODO WHERE task like '%s'", task);
        log.info("SQL **{}**", sql);
        rs = dbUtils.executeSQLGetResultSet(sql);
        while (rs.next()) {
            assertThat(rs.getInt("completed")).isEqualTo(val);
        }
    }

    @Then("^validate that number of all todos with task \"([^\"]*)\" is \"(\\w+)\", period in ms: \"(\\w+)\"$")
    public void checksNumberOfTodos(String task, Integer val, Integer ms) throws InterruptedException {
        Thread.sleep(TestConfiguration.getJenkinsDelay() * 1000 + ms);
        int number = dbUtils.getNumberOfRecordsInTable("todo", "task", task);
        assertThat(number).isEqualTo(val);
    }

    @Then("^validate that number of all todos with task \"([^\"]*)\" is greater than \"(\\w+)\"$")
    public void checkNumberOfTodosMoreThan(String task, Integer val) {
        int number = dbUtils.getNumberOfRecordsInTable("todo", "task", task);
        assertThat(number).isGreaterThan(val);
    }

    @Then("^verify integration with task \"([^\"]*)\"")
    public void verifyIntegrationWithTask(String task) {
        if (!dbUtils.isConnectionValid()) {
            SampleDbConnectionManager.closeConnections();
            dbUtils = new DbUtils("postgresql");
        }
        int oldTaskCount = dbUtils.getNumberOfRecordsInTable("todo", "task", task);
        TestUtils.sleepIgnoreInterrupt(40000L);
        int newTaskCount = dbUtils.getNumberOfRecordsInTable("todo", "task", task);
        assertThat(newTaskCount).isGreaterThan(oldTaskCount + 5);
    }

    @And("^.*checks? that query \"([^\"]*)\" has \"(\\w+)\" output$")
    public void checkNumberValuesExistInTable(String query, Integer number) {
        assertThat(dbUtils.getCountOfInvokedQuery(query)).isEqualTo(number);
    }

    @And("^.*checks? that query \"([^\"]*)\" has some output$")
    public void checkValuesExistInTable(String query) {
        assertThat(dbUtils.getCountOfInvokedQuery(query)).isGreaterThanOrEqualTo(1);
    }

    @And("^.*checks? that query \"([^\"]*)\" has (\\d+) rows? output$")
    public void checkValuesExistInTable(String query, Integer count) {
        assertThat(dbUtils.getCountOfInvokedQuery(query)).isEqualTo(count);
    }

    @Then("^.*checks? that query \"([^\"]*)\" has no output$")
    public void checkValuesNotExistInTable(String query) {
        assertThat(dbUtils.getCountOfInvokedQuery(query)).isEqualTo(0);
    }

    @When("^.*invokes? database query \"([^\"]*)\"")
    public void invokeQuery(String query) {
        assertThat(dbUtils.executeSQLGetUpdateNumber(query)).isGreaterThanOrEqualTo(0);
    }

    @When("^insert into contact database randomized concur contact with name \"([^\"]*)\" and list ID \"([^\"]*)\"")
    public void createContactRowForConcur(String name, String listId) {
        String surname = RandomStringUtils.randomAlphabetic(12);
        String lead = RandomStringUtils.randomAlphabetic(12);

        String query = "insert into CONTACT values ('" + name + "' , '" + surname + "', '" + listId + "' , '" + lead + "', '1999-01-01')";
        log.info("Invoking query:");
        log.info(query);
        invokeQuery(query);
    }

    @Given("^.*reset content of \"([^\"]*)\" table")
    public void resetTableContent(String tableName) {
        if (tableName.equalsIgnoreCase("contact")) {
            dbUtils.resetContactTable();
        } else {
            //there is no default content in other tables
            dbUtils.deleteRecordsInTable(tableName);
        }
    }

    @Given("^.*truncate \"([^\"]*)\" table")
    public void truncateTable(String tableName) {
        // no special handling for contact table, use resetTableContent if that's what you need
        dbUtils.truncateTable(tableName);
    }

    @Given("^execute SQL command \"([^\"]*)\"$")
    public void executeSql(String sqlCmd) {
        this.executeSqlOnDriver(sqlCmd, "postgresql");
    }

    @Given("^execute SQL command \"([^\"]*)\" on \"([^\"]*)\"$")
    public void executeSqlOnDriver(String sqlCmd, String driver) {
        new DbUtils(driver).executeSQLGetUpdateNumber(sqlCmd);
    }

    @Given("^clean \"([^\"]*)\" table$")
    public void cleanDbTable(String dbTable) {
        this.cleanDbTableOnDriver(dbTable, "postgresql");
    }

    @Given("^clean \"([^\"]*)\" table on \"([^\"]*)\"$")
    public void cleanDbTableOnDriver(String dbTable, String driver) {
        new DbUtils(driver).deleteRecordsInTable(dbTable);
    }

    @Given("^create standard table schema on \"([^\"]*)\" driver$")
    public void createStandardDBSchemaOn(String dbType) {
        new DbUtils(dbType).createSEmptyTableSchema();
    }

    @Given("^allocate new \"([^\"]*)\" database for \"([^\"]*)\" connection$")
    public void allocateNewDatabase(String dbLabel, String connectionName) {

        dbAllocatorClient.allocate(dbLabel);
        log.info("Allocated database: '{}'", dbAllocatorClient.getDbAllocation());
        TestUtils.setDatabaseCredentials(connectionName.toLowerCase(), dbAllocatorClient.getDbAllocation());
    }

    @Given("^free allocated \"([^\"]*)\" database$")
    public void freeAllocatedDatabase(String dbLabel) {
        assertThat(dbAllocatorClient.getDbAllocation().getDbLabel()).isEqualTo(dbLabel);
        dbAllocatorClient.free();
    }

    /**
     * Used for verification of successful creation of a new task in the todo app.
     *
     * @return lead task
     */
    private String getLeadTaskFromDb(String task) {
        String leadTask = null;
        log.info("***SELECT id, task, completed FROM TODO WHERE task LIKE '%" + task + "%'***");
        try (ResultSet rs = dbUtils.executeSQLGetResultSet("SELECT id, task, completed FROM TODO WHERE task LIKE '%"
                + task + "%'");) {
            if (rs.next()) {
                leadTask = rs.getString("task");
                log.debug("task = " + leadTask);
            }
        } catch (SQLException ex) {
            Assertions.fail("Error: " + ex);
        }
        return leadTask;
    }

    private String getLeadTaskFromDb() {
        String leadTask = null;
        try (ResultSet rs = dbUtils.executeSQLGetResultSet("SELECT id, task, completed FROM TODO");) {
            if (rs.next()) {
                leadTask = rs.getString("task");
                log.debug("task = " + leadTask);
            }
        } catch (SQLException ex) {
            Assertions.fail("Error: " + ex);
        }
        return leadTask;
    }

    @Then("^check rows number of table \"([^\"]*)\" is greater than (\\d+) after (\\d+) s$")
    public void checkRowsNumberIsGreaterThan(String table, int threshold, int s) throws InterruptedException, SQLException {
        Thread.sleep(s * 1000 + 1000L);
        String sql = "SELECT COUNT(*) FROM CONTACT";
        log.info("SQL **{}**", sql);
        ResultSet rs = this.dbUtils.executeSQLGetResultSet(sql);

        if (rs.next()) {
            assertThat(rs.getInt(1)).isGreaterThan(threshold);
        } else {
            fail("There is no result for command: " + sql);
        }
    }
}
