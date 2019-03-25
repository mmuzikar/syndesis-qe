package io.syndesis.qe.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

import cz.xtf.openshift.OpenShiftBinaryClient;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.syndesis.common.model.action.Action;
import io.syndesis.common.model.action.ConnectorAction;
import io.syndesis.common.model.connection.Connector;
import io.syndesis.common.model.integration.IntegrationDeploymentState;
import io.syndesis.qe.TestConfiguration;
import io.syndesis.qe.accounts.Account;
import io.syndesis.qe.accounts.AccountsDirectory;
import io.syndesis.qe.endpoints.IntegrationOverviewEndpoint;
import io.syndesis.qe.model.IntegrationOverview;
import io.syndesis.qe.utils.dballoc.DBAllocation;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jknetl
 */
@Slf4j
public final class TestUtils {

    private TestUtils() {
    }

    /**
     * Finds an Action of a given connector.
     * <p>
     * TODO(tplevko): Rework this, when all connectors will be unified to follow the new writing style.
     *
     * @param connector       connector
     * @param connectorPrefix connector prefix
     * @return Action with given prefix or null if no such action can be found.
     */
    public static Action findConnectorAction(Connector connector, String connectorPrefix) {
        Optional<ConnectorAction> action;
        action = connector.getActions()
                .stream()
                .filter(a -> a.getId().get().contains(connectorPrefix))
                .findFirst();

        if (!action.isPresent()) {
            action = connector.getActions()
                    .stream()
                    .filter(a -> a.getDescriptor().getCamelConnectorPrefix().contains(connectorPrefix))
                    .findFirst();
        }

        return action.get();
    }

    /**
     * Waits until a predicate is true or timeout exceeds.
     *
     * @param predicate predicate
     * @param supplier  supplier of values to test by predicate
     * @param unit      TimeUnit for timeout
     * @param timeout   how long to wait for event
     * @param sleepUnit TimeUnit of sleep interval between tests
     * @param sleepTime how long to wait between individual tests (in miliseconds)
     * @param <T>       Type of tested value by a predicate
     * @return True if predicate become true within a timeout, otherwise returns false.
     */
    public static <T> boolean waitForEvent(Predicate<T> predicate, Supplier<T> supplier, TimeUnit unit, long timeout, TimeUnit sleepUnit, long sleepTime) {
        final long start = System.currentTimeMillis();
        long elapsed = 0;
        while (!predicate.test(supplier.get()) && unit.toMillis(timeout) >= elapsed) {
            try {
                sleepUnit.sleep(sleepTime);
            } catch (InterruptedException e) {
                log.debug("Interupted while sleeping", e);
            } finally {
                elapsed = System.currentTimeMillis() - start;
                System.gc();
            }
        }

        return predicate.test(supplier.get());
    }

    public static boolean waitForPublishing(IntegrationOverviewEndpoint e, IntegrationOverview i, TimeUnit unit, long timeout) {
        return waitForState(e, i, IntegrationDeploymentState.Published, unit, timeout);
    }

    /**
     * Waits until integration reaches a specified state or timeout exceeds.
     *
     * @param e       Integration endpoint to obtain current state
     * @param i       integration
     * @param state   desired integration state
     * @param unit    Time unit
     * @param timeout timeout
     * @return True if integration is activated within a timeout. False otherwise.
     */
    public static boolean waitForState(IntegrationOverviewEndpoint e, IntegrationOverview i, IntegrationDeploymentState state, TimeUnit unit, long timeout) {
        return waitForEvent(
                //                integration -> integration.getCurrentStatus().orElse(IntegrationDeploymentState.Pending) == state,
                integration -> integration.getCurrentState() == state,
                () -> getIntegration(e, i.getId()).orElse(i),
                unit,
                timeout,
                TimeUnit.SECONDS,
                10
        );
    }

    private static Optional<IntegrationOverview> getIntegration(IntegrationOverviewEndpoint e, String integrationId) {
        return Optional.of(e.getOverview(integrationId));
    }

    public static LocalPortForward createLocalPortForward(String podName, int remotePort, int localPort) {
        try{
            final Pod podToForward = OpenShiftUtils.getInstance().getAnyPod("syndesis.io/component", podName);
            return OpenShiftUtils.portForward(podToForward, remotePort, localPort);
        }catch (IllegalArgumentException ex){
            throw new IllegalArgumentException(ex.getMessage() + ". Probably Syndesis is not in the namespace.");
        }
    }

