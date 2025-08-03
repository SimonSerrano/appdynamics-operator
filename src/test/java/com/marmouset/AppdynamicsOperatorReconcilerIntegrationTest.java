package com.marmouset;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;
import io.netty.handler.codec.http.HttpHeaders.Names;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.marmouset.reconciler.AppdynamicsOperatorReconciler;
import com.marmouset.spec.AppDynamicsAgentSpec;
import com.marmouset.spec.AppDynamicsControllerSpec;
import com.marmouset.spec.AppdynamicsOperatorSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.Map;

class AppdynamicsOperatorReconcilerIntegrationTest {

  public static final String DEPLOYMENT_NAME = "deployment-test";
  public static final String OPERATOR_CRD_NAME = "crd-test";
  public static final String INITIAL_VALUE = "access key";
  public static final String CHANGED_VALUE = "new access key";

  @RegisterExtension
  LocallyRunOperatorExtension extension = LocallyRunOperatorExtension.builder()
      .withReconciler(AppdynamicsOperatorReconciler.class)
      .build();

  @Test
  void shouldCreateInitContainerAndEnvVarsMapping() {
    extension.create(createAppDynOperatorCRD());
    var ns = createDefaultNamespace();
    var deployment = extension.create(createDeploymentWithoutLabels(ns));

    await().untilAsserted(() -> {
      var dep = extension.get(Deployment.class, DEPLOYMENT_NAME);
      assertThat(dep).isNotNull();
      assertThat(dep.getMetadata().getLabels()).isEmpty();
    });

    var deploymentAnnotations = Map.of("com.appdynamics/inject-java", "true", "com.appdynamics/appname", "TestApp");
    deployment.setMetadata(
        new ObjectMetaBuilder()
            .withName(DEPLOYMENT_NAME)
            .withAnnotations(deploymentAnnotations)
            .build());

    deployment = extension.replace(deployment);

    await().untilAsserted(() -> {
      var dep = extension.get(Deployment.class, DEPLOYMENT_NAME);
      assertThat(dep).isNotNull();
      assertThat(dep.getMetadata().getLabels()).containsAllEntriesOf(deploymentAnnotations);
      var initContainers = dep.getSpec().getTemplate().getSpec().getInitContainers();
      assertThat(initContainers).hasSize(1);
    });
  }

  Namespace createDefaultNamespace() {
    return new NamespaceBuilder()
        .withNewMetadata()
        .withName("default")
        .endMetadata()
        .build();
  }

  Deployment createDeploymentWithoutLabels(Namespace namespace) {
    var deployment = new DeploymentBuilder();
    deployment.withMetadata(
        new ObjectMetaBuilder()
            .withName(DEPLOYMENT_NAME)
            .withNamespace(namespace.getMetadata().getName())
            .build())
        .withSpec(
            new DeploymentSpecBuilder()
                .withSelector(
                    new LabelSelectorBuilder()
                        .withMatchLabels(Map.of("app", "java-app"))
                        .build())
                .withTemplate(
                    new PodTemplateSpecBuilder()
                        .withMetadata(
                            new ObjectMetaBuilder()
                                .withLabels(Map.of("app", "java-app"))
                                .build())
                        .withSpec(
                            new PodSpecBuilder()
                                .withContainers(
                                    new ContainerBuilder()
                                        .withName("sample-app-rest")
                                        .withImage("appdynamics/sample-app-rest:latest")
                                        .build())
                                .build())
                        .build())
                .build());

    return deployment.build();
  }

  AppdynamicsOperatorCustomResource createAppDynOperatorCRD() {
    var controllerSpec = new AppDynamicsControllerSpec();
    controllerSpec.setHost("my-controller.com");
    controllerSpec.setPort(8080);
    controllerSpec.setSsl(true);
    var agentSpec = new AppDynamicsAgentSpec();
    agentSpec.setAccountAccessKey(INITIAL_VALUE);
    agentSpec.setAccountName("account");
    agentSpec.setInitContainerCommand("test");
    agentSpec.setJavaAgentImage("test/java-agent");
    var resource = new AppdynamicsOperatorCustomResource();
    resource.setMetadata(new ObjectMetaBuilder()
        .withName(OPERATOR_CRD_NAME)
        .build());
    resource.setSpec(new AppdynamicsOperatorSpec());
    resource.getSpec().setController(controllerSpec);
    resource.getSpec().setAgent(agentSpec);

    return resource;
  }
}
