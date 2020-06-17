package io.syndesis.qe.resource.impl;

import static org.assertj.core.api.Assertions.fail;

import io.syndesis.qe.Addon;
import io.syndesis.qe.Component;
import io.syndesis.qe.Image;
import io.syndesis.qe.TestConfiguration;
import io.syndesis.qe.bdd.CommonSteps;
import io.syndesis.qe.resource.Resource;
import io.syndesis.qe.resource.ResourceFactory;
import io.syndesis.qe.test.InfraFail;
import io.syndesis.qe.utils.OpenShiftUtils;
import io.syndesis.qe.utils.RestUtils;
import io.syndesis.qe.utils.TestUtils;
import io.syndesis.qe.utils.TodoUtils;
import io.syndesis.qe.wait.OpenShiftWaitUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionFluent;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionVersion;
import io.fabric8.kubernetes.api.model.apiextensions.DoneableCustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.dsl.internal.RawCustomResourceOperationsImpl;
import io.fabric8.openshift.api.model.DeploymentConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Syndesis implements Resource {
    private static final String CR_NAME = "app";

    @Setter
    @Getter
    private String crdUrl;
    @Setter
    @Getter
    private String operatorImage;
    @Setter
    @Getter
    private String crUrl;

    private String crApiVersion;

    public Syndesis() {
        defaultValues();
    }

    public void defaultValues() {
        crdUrl = TestConfiguration.syndesisCrdUrl();
        operatorImage = TestConfiguration.syndesisOperatorImage();
        crUrl = TestConfiguration.syndesisCrUrl();
    }

    @Override
    public void deploy() {
        log.info("Deploying Syndesis");
        log.info("  Cluster:   " + TestConfiguration.openShiftUrl());
        log.info("  Namespace: " + TestConfiguration.openShiftNamespace());
        createPullSecret();
        deployCrd();
        pullOperatorImage();
        installCluster();
        grantPermissions();
        deployOperator();

        deployCrAndRoutes();
    }

    public void deployCrAndRoutes() {
        log.info("Deploying Syndesis CR");

        deploySyndesisViaOperator();
        changeRuntime(TestConfiguration.syndesisRuntime());
        checkRoute();
        TodoUtils.createDefaultRouteForTodo("todo2", "/");
        jaegerWorkarounds();
    }

    @Override
    public void undeploy() {
        // Intentionally left blank to preserve current behavior - after test execution, syndesis was left installed and every other resource was
        // undeployed
        // We may need to revisit this later
        log.warn("Skipping Syndesis undeployment");
    }

    @Override
    public boolean isReady() {
        EnumSet<Component> components = Component.getAllComponents();
        List<Pod> syndesisPods = Component.getComponentPods();
        return syndesisPods.size() == components.size() && syndesisPods.stream().allMatch(OpenShiftWaitUtils::isPodReady);
    }

    @Override
    public boolean isDeployed() {
        return true;
    }

    public boolean isUndeployed() {
        List<Pod> syndesisPods = Component.getComponentPods();
        // Either 0 pods when the namespace was empty before undeploying, or 1 pod (the operator)
        return syndesisPods.size() == 0 || (syndesisPods.size() == 1 && syndesisPods.get(0).getMetadata().getName().startsWith("syndesis-operator"));
    }

    public void undeployCustomResources() {
        // if we don't have CRD, we can't have CRs
        if (getCrd() != null) {
            getCrNames().forEach((version, names) -> names.forEach(name -> undeployCustomResource(name, version)));
        }
    }

    /**
     * Undeploys syndesis custom resource using openshift API.
     *
     * @param name custom resource name
     */
    private void undeployCustomResource(String name, String version) {
        deleteCr(name, version);
    }

    public void createPullSecret() {
        if (TestConfiguration.syndesisPullSecret() != null) {
            log.info("Creating a pull secret with name " + TestConfiguration.syndesisPullSecretName());
            OpenShiftUtils.getInstance().secrets().createOrReplaceWithNew()
                .withNewMetadata()
                .withName(TestConfiguration.syndesisPullSecretName())
                .endMetadata()
                .withData(TestUtils.map(".dockerconfigjson", TestConfiguration.syndesisPullSecret()))
                .withType("kubernetes.io/dockerconfigjson")
                .done();
        }
    }

    /**
     * Ensures that jaeger is working correctly by linking secrets.
     * The syndesis-jaeger doesn't contain "syndesis.io/component" label which is using for finding all components. It is added manually here
     */
    public void jaegerWorkarounds() {
        new Thread(() -> {
            try {
                OpenShiftWaitUtils.waitUntilPodAppears("jaeger-operator");
                ensureImagePullForJaegerOperator();
                OpenShiftWaitUtils.waitUntilPodAppears("syndesis-jaeger");
                ensureImagePullForSyndesisJaeger();
                Optional<Pod> jaegerPod = OpenShiftUtils.getPodByPartialName("syndesis-jaeger");
                OpenShiftUtils.getInstance().pods().withName(jaegerPod.get().getMetadata().getName()).edit()
                    .editMetadata().addToLabels("syndesis.io/component", "syndesis-jaeger").endMetadata().done();
            } catch (Exception e) {
                log.warn("Syndesis-jaeger pod never reached ready state! " +
                    "Ignore when the Syndesis is configured to use external Jaeger instance or old DB activity tracking. Exception in case of " +
                    "debugging: " +
                    e);
            }
        }).start();
    }

    public void installCluster() {
        executeOperatorCommandAndWait(
            "install",
            "cluster");
    }

    /**
     * Pulls the operator image via docker pull.
     */
    public void pullOperatorImage() {
        log.info("Pulling operator image {}", operatorImage);
        ProcessBuilder dockerPullPb = new ProcessBuilder("docker",
            "pull",
            operatorImage
        );

        try {
            dockerPullPb.start().waitFor();
        } catch (Exception e) {
            log.error("Could not pull operator image", e);
            fail("Failed to pull operator");
        }
    }

    /**
     * Grants the permissions via the admin user to the regular user.
     */
    public void grantPermissions() {
        log.info("Granting permissions to user {}", TestConfiguration.syndesisUsername());
        executeOperatorCommandAndWait(
            "grant",
            "-u",
            TestConfiguration.syndesisUsername()
        );
    }

    public void executeOperatorCommandAndWait(String... param) {
        try {
            Process process = this.executeOperatorCommand(param);
            process.waitFor();
            if (process.exitValue() != 0) {
                fail("The docker operator command fail. The exit value is " + process.exitValue() +
                    "\nThe process error stream: " + IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8) +
                    "\nThe process input stream: " + IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8));
            }
        } catch (InterruptedException | IOException e) {
            log.error("Something interrupted the docker command", e);
            fail("Something interrupted the docker command");
        }
    }

    public Process executeOperatorCommand(String... param) {
        Process result = null;
        final String[] dockerCommand = {"docker",
            "run",
            "--rm",
            "-v",
            OpenShiftUtils.binary().getOcConfigPath() + ":/tmp/kube/config:z",
            "--entrypoint",
            "syndesis-operator",
            operatorImage
        };

        final String[] staticParam = {"--namespace",
            TestConfiguration.openShiftNamespace(),
            "--config",
            "/tmp/kube/config"
        };

        String[] finalMergedCommand = (String[]) ArrayUtils.addAll(ArrayUtils.addAll(dockerCommand, param), staticParam);
        new File(OpenShiftUtils.binary().getOcConfigPath()).setReadable(true, false);
        try {
            result = new ProcessBuilder(finalMergedCommand).start();
        } catch (IOException e) {
            log.error("Unable to perform docker command", e);
            fail("Unable to perform docker command");
        }
        return result;
    }

    /**
     * In case of multiple uses of a static route, openshift will create the route anyway with a false condition, so rather fail fast.
     */
    public void checkRoute() {
        try {
            OpenShiftWaitUtils.waitFor(() -> OpenShiftUtils.getInstance().routes().withName("syndesis").get() != null, 120000L);
            OpenShiftWaitUtils.waitFor(() -> OpenShiftUtils.getInstance().routes().withName("syndesis").get()
                .getStatus().getIngress() != null, 120000L);
        } catch (TimeoutException | InterruptedException e) {
            InfraFail.fail("Unable to find syndesis route in 120s");
        }

        if ("false".equalsIgnoreCase(
            OpenShiftUtils.getInstance().routes().withName("syndesis").get().getStatus().getIngress().get(0).getConditions().get(0).getStatus())) {
            InfraFail.fail("Syndesis route failed to provision because of: " +
                OpenShiftUtils.getInstance().routes().withName("syndesis").get().getStatus().getIngress().get(0).getConditions().get(0).getMessage());
        }
    }

    public Map<String, Object> getCr() {
        return getSyndesisCrClient().get(TestConfiguration.openShiftNamespace(), CR_NAME);
    }

    public void createCr(Map<String, Object> cr) {
        RawCustomResourceOperationsImpl syndesisCrClient = getSyndesisCrClient();
        OpenShiftUtils.asRegularUser(() -> {
            try {
                syndesisCrClient.create(TestConfiguration.openShiftNamespace(), cr);
            } catch (IOException e) {
                fail("Unable to create CR: " + e);
            }
        });
    }

    public void editCr(Map<String, Object> cr) {
        RawCustomResourceOperationsImpl syndesisCrClient = getSyndesisCrClient();
        OpenShiftUtils.asRegularUser(() -> {
            try {
                syndesisCrClient.edit(TestConfiguration.openShiftNamespace(), CR_NAME, cr);
            } catch (IOException e) {
                fail("Unable to modify CR: " + e);
            }
        });
    }

    private void deleteCr(String name, String version) {
        log.info("Undeploying custom resource \"{}\" in version \"{}\"", name, version);
        RawCustomResourceOperationsImpl syndesisCrClient = getSyndesisCrClient(version);
        OpenShiftUtils.asRegularUser(() -> syndesisCrClient.delete(TestConfiguration.openShiftNamespace(), name));
    }

    private Map<String, Set<String>> getCrNames() {
        final Map<String, Set<String>> versionAndNames = new HashMap<>();
        Map<String, Object> crs = new HashMap<>();
        // CustomResourceDefinition can have multiple versions, so loop over all versions and gather all custom resources in this namespace
        // (There should be always only one, but to be bullet-proof)
        for (CustomResourceDefinitionVersion version : getCrd().getSpec().getVersions()) {
            try {
                crs.putAll(getSyndesisCrClient(version.getName()).list(TestConfiguration.openShiftNamespace()));
            } catch (KubernetesClientException kce) {
                // If there are no custom resources with this version, ignore
                if (!kce.getMessage().contains("404")) {
                    throw kce;
                }
            }
        }
        JSONArray items = new JSONArray();
        try {
            items = new JSONObject(crs).getJSONArray("items");
        } catch (JSONException ex) {
            // probably the CRD isn't present in the cluster
        }
        for (int i = 0; i < items.length(); i++) {
            final String version = StringUtils.substringAfter(items.getJSONObject(i).getString("apiVersion"), "/");
            versionAndNames.computeIfAbsent(version, v -> new HashSet<>());
            versionAndNames.get(version).add(items.getJSONObject(i).getJSONObject("metadata").getString("name"));
        }

        return versionAndNames;
    }

    public RawCustomResourceOperationsImpl getSyndesisCrClient() {
        return OpenShiftUtils.getInstance().customResource(makeSyndesisContext());
    }

    public RawCustomResourceOperationsImpl getSyndesisCrClient(String version) {
        return OpenShiftUtils.getInstance().customResource(makeSyndesisContext(version));
    }

    public CustomResourceDefinition getCrd() {
        return OpenShiftUtils.getInstance().customResourceDefinitions().withName("syndesises.syndesis.io").get();
    }

    private CustomResourceDefinitionContext makeSyndesisContext() {
        return makeSyndesisContext(getCrApiVersion());
    }

    private CustomResourceDefinitionContext makeSyndesisContext(String version) {
        CustomResourceDefinition syndesisCrd = getCrd();
        CustomResourceDefinitionContext.Builder builder = new CustomResourceDefinitionContext.Builder()
            .withGroup(syndesisCrd.getSpec().getGroup())
            .withPlural(syndesisCrd.getSpec().getNames().getPlural())
            .withScope(syndesisCrd.getSpec().getScope())
            .withVersion(version);
        return builder.build();
    }

    public void deployCrd() {
        log.info("Creating custom resource definition from " + crdUrl);
        CustomResourceDefinition newCrd;
        try (InputStream is = new URL(crdUrl).openStream()) {
            newCrd = OpenShiftUtils.getInstance().customResourceDefinitions().load(is).get();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to load CRD", ex);
        }

        CustomResourceDefinition existingCrd = OpenShiftUtils.getInstance().customResourceDefinitions()
            .withName(newCrd.getMetadata().getName()).get();
        if (existingCrd == null) {
            // Just create a new CRD
            OpenShiftUtils.getInstance().customResourceDefinitions().create(newCrd);
        } else {
            // Edit the existing CRD, if it doesn't contain the version we want to deploy now
            // else do nothing, as the existing crd and new crd are probably the same
            List<CustomResourceDefinitionVersion> versions = OpenShiftUtils.getInstance().customResourceDefinitions()
                .withName(existingCrd.getMetadata().getName()).get().getSpec().getVersions();
            if (existingCrd.getSpec().getVersions().stream().noneMatch(v -> newCrd.getSpec().getVersion().equals(v.getName()))) {
                CustomResourceDefinitionFluent.SpecNested<DoneableCustomResourceDefinition> crd =
                    OpenShiftUtils.getInstance().customResourceDefinitions().withName(existingCrd.getMetadata().getName())
                        .edit()
                        .editSpec()
                        // Add a new version
                        .addNewVersion()
                        .withName(newCrd.getSpec().getVersion())
                        .withServed(true)
                        .withStorage(true)
                        .endVersion();
                versions.stream().filter(v -> !v.getName().equals(newCrd.getSpec().getVersion()))
                    .forEach(v -> crd.editMatchingVersion(mv -> mv.getName().equals(v.getName())).withServed(true).withStorage(false).endVersion());
                crd.endSpec()
                    .editStatus()
                    // Also add it to stored versions
                    .addToStoredVersions(newCrd.getSpec().getVersion())
                    .endStatus()
                    .done();
            } else {
                // We need to make "current" CRD version "served" and with "storage"
                CustomResourceDefinitionFluent.SpecNested<DoneableCustomResourceDefinition> crd =
                    OpenShiftUtils.getInstance().customResourceDefinitions().withName(existingCrd.getMetadata().getName())
                        .edit()
                        .editSpec()
                        // Edit the version we want to deploy now
                        .editMatchingVersion(v -> v.getName().equals(newCrd.getSpec().getVersion()))
                        .withServed(true)
                        .withStorage(true)
                        .endVersion();
                versions.stream().filter(v -> !v.getName().equals(newCrd.getSpec().getVersion()))
                    .forEach(v -> crd.editMatchingVersion(mv -> mv.getName().equals(v.getName())).withServed(true).withStorage(false).endVersion());
                crd.endSpec().done();
            }
        }
    }

    public List<HasMetadata> getOperatorResources() {
        String imageName = StringUtils.substringBeforeLast(operatorImage, ":");
        String imageTag = StringUtils.substringAfterLast(operatorImage, ":");

        log.info("Generating resources using operator image {}", operatorImage);
        List<HasMetadata> resourceList = null;
        try {
            Process p = this.executeOperatorCommand(
                "install",
                "operator",
                "--image",
                imageName,
                "--tag",
                imageTag,
                "-e", "yaml");

            final String resources = IOUtils.toString(p.getInputStream(), StandardCharsets.UTF_8);
            log.debug("Resources generated from the operator image");
            log.debug(resources);
            resourceList = OpenShiftUtils.getInstance().load(IOUtils.toInputStream(resources, StandardCharsets.UTF_8)).get();
            p.waitFor();
        } catch (Exception e) {
            log.error("Could not load resources from operator image, check debug logs", e);
            fail("Failed to install using operator");
        }

        return resourceList;
    }

    public void deployOperator() {
        List<HasMetadata> resourceList = getOperatorResources();
        final String operatorResourcesName = "syndesis-operator";
        Optional<HasMetadata> serviceAccount = resourceList.stream()
            .filter(resource -> "ServiceAccount".equals(resource.getKind()) && operatorResourcesName.equals(resource.getMetadata().getName()))
            .findFirst();

        if (serviceAccount.isPresent()) {
            ((ServiceAccount) serviceAccount.get())
                .getImagePullSecrets().add(new LocalObjectReference(TestConfiguration.syndesisPullSecretName()));
        } else {
            log.error("Service account not found in resources");
        }

        DeploymentConfig dc = (DeploymentConfig) resourceList.stream()
            .filter(r -> "DeploymentConfig".equals(r.getKind()) && operatorResourcesName.equals(r.getMetadata().getName()))
            .findFirst().orElseThrow(() -> new RuntimeException("Unable to find deployment config in operator resources"));

        List<EnvVar> envVarsToAdd = new ArrayList<>();
        envVarsToAdd.add(new EnvVar("TEST_SUPPORT", "true", null));

        dc.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv().addAll(envVarsToAdd);

        List<HasMetadata> finalResourceList = resourceList;
        OpenShiftUtils.asRegularUser(() -> OpenShiftUtils.getInstance().resourceList(finalResourceList).createOrReplace());

        Map<String, String> imagesEnvVars = null;
        // For upgrade, we want to override images only for "current" version
        if (operatorImage.equals(TestConfiguration.syndesisOperatorImage())) {
            imagesEnvVars = generateImageEnvVars();
        }

        if (imagesEnvVars != null && !imagesEnvVars.isEmpty()) {
            log.info("Overriding images to be deployed");
            try {
                OpenShiftWaitUtils.waitFor(() -> OpenShiftUtils.getInstance().getDeploymentConfig(operatorResourcesName) != null);
            } catch (TimeoutException | InterruptedException e) {
                fail("Unable to get operator deployment config", e);
            }

            OpenShiftUtils.getInstance().scale(operatorResourcesName, 0);

            try {
                OpenShiftWaitUtils.waitFor(OpenShiftWaitUtils.areNoPodsPresent(operatorResourcesName));
            } catch (TimeoutException | InterruptedException e) {
                fail("Operator pod shouldn't be present after scaling down", e);
            }

            OpenShiftUtils.getInstance().updateDeploymentConfigEnvVars(operatorResourcesName, imagesEnvVars);
            try {
                OpenShiftUtils.getInstance().scale(operatorResourcesName, 1);
            } catch (KubernetesClientException ex) {
                // retry one more time after a slight delay
                log.warn("Caught KubernetesClientException: " + ex);
                log.warn("Will retry in 30 seconds");
                TestUtils.sleepIgnoreInterrupt(30000L);
                OpenShiftUtils.getInstance().scale(operatorResourcesName, 1);
            }
        }

        log.info("Waiting for syndesis-operator to be ready");
        OpenShiftUtils.getInstance().waiters()
            .areExactlyNPodsReady(1, "syndesis.io/component", operatorResourcesName)
            .interval(TimeUnit.SECONDS, 20)
            .timeout(TimeUnit.MINUTES, 10)
            .waitFor();
    }

    public Map<String, String> generateImageEnvVars() {
        Map<String, String> imagesEnvVars = new HashMap<>();
        Set<Image> images = EnumSet.allOf(Image.class);
        for (Image image : images) {
            if (TestConfiguration.image(image) != null) {
                log.info("Will override " + image.name().toLowerCase() + " image with " + TestConfiguration.image(image));
                imagesEnvVars.put("RELATED_IMAGE_" + image.name(), TestConfiguration.image(image));
            }
        }

        return imagesEnvVars;
    }

    protected void deploySyndesisViaOperator() {
        log.info("Deploying syndesis resource from " + crUrl);
        try (InputStream is = new URL(crUrl).openStream()) {
            JSONObject crJson = new JSONObject(getSyndesisCrClient().load(is));

            JSONObject serverFeatures = crJson.getJSONObject("spec").getJSONObject("components")
                .getJSONObject("server").getJSONObject("features");
            if (TestUtils.isJenkins()) {
                serverFeatures.put("integrationStateCheckInterval", TestConfiguration.stateCheckInterval());
            }
            serverFeatures.put("integrationLimit", 10);
            crJson.getJSONObject("spec").getJSONObject("addons").getJSONObject("todo").put("enabled", true);
            // add nexus
            addMavenRepo(serverFeatures);

            // set correct image stream namespace
            crJson.getJSONObject("spec").put("imageStreamNamespace", TestConfiguration.openShiftNamespace());

            // set the route
            crJson.getJSONObject("spec").put("routeHostname", StringUtils.substringAfter(TestConfiguration.syndesisUrl(), "https://"));

            createCr(crJson.toMap());
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to load operator syndesis template", ex);
        }
    }

    protected void addMavenRepo(JSONObject serverFeatures) {
        String replacementRepo = null;
        if (TestUtils.isProdBuild()) {
            if (TestConfiguration.prodRepository() != null) {
                replacementRepo = TestConfiguration.prodRepository();
            } else {
                fail("Trying to deploy prod version using operator and system property " + TestConfiguration.PROD_REPOSITORY + " is not set!");
            }
        } else {
            if (TestConfiguration.upstreamRepository() != null) {
                replacementRepo = TestConfiguration.upstreamRepository();
            } else {
                // no replacement, will use maven central
                log.warn("No repo to add, skipping");
                return;
            }
        }

        Map<String, Object> mavenConfiguration = new HashMap<>();
        if (TestConfiguration.appendRepository()) {
            log.info("Appending maven repo {}", replacementRepo);
            mavenConfiguration.put("append", true);
            mavenConfiguration.put("repositories", TestUtils.map(
                "central", "https://repo.maven.apache.org/maven2/",
                "repo-02-redhat-ga", "https://maven.repository.redhat.com/ga/",
                "repo-03-jboss-ea", "https://repository.jboss.org/nexus/content/groups/ea/",
                "qe-repo", replacementRepo
                )
            );
        } else {
            log.info("Adding maven repo {}", replacementRepo);
            mavenConfiguration.put("append", false);
            mavenConfiguration.put("repositories", TestUtils.map(
                "qe-repo", replacementRepo
                )
            );
        }

        serverFeatures.put("maven", mavenConfiguration);
    }

    /**
     * Checks if the given addon is enabled in the CR.
     *
     * @param addon addon to check
     * @return true/false
     */
    public boolean isAddonEnabled(Addon addon) {
        try {
            JSONObject spec = new JSONObject(getCr()).getJSONObject("spec");

            // Special case for external DB
            if (addon == Addon.EXTERNAL_DB) {
                return spec.getJSONObject("components").getJSONObject(addon.getValue()).has("externalDbURL");
            } else {
                return spec.getJSONObject("addons").getJSONObject(addon.getValue()).getBoolean("enabled");
            }
        } catch (KubernetesClientException kce) {
            if (!kce.getMessage().contains("404")) {
                // If the error is something different than the CR wasn't found rethrow the exception
                throw kce;
            }
            return false;
        } catch (JSONException e) {
            // ignore exception as some of the object wasn't present
            return false;
        }
    }

    /**
     * Test whether addon contains specific property
     */
    public boolean containsAddonProperty(Addon addon, String key) {
        return new JSONObject(getCr()).getJSONObject("spec").getJSONObject("addons").getJSONObject(addon.getValue()).has(key);
    }

    public void updateAddon(Addon addon, boolean enabled) {
        updateAddon(addon, enabled, null);
    }

    /**
     * Enable or disable the addon
     *
     * @param addon - which type of addon
     * @param enabled - enable or disable?
     * @param properties - additional properties for the specific addon
     */
    public void updateAddon(Addon addon, boolean enabled, Map<String, Object> properties) {
        log.info((enabled ? "Enabling " : "Disabling ") + addon + " addon.");
        JSONObject cr = new JSONObject(getCr());
        JSONObject specAddon = cr.getJSONObject("spec").getJSONObject("addons").getJSONObject(addon.getValue());
        specAddon.put("enabled", enabled);
        if (properties != null) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                log.info("Adding property '" + entry.getKey() + ": " + entry.getValue() + "' for addon " + addon.getValue() + " to the CR");
                specAddon.put(entry.getKey(), entry.getValue());
            }
        }
        try {
            this.editCr(cr.toMap());
        } catch (KubernetesClientException kce) {
            if (kce.getMessage().contains("the object has been modified")) {
                log.warn("CR was modified in the mean time, retrying in 30 seconds");
                TestUtils.sleepIgnoreInterrupt(30000L);
                updateAddon(addon, enabled, properties);
            } else {
                throw kce;
            }
        }
    }

    public void changeRuntime(String runtime) {
        boolean needsReload = false;
        if ("camelk".equalsIgnoreCase(runtime)) {
            if (!ResourceFactory.get(CamelK.class).isReady()) {
                CommonSteps.deployCamelK();
                CommonSteps.waitForCamelK();
            }
            Syndesis syndesis = ResourceFactory.get(Syndesis.class);
            if (!syndesis.isAddonEnabled(Addon.CAMELK)) {
                syndesis.updateAddon(Addon.CAMELK, true);
                needsReload = true;
            }
        } else {
            if (ResourceFactory.get(CamelK.class).isReady()) {
                ResourceFactory.get(CamelK.class).undeploy();
            }

            Syndesis syndesis = ResourceFactory.get(Syndesis.class);
            if (syndesis.isAddonEnabled(Addon.CAMELK)) {
                syndesis.updateAddon(Addon.CAMELK, false);
                needsReload = true;
            }
        }
        if (needsReload) {
            log.info("Waiting for syndesis-server to reload");
            try {
                OpenShiftWaitUtils.waitForPodIsReloaded("server");
            } catch (InterruptedException | TimeoutException e) {
                InfraFail.fail("Server was not reloaded after deployment config change", e);
            }
            // even though server is in ready state, inside app is still starting so we have to wait a lot just to be sure
            try {
                OpenShiftWaitUtils.waitFor(() -> OpenShiftUtils.getPodLogs("server").contains("Started Application in"), 1000 * 300L);
            } catch (TimeoutException | InterruptedException e) {
                InfraFail.fail("Syndesis server did not start in 300s with new variable", e);
            }
            RestUtils.reset();
        }
    }

    /**
     * Gets the API version from the CR.
     *
     * @return api version string
     */
    private String getCrApiVersion() {
        if (crApiVersion == null) {
            try (InputStream is = new URL(crUrl).openStream()) {
                crApiVersion = StringUtils.substringAfter(((Map<String, String>) new Yaml().load(is)).get("apiVersion"), "/");
            } catch (IOException e) {
                fail("Unable to read syndesis CR", e);
            }
        }
        return crApiVersion;
    }

    private void ensureImagePullForJaegerOperator() {
        //if jaeger operator is used
        OpenShiftUtils.getAnyPod("name", "jaeger-operator").ifPresent(operatorPod -> {
            ensureImagePull("jaeger-operator", "jaeger");
        });
    }

    private void ensureImagePullForSyndesisJaeger() {
        //if syndesis-jaeger is used
        OpenShiftUtils.getAnyPod("app.kubernetes.io/name", "syndesis-jaeger").ifPresent(syndesisJaegerPod -> {
            ensureImagePull("syndesis-jaeger", "jaeger");
        });
    }

    /**
     * Productised builds need to link syndesis-pull secret and redeploy pods
     *
     * @param partialPodName
     * @param serviceAccountName
     */
    public void ensureImagePull(String partialPodName, String serviceAccountName) {
        try {
            OpenShiftWaitUtils.waitFor(() -> OpenShiftUtils.hasPodIssuesPullingImage(OpenShiftUtils.getPodByPartialName(partialPodName).get()) ||
                OpenShiftUtils.getPodByPartialName(partialPodName).filter(OpenShiftWaitUtils::isPodRunning).isPresent(), 10 * 60 * 1000);
        } catch (TimeoutException | InterruptedException e) {
            InfraFail.fail("Pod " + partialPodName +
                " is not in the one of the desired state (Running,ImagePullBackOff,ErrImagePull)! Check the log for more details.");
        }
        Pod podAfterWait = OpenShiftUtils.getPodByPartialName(partialPodName).get(); //needs to get new instance of the pod
        if (OpenShiftUtils.hasPodIssuesPullingImage(podAfterWait)) {
            log.info(
                "{} failed to pull image (probably due to permission to the Red Hat registry), linking secret with the SA and restarting the pod",
                podAfterWait.getMetadata().getName());
            linkServiceAccountWithSyndesisPullSecret(serviceAccountName);
            OpenShiftUtils.getInstance().deletePod(podAfterWait);
            OpenShiftWaitUtils.waitUntilPodIsRunning(partialPodName);
        }
    }

    public void linkServiceAccountWithSyndesisPullSecret(String serviceAccountName) {
        //create secret for red hat registry
        OpenShiftUtils.getInstance().serviceAccounts().list().getItems().stream()
            .filter(sa -> sa.getMetadata().getName().contains(serviceAccountName))
            .forEach(sa -> {
                sa.getImagePullSecrets().add(new LocalObjectReference(TestConfiguration.syndesisPullSecretName()));
                OpenShiftUtils.getInstance().serviceAccounts().createOrReplace(sa);
            });
    }
}