    public static LocalPortForward createLocalPortForward(Pod pod, int remotePort, int localPort) {
        return OpenShiftUtils.portForward(pod, remotePort, localPort);
    }

    public static boolean isDcDeployed(String dcName) {
        DeploymentConfig dc = OpenShiftUtils.client().deploymentConfigs().withName(dcName).get();
        return dc != null && dc.getStatus().getReadyReplicas() != null && dc.getStatus().getReadyReplicas() > 0;
    }

    public static void terminateLocalPortForward(LocalPortForward lpf) {
        if (lpf == null) {
            return;
        }
        if (lpf.isAlive()) {
            try {
                lpf.close();
            } catch (IOException ex) {
                log.error("Error: " + ex);
            }
        } else {
            log.info("Local Port Forward already closed.");
        }
    }

    /**
     * Creates map from objects.
     *
     * @param values key1, value1, key2, value2, ...
     * @return map instance with objects
     */
    public static Map<String, String> map(Object... values) {
        final HashMap<String, String> rc = new HashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            rc.put(values[i].toString(), values[i + 1].toString());
        }
        return rc;
    }

    public static void sleepIgnoreInterrupt(long milis) {
        try {
            Thread.sleep(milis);
        } catch (InterruptedException e) {
            log.error("Sleep was interrupted!");
            e.printStackTrace();
        }
    }

    public static void sleepForJenkinsDelayIfHigher(int delayInSeconds) {
        log.debug("sleeping for " + delayInSeconds + " seconds");
        sleepIgnoreInterrupt(TestConfiguration.getJenkinsDelay() > delayInSeconds ? TestConfiguration.getJenkinsDelay() : delayInSeconds * 1000);
    }

    public static void setDatabaseCredentials(String connectionName, DBAllocation dbAllocation) {
        Map<String, String> allocPropertiesMap = dbAllocation.getAllocationMap();

        TestUtils.transhipExternalProperties(connectionName, allocPropertiesMap);
    }

    /**
     * This is method for transhipping externally dynamicaly generated connection data(Database, etc.) into
     * io.syndesis.qe.accounts.Account properties.
     *
     * @param connectionName name of the connection
     * @param sourceMap      source map
     */
    private static void transhipExternalProperties(String connectionName, Map<String, String> sourceMap) {
        Optional<Account> optional = AccountsDirectory.getInstance().getAccount(connectionName);

        Account account;

        if (!optional.isPresent()) {
            account = new Account();
            account.setService(connectionName);
            AccountsDirectory.getInstance().setAccount(connectionName, account);
        } else {
            account = optional.get();
        }

        Map<String, String> properties = account.getProperties();
        if (properties == null) {
            account.setProperties(new HashMap<>());
            properties = account.getProperties();
        }
        switch (account.getService()) {
            case "oracle12":
            case "mysql":
                properties.put("url", sourceMap.get("db.jdbc_url"));
                properties.put("user", sourceMap.get("db.username"));
                properties.put("password", sourceMap.get("db.password"));
                properties.put("schema", sourceMap.get("db.name"));
                log.debug("UPDATED ACCOUNT {} PROPERTIES:", account.getService());
                properties.forEach((key, value) -> log.debug("Key: *{}*, value: *{}*", key, value));
                break;
        }
    }

    /**
     * Checks if the user is cluster admin.
     * @return true/false
     */
    public static boolean isUserAdmin() {
        try {
            OpenShiftUtils.client().users().list();
            return true;
        } catch (KubernetesClientException ex) {
            return false;
        }
    }

    /**
     * Check if the test is running on jenkins.
     * @return true/false
     */
    public static boolean isJenkins() {
        return System.getenv("WORKSPACE") != null;
    }

    /**
     * Checks if we are testing productized bits.
     * @return true/false
     */
    public static boolean isProdBuild() {
        return System.getProperty("syndesis.version").contains("redhat");
    }

    /**
     * Prints pods using oc binary client.
     */
    public static void printPods() {
        // Use oc client directly, as it has nice output
        final String output = OpenShiftBinaryClient.getInstance().executeCommandWithReturn(
                "Unable to list pods",
                "get", "pods", "-n", TestConfiguration.openShiftNamespace()
        );
        log.info(output);
    }
}
