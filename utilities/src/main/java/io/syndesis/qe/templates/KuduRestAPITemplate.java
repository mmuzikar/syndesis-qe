package io.syndesis.qe.templates;

import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.syndesis.qe.utils.OpenShiftUtils;
import io.syndesis.qe.utils.TestUtils;
import io.syndesis.qe.wait.OpenShiftWaitUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.fail;

@Slf4j
public class KuduRestAPITemplate {
    private static final String APP_NAME = "kudu-rest-api";
    private static final String LABEL_NAME = "app";

    public static void deploy() {
        if (!TestUtils.isDcDeployed(APP_NAME)) {
            List<ContainerPort> ports = new LinkedList<>();
            ports.add(new ContainerPortBuilder()
                    .withName("kudu-rest-api")
                    .withContainerPort(8080)
                    .build());

            OpenShiftUtils.client().deploymentConfigs().createOrReplaceWithNew()
                    .editOrNewMetadata()
                    .withName(APP_NAME)
                    .addToLabels(LABEL_NAME, APP_NAME)
                    .endMetadata()

                    .editOrNewSpec()
                    .addToSelector(LABEL_NAME, APP_NAME)
                    .withReplicas(1)
                    .editOrNewTemplate()
                    .editOrNewMetadata()
                    .addToLabels(LABEL_NAME, APP_NAME)
                    .endMetadata()
                    .editOrNewSpec()
                    .addNewContainer().withName(APP_NAME).withImage("mcada/syndesis-kudu-rest-api:latest").addAllToPorts(ports)

                    .endContainer()
                    .endSpec()
                    .endTemplate()
                    .addNewTrigger()
                    .withType("ConfigChange")
                    .endTrigger()
                    .endSpec()
                    .done();

            ServiceSpecBuilder serviceSpecBuilder = new ServiceSpecBuilder().addToSelector(LABEL_NAME, APP_NAME);

            serviceSpecBuilder.addToPorts(new ServicePortBuilder()
                    .withName("kudu-rest-api-service")
                    .withPort(8080)
                    .withTargetPort(new IntOrString(8080))
                    .build());

            OpenShiftUtils.getInstance().client().services().createOrReplaceWithNew()
                    .editOrNewMetadata()
                    .withName(APP_NAME)
                    .addToLabels(LABEL_NAME, APP_NAME)
                    .endMetadata()
                    .editOrNewSpecLike(serviceSpecBuilder.build())
                    .endSpec()
                    .done();

            final Route route = new RouteBuilder()
                    .withNewMetadata()
                    .withName(APP_NAME)
                    .endMetadata()
                    .withNewSpec()
                    .withPath("/")
                    .withWildcardPolicy("None")
                    .withNewTls()
                    .withTermination("edge")
                    .withInsecureEdgeTerminationPolicy("Allow")
                    .endTls()
                    .withNewTo()
                    .withKind("Service").withName(APP_NAME)
                    .endTo()
                    .endSpec()
                    .build();

            log.info("Creating route {} with path {}", APP_NAME, "/");
            OpenShiftUtils.client().routes().createOrReplace(route);

            try {
                OpenShiftWaitUtils.waitFor(OpenShiftWaitUtils.areExactlyNPodsReady(LABEL_NAME, APP_NAME, 1), 15 * 60 * 1000L);
                log.info("Checking pod logs if the app is ready");
                OpenShiftWaitUtils.waitFor(() -> OpenShiftUtils.getPodLogs(APP_NAME).contains("Started App in"), 5 * 60 * 1000L);
            } catch (InterruptedException | TimeoutException e) {
                fail("Wait for " + APP_NAME + " deployment failed ", e);
            }
        }
    }

    public static void cleanUp() {
        OpenShiftUtils.getInstance().getDeploymentConfigs().stream().filter(dc -> dc.getMetadata().getName().equals(APP_NAME)).findFirst()
                .ifPresent(dc -> OpenShiftUtils.getInstance().deleteDeploymentConfig(dc, true));
        OpenShiftUtils.getInstance().getServices().stream().filter(service -> APP_NAME.equals(service.getMetadata().getName())).findFirst()
                .ifPresent(service -> OpenShiftUtils.getInstance().deleteService(service));
        OpenShiftUtils.getInstance().getRoutes().stream().filter(route -> APP_NAME.equals(route.getMetadata().getName())).findFirst()
                .ifPresent(route -> OpenShiftUtils.getInstance().deleteRoute(route));
    }
}
