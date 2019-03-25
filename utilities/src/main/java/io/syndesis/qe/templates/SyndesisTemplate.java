package io.syndesis.qe.templates;

import static org.assertj.core.api.Fail.fail;

import org.apache.commons.codec.binary.Base64;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import cz.xtf.openshift.OpenShiftBinaryClient;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.ImageStreamList;
import io.fabric8.openshift.api.model.TagImportPolicy;
import io.fabric8.openshift.api.model.Template;
import io.syndesis.qe.Component;
import io.syndesis.qe.TestConfiguration;
import io.syndesis.qe.utils.HTTPResponse;
import io.syndesis.qe.utils.HttpUtils;
import io.syndesis.qe.utils.OpenShiftUtils;
import io.syndesis.qe.utils.TestUtils;
import io.syndesis.qe.utils.TodoUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SyndesisTemplate {
    public static Template getTemplate() {
        try (InputStream is = new URL(TestConfiguration.syndesisTemplateUrl()).openStream()) {
            return OpenShiftUtils.client().templates().load(is).get();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read template ", ex);
        }
    }

    public static ServiceAccount getSupportSA() {
        // Refresh the support SA URL as it can change during multiple tests executions
        try (InputStream is = new URL(TestConfiguration.syndesisTemplateSA()).openStream()) {
            return OpenShiftUtils.client().serviceAccounts().load(is).get();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read SA ", ex);
        }
    }

    public static void deploy() {
        if (TestConfiguration.useOperator()) {
            deployUsingOperator();
        } else {
            deployUsingTemplate();
        }
    }

    public static void deployUsingTemplate() {
        log.info("Deploying using template");
        OpenShiftUtils.getInstance().cleanAndAssert();

        // get & create restricted SA
        OpenShiftUtils.getInstance().createServiceAccount(getSupportSA());
        // get token from SA `oc secrets get-token` && wait until created to prevent 404
        TestUtils.waitForEvent(Optional::isPresent,
                () -> OpenShiftUtils.getInstance().getSecrets().stream().filter(s -> s.getMetadata().getName().startsWith("syndesis-oauth-client-token")).findFirst(),
                TimeUnit.MINUTES,
                2,
                TimeUnit.SECONDS,
                5);

        Secret secret = OpenShiftUtils.getInstance().getSecrets().stream().filter(s -> s.getMetadata().getName().startsWith("syndesis-oauth-client-token")).findFirst().get();
        // token is Base64 encoded by default
        String oauthTokenEncoded = secret.getData().get("token");
        byte[] oauthTokenBytes = Base64.decodeBase64(oauthTokenEncoded);
        String oauthToken = new String(oauthTokenBytes);

        // get the template
        Template template = getTemplate();
        // set params
        Map<String, String> templateParams = new HashMap<>();
        templateParams.put("ROUTE_HOSTNAME", TestConfiguration.openShiftNamespace() + "." + TestConfiguration.openShiftRouteSuffix());
        templateParams.put("OPENSHIFT_MASTER", TestConfiguration.openShiftUrl());
        templateParams.put("OPENSHIFT_PROJECT", TestConfiguration.openShiftNamespace());
        templateParams.put("SAR_PROJECT", TestConfiguration.openShiftSARNamespace());
        templateParams.put("OPENSHIFT_OAUTH_CLIENT_SECRET", oauthToken);
        templateParams.put("TEST_SUPPORT_ENABLED", "true");
        templateParams.put("MAX_INTEGRATIONS_PER_USER", "5");
        if (TestUtils.isJenkins()) {
            templateParams.put("INTEGRATION_STATE_CHECK_INTERVAL", "150");
        }
        // process & create
        KubernetesList processedTemplate = OpenShiftUtils.getInstance().recreateAndProcessTemplate(template, templateParams);
        for (HasMetadata hasMetadata : processedTemplate.getItems()) {
            OpenShiftUtils.getInstance().createResources(hasMetadata);
        }

        //TODO: there's a bug in openshift-client, we need to initialize manually
        OpenShiftUtils.client().roleBindings().createOrReplaceWithNew()
                .withNewMetadata()
                    .withName("syndesis:editors")
                .endMetadata()
                .withNewRoleRef().withName("edit").endRoleRef()
                .addNewSubject().withKind("ServiceAccount").withName(Component.SERVER.getName()).withNamespace(TestConfiguration.openShiftNamespace()).endSubject()
                .addToUserNames(String.format("system:serviceaccount:%s:%s", TestConfiguration.openShiftNamespace(), Component.SERVER.getName()))
                .done();
        patchImageStreams();
        importProdImages();
    }

    private static void deployUsingOperator() {
        log.info("Deploying using Operator");
        if (!TestUtils.isUserAdmin()) {
            StringBuilder sb = new StringBuilder("\n");
            sb.append("****************************************************\n");
            sb.append("* Operator deployment needs user with admin rights *\n");
            sb.append("****************************************************\n");
            sb.append("If you are using minishift, you can use \"oc adm policy --as system:admin add-cluster-role-to-user cluster-admin developer\"\n");
            sb.append("Or use syndesis.config.template.use.operator=false\n");
            log.error(sb.toString());
            throw new RuntimeException(sb.toString());
        }

        OpenShiftUtils.getInstance().cleanAndAssert();
        deployCrd();
        deployOperator();
        importProdImages();
        deploySyndesisViaOperator();
        fixMavenRepos();
        patchImageStreams();
        // Prod template does have broker-amq deployment config defined for some reason, so delete it
        OpenShiftUtils.client().deploymentConfigs().withName("broker-amq").delete();
        TodoUtils.createDefaultRouteForTodo("todo2", "/");
    }

    private static void deployCrd() {
        log.info("Creating custom resource definition from " + TestConfiguration.syndesisOperatorCrdUrl());
        try (InputStream is = new URL(TestConfiguration.syndesisOperatorCrdUrl()).openStream()) {
            CustomResourceDefinition crd = OpenShiftUtils.client().customResourceDefinitions().load(is).get();
            OpenShiftUtils.client().customResourceDefinitions().create(crd);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to load CRD", ex);
        } catch (KubernetesClientException kce) {
            if (!kce.getMessage().contains("already exists")) {
                throw kce;
            }
        }
    }

    private static void deployOperator() {
        log.info("Deploying operator from " + TestConfiguration.syndesisOperatorUrl());
        final String output = OpenShiftBinaryClient.getInstance().executeCommandWithReturn(
                "Unable to process operator resource " + TestConfiguration.syndesisOperatorUrl(),
                "create",
                "-n", TestConfiguration.openShiftNamespace(),
                "-f", TestConfiguration.syndesisOperatorUrl()
        );
        log.debug(output);

        importProdImage("operator");

        log.info("Waiting for syndesis-operator to be ready");
        OpenShiftUtils.xtf().waiters()
                .areExactlyNPodsReady(1, "syndesis.io/component", "syndesis-operator")
                .interval(TimeUnit.SECONDS, 20)
                .timeout(TimeUnit.MINUTES, 10)
                .assertEventually();
    }

    private static void deploySyndesisViaOperator() {
        log.info("Deploying syndesis resource from " + TestConfiguration.syndesisOperatorTemplateUrl());
        try (InputStream is = new URL(TestConfiguration.syndesisOperatorTemplateUrl()).openStream()) {
            CustomResourceDefinition crd = OpenShiftUtils.client().customResourceDefinitions().load(is).get();
            Map<String, Object> integration = (Map)crd.getSpec().getAdditionalProperties().get("integration");
            integration.put("limit", 5);
            if (TestUtils.isJenkins()) {
                integration.put("stateCheckInterval", 150);
            }
            crd.getSpec().getAdditionalProperties().put("testSupport", true);
            crd.getSpec().getAdditionalProperties().put("routeHostname", TestConfiguration.openShiftNamespace() + "." + TestConfiguration.openShiftRouteSuffix());
            crd.getSpec().getAdditionalProperties().put("imageStreamNamespace", TestConfiguration.openShiftNamespace());
            OpenShiftUtils.invokeApi(
                    HttpUtils.Method.POST,
                    "/apis/syndesis.io/v1alpha1/namespaces/" + TestConfiguration.openShiftNamespace() + "/" + TestConfiguration.customResourcePlural(),
                    Serialization.jsonMapper().writeValueAsString(crd)
            );
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to load operator syndesis template", ex);
        }
    }

    private static void patchImageStreams() {
        ImageStreamList isl = OpenShiftUtils.client().imageStreams().inNamespace(TestConfiguration.openShiftNamespace()).withLabel("syndesis.io/component").list();
        final int maxRetries = 120;
        int retries = 0;
        while (isl.getItems().size() < 6) {
            TestUtils.sleepIgnoreInterrupt(5000L);
            isl = OpenShiftUtils.client().imageStreams().inNamespace(TestConfiguration.openShiftNamespace()).withLabel("syndesis.io/component").list();
            retries++;
            if (retries == maxRetries) {
                fail("Unable to find image streams after " + maxRetries + " tries.");
            }
        }
        log.info("Patching imagestreams");
        isl.getItems().forEach(is -> {
            if (!is.getSpec().getTags().isEmpty()) {
                is.getSpec().getTags().get(0).setImportPolicy(new TagImportPolicy(false, false));
            }
            OpenShiftUtils.client().imageStreams().createOrReplace(is);
        });
    }

    private static void importProdImage(String imageStreamPartialName) {
        if (TestUtils.isProdBuild()) {
            int responseCode = -1;
            int retries = 0;
            while (responseCode != 201 && retries < 3) {
                if (retries != 0) {
                    TestUtils.sleepIgnoreInterrupt(15000L);
                }
                ImageStream is = OpenShiftUtils.client().imageStreams().list().getItems().stream()
                        .filter(imgStream -> imgStream.getMetadata().getName().contains(imageStreamPartialName)).findFirst().get();
                Map<String, String> metadata = new HashMap<>();
                metadata.put("name", is.getMetadata().getName());
                metadata.put("namespace", is.getMetadata().getNamespace());
                // Sometimes the resource versions do not match, therefore it is needed to refresh the value
                metadata.put("resourceVersion",
                        OpenShiftUtils.client().imageStreams().withName(is.getMetadata().getName()).get().getMetadata().getResourceVersion());

                log.info("Importing image from imagestream " + is.getMetadata().getName());
                HTTPResponse r = OpenShiftUtils.invokeApi(
                        HttpUtils.Method.POST,
                        String.format("/apis/image.openshift.io/v1/namespaces/%s/imagestreamimports", TestConfiguration.openShiftNamespace()),
                        ImageStreamImport.getJson(
                                new ImageStreamImport(is.getApiVersion(), metadata, is.getSpec().getTags().get(0).getFrom().getName(), is.getSpec().getTags().get(0).getName())
                        )
                );
                responseCode = r.getCode();
                if (responseCode != 201 && retries == 2) {
                    fail("Unable to import image for image stream " + is.getMetadata().getName() + " after 3 retries");
                }

                retries++;
            }
        }
    }

    private static void importProdImages() {
        if (TestUtils.isProdBuild()) {
            ImageStreamList isl =
                    OpenShiftUtils.client().imageStreams().inNamespace(TestConfiguration.openShiftNamespace()).withLabel("syndesis.io/component")
                            .list();

            while (isl.getItems().size() < 8) {
                TestUtils.sleepIgnoreInterrupt(5000L);
                isl = OpenShiftUtils.client().imageStreams().inNamespace(TestConfiguration.openShiftNamespace()).withLabel("syndesis.io/component")
                        .list();
            }

            isl.getItems().forEach(is -> importProdImage(is.getMetadata().getName()));
        }
    }

    private static void fixMavenRepos() {
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
                return;
            }
        }

        Optional<ConfigMap> cm = OpenShiftUtils.client().configMaps().list().getItems().stream()
                .filter(cMap -> cMap.getMetadata().getName().equals("syndesis-server-config")).findFirst();
        int retries = 0;
        final int maxRetries = TestUtils.isJenkins() ? 60 : 12;
        while (!cm.isPresent() && retries < maxRetries) {
            TestUtils.sleepIgnoreInterrupt(10000L);
            cm = OpenShiftUtils.client().configMaps().list().getItems().stream()
                    .filter(cMap -> cMap.getMetadata().getName().equals("syndesis-server-config")).findFirst();
            if (retries == (maxRetries - 1)) {
                fail("Unable to find syndesis-server-config configmap after {} tries", maxRetries);
            }
            retries++;
        }
        String data = cm.get().getData().get("application.yml");

        // ensure maven repos in config map (not there by default in upstream)
        // we should ideally parse the yaml, but this should be good for now
        if (!data.contains("maven:")) {
            String mavenRepos = "\nmaven:\n" +
                    "  repositories:\n" +
                    "    01_maven_central: https://repo1.maven.org/maven2\n" +
                    "    02_redhat_ea_repository: https://maven.repository.redhat.com/ga/\n" +
                    "    03_jboss_ea: https://repository.jboss.org/\n";
            data += mavenRepos;
        }
        data = data.replaceAll("https://repo1.maven.org/maven2", replacementRepo);

        OpenShiftUtils.client().configMaps().withName("syndesis-server-config").edit().withData(TestUtils.map("application.yml", data)).done();
    }

    /**
     * Returns \"key\":\"value\".
     * @param key key to use
     * @param value value to use
     * @return json
     */
    private static String jsonKeyValue(String key, String value) {
        if ("true".equals(value)) {
            // Don't quote the boolean value
            return "\"" + key + "\":" + value;
        } else {
            return "\"" + key + "\":\"" + value + "\"";
        }
    }

    @Data
    private static class ImageStreamImport {
        private String kind = "ImageStreamImport";
        private String apiVersion;
        private Map<String, String> metadata;
        @JsonIgnore
        private String image;
        @JsonIgnore
        private String name;

        ImageStreamImport(String apiVersion, Map metadata, String image, String name) {
            this.apiVersion = apiVersion;
            this.metadata = metadata;
            this.image = image;
            this.name = name;
        }

        public static String getJson(ImageStreamImport imageStreamImport) {
            ObjectMapper om = new ObjectMapper();
            om.setSerializationInclusion(JsonInclude.Include.USE_DEFAULTS);
            try {
                StringBuilder json = new StringBuilder("");
                StringBuilder spec = new StringBuilder("");
                String obj = om.writeValueAsString(imageStreamImport);
                json.append(obj.substring(0, obj.length() - 1));
                // Jackson can't serialize Map that is in the List that is in Map's Object value properly, therefore creating the json snippet manually here
                spec.append("\"spec\":{").append(jsonKeyValue("import", "true")).append(",")
                        .append("\"images\":[{\"from\":{").append(jsonKeyValue("kind", "DockerImage")).append(",").append(jsonKeyValue("name", imageStreamImport.getImage()))
                        .append("},").append("\"to\":{").append(jsonKeyValue("name", imageStreamImport.getName())).append("},\"importPolicy\":{").append(jsonKeyValue("insecure", "true")).append("},\"referencePolicy\":{")
                        .append(jsonKeyValue("type", "")).append("}}]},\"status\":{}}");
                json.append(",").append(spec.toString());
                return json.toString();
            } catch (JsonProcessingException e) {
                fail("Unable to process json", e);
            }
            return null;
        }
    }
}
