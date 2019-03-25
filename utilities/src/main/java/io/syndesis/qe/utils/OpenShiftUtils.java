package io.syndesis.qe.utils;

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Java6Assertions.assertThat;

import java.util.Optional;

import cz.xtf.openshift.OpenShiftBinaryClient;
import cz.xtf.openshift.OpenShiftUtil;
import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftConfigBuilder;
import io.syndesis.qe.Component;
import io.syndesis.qe.TestConfiguration;
import io.syndesis.qe.wait.OpenShiftWaitUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;

/**
 * OpenShift utils.
 * <p>
 * Sep 8, 2017 Red Hat
 *
 * @author tplevko@redhat.com
 */
@Slf4j
public final class OpenShiftUtils {

    private static OpenShiftUtil xtfUtils = null;

    public static OpenShiftUtil getInstance() {
        if (xtfUtils == null) {
            new OpenShiftUtils();
        }
        return xtfUtils;
    }

    public static OpenShiftUtil xtf() {
        return getInstance();
    }

    private OpenShiftUtils() {
        if (xtfUtils == null) {
            final OpenShiftConfigBuilder openShiftConfigBuilder = new OpenShiftConfigBuilder()
                    .withMasterUrl(TestConfiguration.openShiftUrl())
                    .withTrustCerts(true)
                    .withRequestTimeout(120_000)
                    .withNamespace(TestConfiguration.openShiftNamespace());
            if (!TestConfiguration.openShiftToken().isEmpty()) {
                //if token is provided, lets use it
                //otherwise f8 client should be able to leverage ~/.kube/config or mounted secrets
                openShiftConfigBuilder.withOauthToken(TestConfiguration.openShiftToken());
            }
            xtfUtils = new OpenShiftUtil(openShiftConfigBuilder.build());
        }
    }

    public static NamespacedOpenShiftClient client() {
        return getInstance().client();
    }

    public static Route createRestRoute(String openShiftNamespace, String urlSuffix) {
        final Route route = new RouteBuilder()
                .withNewMetadata()
                .withName(Component.SERVER.getName())
                .endMetadata()
                .withNewSpec()
                .withPath("/api").withHost("rest-" + openShiftNamespace + "." + urlSuffix)
                .withWildcardPolicy("None")
                .withNewTls()
                .withTermination("edge")
                .endTls()
                .withNewTo()
                .withKind("Service").withName(Component.SERVER.getName())
                .endTo()
                .endSpec()
                .build();
        return client().resource(route).createOrReplace();
    }

    public static LocalPortForward portForward(Pod pod, int remotePort, int localPort) {
        return getPodResource(pod).portForward(remotePort, localPort);
    }

    private static PodResource<Pod, DoneablePod> getPodResource(Pod pod) {
        if (pod.getMetadata().getNamespace() != null) {
            return client().pods().inNamespace(pod.getMetadata().getNamespace()).withName(pod.getMetadata().getName());
        } else {
            return client().pods().withName(pod.getMetadata().getName());
        }
    }

    public static Optional<Pod> getPodByPartialName(String partialName) {
        return OpenShiftUtils.getInstance().getPods().stream()
                .filter(p -> p.getMetadata().getName().contains(partialName))
                .filter(p -> !p.getMetadata().getName().contains("deploy"))
                .filter(p -> !p.getMetadata().getName().contains("build"))
                .findFirst();
    }

    public static int extractPodSequenceNr(Pod pod) {
        String podFullName = pod.getMetadata().getName();
        String[] pole = podFullName.split("-");
        return Integer.parseInt(pole[2]);
    }

    public static String getIntegrationLogs(String integrationName) {
        return getPodLogs(integrationName.replaceAll("[\\s_]", "-").toLowerCase());
    }

    public static String getPodLogs(String podPartialName) {
        // pod has to be in running state because pod in ContainerCreating state causes exception
        OpenShiftWaitUtils.waitUntilPodIsRunning(podPartialName);
        Optional<Pod> integrationPod = getPodByPartialName(podPartialName);
        if (integrationPod.isPresent()) {
            String logText = OpenShiftUtils.getInstance().getPodLog(integrationPod.get());
            assertThat(logText)
                    .isNotEmpty();
            return logText;
        } else {
            fail("No pod found for pod name: " + podPartialName);
        }
        //this can not happen due to assert
        return null;
    }

    public static boolean arePodLogsEmpty(String podPartialName) {
        // pod has to be in running state because pod in ContainerCreating state causes exception
        OpenShiftWaitUtils.waitUntilPodIsRunning(podPartialName);

        Optional<Pod> integrationPod = getPodByPartialName(podPartialName);
        return integrationPod.map(pod -> OpenShiftUtils.getInstance().getPodLog(pod).isEmpty()).orElse(true);
    }

    /**
     * Invoke openshift's API. Only part behind master url is necessary and the path must start with slash.
     * @param method HTTP method to use
     * @param url api path
     * @param body body to send as JSON
     * @return response object
     */
    public static HTTPResponse invokeApi(HttpUtils.Method method, String url, String body) {
        return invokeApi(method, url, body, null);
    }

    /**
     * Invoke openshift's API. Only part behind master url is necessary and the path must start with slash.
     * @param method HTTP method to use
     * @param url api path
     * @param body body to send as JSON
     * @param headers headers to send, can be null
     * @return response object
     */
    public static HTTPResponse invokeApi(HttpUtils.Method method, String url, String body, Headers headers) {
        url = TestConfiguration.openShiftUrl() + url;
        if (headers == null) {
            headers = Headers.of("Authorization", "Bearer " + OpenShiftUtils.client().getConfiguration().getOauthToken());
        } else {
            if (headers.get("Authorization") == null) {
                headers = headers.newBuilder().add("Authorization", "Bearer " + OpenShiftUtils.client().getConfiguration().getOauthToken()).build();
            }
        }

        log.debug(url);
        HTTPResponse response = null;
        switch (method) {
            case GET: {
                response = HttpUtils.doGetRequest(url, headers);
                break;
            }
            case POST: {
                response = HttpUtils.doPostRequest(url, body, headers);
                break;
            }
            case PUT: {
                response = HttpUtils.doPutRequest(url, body, headers);
                break;
            }
            case DELETE: {
                response = HttpUtils.doDeleteRequest(url, headers);
                break;
            }
            default: {
                fail("Unable to use specified HTTP Method!");
            }
        }

        return response;
    }

    /**
     * Creates the resource using binary oc client.
     * @param resource path to resource file to use with -f
     */
    public static void create(String resource) {
        final String output = OpenShiftBinaryClient.getInstance().executeCommandWithReturn(
                "Unable to create resource " + resource,
                "apply",
                "--overwrite=false",
                "-n", TestConfiguration.openShiftNamespace(),
                "-f", resource
        );
        log.info(output);
    }
}
