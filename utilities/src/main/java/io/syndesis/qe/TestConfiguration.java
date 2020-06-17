package io.syndesis.qe;

import io.syndesis.qe.utils.TestUtils;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestConfiguration {

    private static final String TEST_PROPERTIES_FILE = "syndesis.config.test.properties";

    public static final String SYNDESIS_VERSION = "syndesis.version";

    public static final String OPENSHIFT_URL = "syndesis.config.openshift.url";
    public static final String OPENSHIFT_TOKEN = "syndesis.config.openshift.token";
    public static final String OPENSHIFT_NAMESPACE = "syndesis.config.openshift.namespace";
    public static final String OPENSHIFT_SAR_NAMESPACE = "syndesis.config.openshift.sar_namespace";
    public static final String OPENSHIFT_NAMESPACE_CLEANUP = "syndesis.config.openshift.namespace.cleanup";
    public static final String OPENSHIFT_OPERATORHUB_DEPLOY = "syndesis.config.openshift.operatorhub.deploy";
    public static final String OPENSHIFT_ROUTE_SUFFIX = "syndesis.config.openshift.route.suffix";

    public static final String SYNDESIS_SINGLE_USER = "syndesis.config.single.user";
    public static final String SYNDESIS_ADMIN_USERNAME = "syndesis.config.admin.username";
    public static final String SYNDESIS_ADMIN_PASSWORD = "syndesis.config.admin.password";
    public static final String SYNDESIS_UI_USERNAME = "syndesis.config.ui.username";
    public static final String SYNDESIS_UI_PASSWORD = "syndesis.config.ui.password";
    public static final String SYNDESIS_UI_URL = "syndesis.config.ui.url";
    public static final String SYNDESIS_UI_BROWSER = "syndesis.config.ui.browser";
    public static final String SYNDESIS_URL_SUFFIX = "syndesis.config.url.suffix";
    public static final String SYNDESIS_CALLBACK_URL_SUFFIX = "syndesis.config.callbackUrlSuffix";
    public static final String SYNDESIS_ENABLE_TEST_SUPPORT = "syndesis.config.enableTestSupport";

    public static final String SYNDESIS_REST_API_PATH = "syndesis.config.rest.api.path";
    public static final String SYNDESIS_SERVER_ROUTE = "syndesis.config.server.route";

    public static final String SYNDESIS_CREDENTIALS_FILE = "syndesis.config.credentials.file";
    public static final String SYNDESIS_VERSIONS_FILE = "syndesis.config.versions.file";

    public static final String SYNDESIS_INSTALL_VERSION = "syndesis.config.install.version";
    public static final String SYNDESIS_CRD_URL = "syndesis.config.crd.url";
    public static final String SYNDESIS_OPERATOR_IMAGE = "syndesis.config.operator.image";
    public static final String SYNDESIS_CR_URL = "syndesis.config.cr.url";

    public static final String SYNDESIS_BUILD_PROPERTIES_URL = "syndesis.config.build.properties.url";

    public static final String SYNDESIS_PULL_SECRET = "syndesis.config.pull.secret";
    public static final String SYNDESIS_PULL_SECRET_NAME = "syndesis.config.pull.secret.name";

    public static final String SYNDESIS_PUBLIC_OAUTH_PROXY_URL = "syndesis.config.template.public.oath.proxy.url";

    public static final String SYNDESIS_CUSTOM_RESOURCE_PLURAL = "syndesis.config.custom.resource.plural";

    public static final String SYNDESIS_NIGHTLY_VERSION = "syndesis.nightly.version";

    public static final String JENKINS_DELAY = "jenkins.delay";
    public static final String TESTSUITE_TIMEOUT = "syndesis.config.timeout";

    public static final String DB_ALLOCATOR_URL = "syndesis.dballocator.url";

    public static final String PROD_REPOSITORY = "syndesis.config.prod.repository";
    public static final String UPSTREAM_REPOSITORY = "syndesis.config.upstream.repository";

    public static final String SKIP_TESTS_WITH_OPEN_ISSUES = "syndesis.skip.open.issues";

    public static final String STATE_CHECK_INTERVAL = "syndesis.server.state.check.interval";
    public static final String SNOOP_SELECTORS = "syndesis.config.snoop.selectors";

    public static final String SYNDESIS_UPGRADE_PREVIOUS_VERSION = "syndesis.upgrade.previous.version";

    public static final String SYNDESIS_UPGRADE_CURRENT_VERSION = "syndesis.upgrade.current.version";

    public static final String SYNDESIS_UPGRADE_PREVIOUS_OPERATOR_IMAGE = "syndesis.upgrade.previous.operator";

    public static final String SYNDESIS_UPGRADE_PREVIOUS_CRD = "syndesis.upgrade.previous.crd";

    public static final String SYNDESIS_UPGRADE_PREVIOUS_CR = "syndesis.upgrade.previous.cr";

    private static final String BROWSER_BINARY_PATH = "syndesis.config.browser.path";

    private static final String CAMEL_K_VERSION = "syndesis.camelk.version";
    private static final String SYNDESIS_RUNTIME = "syndesis.config.runtime";

    private static final String JAEGER_VERSION = "syndesis.jaeger.version";
    private static final String CAMEL_VERSION = "camel.version";

    private static final String SYNDESIS_APPEND_REPOSITORY = "syndesis.config.append.repository";

    private static final String QUAY_USERNAME = "syndesis.config.quay.username";
    private static final String QUAY_PASSWORD = "syndesis.config.quay.password";
    private static final String QUAY_NAMESPACE = "syndesis.config.quay.namespace";
    private static final String QUAY_AUTH_TOKEN = "syndesis.config.quay.auth.token";
    private static final String QUAY_OPSRC_TOKEN = "syndesis.config.quay.opsrc.token";

    private static final TestConfiguration INSTANCE = new TestConfiguration();

    private final Properties properties = new Properties();

    private TestConfiguration() {
        try {
            // first let's try properties in module dir
            copyValues(fromPath("test.properties"), true);

            // then properties in repo root
            copyValues(fromPath("../" + System.getProperty(TEST_PROPERTIES_FILE, "test.properties")));

            // build properties file
            copyValues(fromPath(System.getProperty(SYNDESIS_BUILD_PROPERTIES_URL)));

            // then system variables
            copyValues(System.getProperties());

            // then environment variables
            // TODO: copyValues(fromEnvironment());

            // then defaults
            copyValues(defaultValues());
        } catch (Exception e) {
            // If it fails here, then the user doesn't get any meaningful error other than:
            // java.lang.NoClassDefFoundError: Could not initialize class io.syndesis.qe.TestConfiguration
            log.error(
                "Error while creating TestConfiguration class! Please check if all mandatory properties are set" +
                    " (either in test.properties or via the command line)");
            e.printStackTrace();
            throw e;
        }
    }

    public static TestConfiguration get() {
        return INSTANCE;
    }

    public static String syndesisVersion() {
        return get().readValue(SYNDESIS_VERSION);
    }

    public static String openShiftUrl() {
        return get().readValue(OPENSHIFT_URL);
    }

    public static String openShiftToken() {
        return get().readValue(OPENSHIFT_TOKEN);
    }

    public static String openShiftNamespace() {
        return get().readValue(OPENSHIFT_NAMESPACE);
    }

    // Namespace for the subject access review. If unspecified, will use the same namespace as for the deployment.
    public static String openShiftSARNamespace() {
        return get().readValue(OPENSHIFT_SAR_NAMESPACE, get().readValue(OPENSHIFT_NAMESPACE));
    }

    public static String openShiftRouteSuffix() {
        if (get().readValue(OPENSHIFT_ROUTE_SUFFIX) == null) {
            final String prefix = "apps.";
            if (get().readValue(SYNDESIS_UI_URL) != null && get().readValue(SYNDESIS_UI_URL).contains("syndesis.my-minishift.syndesis.io")) {
                //for jenkins
                get().overrideProperty(OPENSHIFT_ROUTE_SUFFIX, "my-minishift.syndesis.io");
            } else if (openShiftUrl().endsWith("8443")) {
                //OCP 3.11
                if (openShiftUrl().matches("https:\\/\\/(\\d{1,4}\\.){3}\\d{1,4}:8443")) {
                    //minishift
                    get().overrideProperty(OPENSHIFT_ROUTE_SUFFIX, StringUtils.substringBetween(openShiftUrl(), "https://", ":8443") + ".nip.io");
                } else {
                    //remote instance
                    get().overrideProperty(OPENSHIFT_ROUTE_SUFFIX, prefix + StringUtils.substringBetween(openShiftUrl(), "https://master.", ":8443"));
                }
            } else {
                //OCP 4.x
                if (openShiftUrl().contains("https://api.crc.testing:6443")) {
                    //CRC
                    get().overrideProperty(OPENSHIFT_ROUTE_SUFFIX, "apps-crc.testing");
                } else {
                    get().overrideProperty(OPENSHIFT_ROUTE_SUFFIX, prefix + StringUtils.substringBetween(openShiftUrl(), "https://api.", ":6443"));
                }
            }
        }
        return get().readValue(OPENSHIFT_ROUTE_SUFFIX);
    }

    public static String syndesisUsername() {
        return get().readValue(SYNDESIS_UI_USERNAME);
    }

    public static String syndesisPassword() {
        return get().readValue(SYNDESIS_UI_PASSWORD);
    }

    public static String adminUsername() {
        return get().readValue(SYNDESIS_ADMIN_USERNAME);
    }

    public static String adminPassword() {
        return get().readValue(SYNDESIS_ADMIN_PASSWORD);
    }

    public static String syndesisUrl() {
        if (get().readValue(SYNDESIS_UI_URL) == null) {
            // If the UI URL isn't set, use the same style as the default route created by the operator
            // "https://syndesis-<namespace>.apps.<cluster>"
            final String prefix = "https://syndesis-" + openShiftNamespace() + ".";
            get().overrideProperty(SYNDESIS_UI_URL, prefix + openShiftRouteSuffix());
        }
        return get().readValue(SYNDESIS_UI_URL);
    }

    public static String syndesisCallbackUrlSuffix() {
        return get().readValue(SYNDESIS_CALLBACK_URL_SUFFIX);
    }

    public static String syndesisRestApiPath() {
        return get().readValue(SYNDESIS_REST_API_PATH);
    }

    public static String syndesisCredentialsFile() {
        return get().readValue(SYNDESIS_CREDENTIALS_FILE);
    }

    public static String syndesisVersionsFile() {
        return get().readValue(SYNDESIS_VERSIONS_FILE);
    }

    public static String syndesisPublicOauthProxyTemplateUrl() {
        return get().readValue(SYNDESIS_PUBLIC_OAUTH_PROXY_URL);
    }

    public static String syndesisPullSecret() {
        return get().readValue(SYNDESIS_PULL_SECRET);
    }

    public static String syndesisPullSecretName() {
        return get().readValue(SYNDESIS_PULL_SECRET_NAME);
    }

    public static boolean enableTestSupport() {
        return Boolean.parseBoolean(get().readValue(SYNDESIS_ENABLE_TEST_SUPPORT, "false"));
    }

    public static String syndesisCrdUrl() {
        return get().readValue(SYNDESIS_CRD_URL);
    }

    public static String syndesisOperatorImage() {
        return get().readValue(SYNDESIS_OPERATOR_IMAGE);
    }

    public static String syndesisCrUrl() {
        return get().readValue(SYNDESIS_CR_URL);
    }

    public static String syndesisBrowser() {
        return get().readValue(SYNDESIS_UI_BROWSER);
    }

    public static boolean namespaceCleanup() {
        return Boolean.parseBoolean(get().readValue(OPENSHIFT_NAMESPACE_CLEANUP));
    }

    public static boolean operatorhubDeploy() {
        return Boolean.parseBoolean(get().readValue(OPENSHIFT_OPERATORHUB_DEPLOY));
    }

    public static boolean useServerRoute() {
        return Boolean.parseBoolean(get().readValue(SYNDESIS_SERVER_ROUTE));
    }

    public static String customResourcePlural() {
        return get().readValue(SYNDESIS_CUSTOM_RESOURCE_PLURAL);
    }

    public static int getJenkinsDelay() {
        return Integer.parseInt(get().readValue(JENKINS_DELAY, "1"));
    }

    public static int getConfigTimeout() {
        return Integer.parseInt(get().readValue(TESTSUITE_TIMEOUT, "30"));
    }

    public static String getDbAllocatorUrl() {
        return get().readValue(DB_ALLOCATOR_URL, "localhost:8080");
    }

    public static String prodRepository() {
        return get().readValue(PROD_REPOSITORY);
    }

    public static String upstreamRepository() {
        return get().readValue(UPSTREAM_REPOSITORY);
    }

    public static Boolean skipTestsWithOpenIssues() {
        return Boolean.parseBoolean(get().readValue(SKIP_TESTS_WITH_OPEN_ISSUES));
    }

    public static int stateCheckInterval() {
        return Integer.parseInt(get().readValue(STATE_CHECK_INTERVAL));
    }

    public static boolean snoopSelectors() {
        return Boolean.parseBoolean(get().readValue(SNOOP_SELECTORS));
    }

    public static String image(Image image) {
        return get().readValue(image.name());
    }

    public static String camelKVersion() {
        return get().readValue(CAMEL_K_VERSION);
    }

    public static String jaegerVersion() {
        return get().readValue(JAEGER_VERSION);
    }

    public static String syndesisInstallVersion() {
        return get().readValue(SYNDESIS_INSTALL_VERSION);
    }

    public static String syndesisNightlyVersion() {
        return get().readValue(SYNDESIS_NIGHTLY_VERSION);
    }

    public static String upgradePreviousVersion() {
        return get().readValue(SYNDESIS_UPGRADE_PREVIOUS_VERSION);
    }

    public static String upgradeCurrentVersion() {
        return get().readValue(SYNDESIS_UPGRADE_CURRENT_VERSION);
    }

    public static String upgradePreviousOperatorImage() {
        return get().readValue(SYNDESIS_UPGRADE_PREVIOUS_OPERATOR_IMAGE);
    }

    public static String upgradePreviousCrd() {
        return get().readValue(SYNDESIS_UPGRADE_PREVIOUS_CRD);
    }

    public static String upgradePreviousCr() {
        return get().readValue(SYNDESIS_UPGRADE_PREVIOUS_CR);
    }

    public static String syndesisRuntime() {
        return get().readValue(SYNDESIS_RUNTIME);
    }

    public static boolean appendRepository() {
        return Boolean.parseBoolean(get().readValue(SYNDESIS_APPEND_REPOSITORY, "false"));
    }

    public static String quayUsername() {
        return get().readValue(QUAY_USERNAME);
    }

    public static String quayPassword() {
        return get().readValue(QUAY_PASSWORD);
    }

    public static String quayNamespace() {
        return get().readValue(QUAY_NAMESPACE);
    }

    public static String quayAuthToken() {
        return get().readValue(QUAY_AUTH_TOKEN);
    }

    public static String quayOpsrcToken() {
        return get().readValue(QUAY_OPSRC_TOKEN);
    }

    private Properties defaultValues() {
        final Properties defaultProps = new Properties();

        defaultProps.setProperty(OPENSHIFT_URL, "");
        defaultProps.setProperty(OPENSHIFT_TOKEN, "");
        defaultProps.setProperty(SYNDESIS_REST_API_PATH, "/api/v1");
        defaultProps.setProperty(SYNDESIS_SERVER_ROUTE, "false");

        defaultProps.setProperty(SYNDESIS_CREDENTIALS_FILE, "../credentials.json");
        defaultProps.setProperty(SYNDESIS_VERSIONS_FILE, "src/test/resources/dependencyVersions.properties");

        defaultProps.setProperty(SYNDESIS_UI_BROWSER, "chrome");

        defaultProps.setProperty(OPENSHIFT_NAMESPACE_CLEANUP, "false");
        defaultProps.setProperty(OPENSHIFT_OPERATORHUB_DEPLOY, "false");

        // to keep backward compatibility
        if (properties.getProperty(SYNDESIS_URL_SUFFIX) != null && properties.getProperty(OPENSHIFT_ROUTE_SUFFIX) == null) {
            defaultProps.setProperty(OPENSHIFT_ROUTE_SUFFIX, properties.getProperty(SYNDESIS_URL_SUFFIX));
        }

        String syndesisVersion;
        if (System.getProperty(SYNDESIS_INSTALL_VERSION) != null) {
            syndesisVersion = System.getProperty(SYNDESIS_INSTALL_VERSION);
        } else {
            syndesisVersion = "master";
            // only use defined system property if it doesnt end with SNAPSHOT and it is not prod build
            if (!properties.getProperty(SYNDESIS_VERSION).endsWith("SNAPSHOT") && !properties.getProperty(SYNDESIS_VERSION).contains("redhat")) {
                syndesisVersion = System.getProperty(SYNDESIS_VERSION);
            }
        }

        if (properties.getProperty(SYNDESIS_PUBLIC_OAUTH_PROXY_URL) == null) {
            defaultProps.setProperty(SYNDESIS_PUBLIC_OAUTH_PROXY_URL, String
                .format("https://raw.githubusercontent.com/syndesisio/syndesis/%s/install/support/syndesis-public-oauth-proxy.yml", syndesisVersion));
        }

        defaultProps.setProperty(SYNDESIS_CRD_URL, getClass().getClassLoader().getResource("syndesis-crd.yaml").toString());
        String operatorVersion;
        if (this.properties.getProperty(SYNDESIS_INSTALL_VERSION) != null) {
            operatorVersion = this.properties.getProperty(SYNDESIS_INSTALL_VERSION);
        } else {
            operatorVersion = "latest";
        }
        defaultProps.setProperty(SYNDESIS_OPERATOR_IMAGE, "syndesis/syndesis-operator" + ':' + operatorVersion);

        if (properties.getProperty(SYNDESIS_CR_URL) == null) {
            defaultProps.setProperty(SYNDESIS_CR_URL, "https://raw.githubusercontent.com/syndesisio/fuse-online-install/master/default-cr.yml");
        }

        defaultProps.setProperty(SYNDESIS_CUSTOM_RESOURCE_PLURAL, "syndesises");

        // When the single user property is set (for the env where the syndesis is already deployed and you are not an admin)
        // Make the user "admin" anyway, as that user is used in all k8s client invocations by default
        if (properties.getProperty(SYNDESIS_SINGLE_USER) != null) {
            defaultProps.setProperty(SYNDESIS_ADMIN_USERNAME, properties.getProperty(SYNDESIS_UI_USERNAME));
        }
        if (properties.getProperty(SYNDESIS_SINGLE_USER) != null) {
            defaultProps.setProperty(SYNDESIS_ADMIN_PASSWORD, properties.getProperty(SYNDESIS_UI_PASSWORD));
        }

        // Copy syndesis properties to their xtf counterparts - used by binary oc client
        System.setProperty("xtf.openshift.url", properties.getProperty(OPENSHIFT_URL));
        System.setProperty("xtf.openshift.master.username", properties.getProperty(SYNDESIS_ADMIN_USERNAME));
        System.setProperty("xtf.openshift.master.password", properties.getProperty(SYNDESIS_ADMIN_PASSWORD));
        System.setProperty("xtf.openshift.namespace", properties.getProperty(OPENSHIFT_NAMESPACE));

        // Set oc version - this version of the client will be used as the binary client
        System.setProperty("xtf.openshift.version", "4.3");

        if (properties.getProperty(SYNDESIS_RUNTIME) == null) {
            defaultProps.setProperty(SYNDESIS_RUNTIME, "springboot");
        }
        if (properties.getProperty(SYNDESIS_PULL_SECRET_NAME) == null) {
            defaultProps.setProperty(SYNDESIS_PULL_SECRET_NAME, "syndesis-pull-secret");
        }

        defaultProps.setProperty(STATE_CHECK_INTERVAL, "" + (TestUtils.isJenkins() ? 150 : 60));
        if (properties.getProperty(SNOOP_SELECTORS) == null) {
            defaultProps.setProperty(SNOOP_SELECTORS, "false");
        }

        if (properties.getProperty(CAMEL_K_VERSION) == null) {
            defaultProps.setProperty(CAMEL_K_VERSION, "0.3.4");
        }

        if (properties.getProperty(JAEGER_VERSION) == null) {
            defaultProps.setProperty(JAEGER_VERSION, "v1.17.0");
        }

        /**
         * Resteasy 4.4 uses 2.10 jackson that has this security feature due to CVEs and the only option is to use system properties for that
         * Needed for atlasmap to be able to deserialize inspection response
         */
        System.setProperty("resteasy.jackson.deserialization.whitelist.allowIfBaseType.prefix", "*");
        System.setProperty("resteasy.jackson.deserialization.whitelist.allowIfSubType.prefix", "*");

        if (properties.getProperty(SYNDESIS_APPEND_REPOSITORY) == null) {
            defaultProps.setProperty(SYNDESIS_APPEND_REPOSITORY, "true");
        }
        return defaultProps;
    }

    public String readValue(final String key) {
        return readValue(key, null);
    }

    public String readValue(final String key, final String defaultValue) {
        return this.properties.getProperty(key, defaultValue);
    }

    private Properties fromPath(final String path) {
        final Properties props = new Properties();

        if (path == null) {
            return props;
        }

        final Path propsPath;
        if (path.startsWith("file:")) {
            propsPath = Paths.get(URI.create(path));
        } else {
            propsPath = Paths.get(path).toAbsolutePath();
        }

        InputStream is = null;

        try {
            if (path.startsWith("http")) {
                is = new URL(path).openStream();
            } else if (Files.isReadable(propsPath)) {
                is = Files.newInputStream(propsPath);
            }

            if (is != null) {
                log.debug("Loading properties from " + path);
                props.load(is);
                is.close();
            }
        } catch (IOException e) {
            log.warn("Unable to read properties from " + propsPath, e);
        }

        return props;
    }

    private void copyValues(final Properties source) {
        copyValues(source, false);
    }

    private void copyValues(final Properties source, final boolean overwrite) {
        source.stringPropertyNames().stream()
            .filter(key -> overwrite || !this.properties.containsKey(key))
            .forEach(key -> this.properties.setProperty(key, source.getProperty(key)));
    }

    public static Optional<String> browserBinary() {
        return Optional.ofNullable(get().readValue(BROWSER_BINARY_PATH));
    }

    /**
     * @param key
     * @param value
     */
    public void overrideProperty(String key, String value) {
        properties.put(key, value);
    }

    /**
     * @param key
     */
    public void clearProperty(String key) {
        properties.remove(key);
    }
}
