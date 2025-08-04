package com.marmouset;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
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
import com.marmouset.visitor.EnvVarsVisitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

    var deploymentLabels = Map.of("com.appdynamics/appname", "TestApp");
    var deploymentAnnotations = Map.of("com.appdynamics/inject-java", "true");
    deployment.setMetadata(
        new ObjectMetaBuilder()
            .withName(DEPLOYMENT_NAME)
            .withLabels(deploymentLabels)
            .withAnnotations(deploymentAnnotations)
            .build());

    deployment = extension.replace(deployment);

    await().untilAsserted(this::assertInitContainerIsSet);
    await().untilAsserted(this::configMapIsSet);
    await().untilAsserted(this::envVarsAreSet);
  }

  private void configMapIsSet() {
    var dep = extension.get(Deployment.class, DEPLOYMENT_NAME);
    assertThat(dep).isNotNull();
    var containers = dep.getSpec().getTemplate().getSpec().getContainers();
    assertThat(containers).hasSize(1);
    var container = containers.get(0);
    var envs = container.getEnvFrom();
    assertThat(envs).hasSize(1);
    assertThat(envs).contains(
        new EnvFromSourceBuilder()
            .withNewConfigMapRef()
            .withName("appdynamics-config-map")
            .endConfigMapRef()
            .build());
  }

  private void envVarsAreSet() {
    var dep = extension.get(Deployment.class, DEPLOYMENT_NAME);
    assertThat(dep).isNotNull();
    var containers = dep.getSpec().getTemplate().getSpec().getContainers();
    assertThat(containers).hasSize(1);
    var container = containers.get(0);
    var envs = container.getEnv();
    assertThat(envs).hasSize(2);
    assertThat(envs).contains(
        new EnvVarBuilder().withName(EnvVarsVisitor.APPDYNAMICS_AGENT_APPLICATION_NAME).withValue("TestApp").build(),
        new EnvVarBuilder().withName(EnvVarsVisitor.APPDYNAMICS_AGENT_TIER_NAME).withValue("default").build());
  }

  private void assertInitContainerIsSet() {
    var dep = extension.get(Deployment.class, DEPLOYMENT_NAME);
    assertThat(dep).isNotNull();
    var initContainers = dep.getSpec().getTemplate().getSpec().getInitContainers();
    assertThat(initContainers).hasSize(1);
    var initContainer = initContainers.get(0);
    assertThat(initContainer.getName()).isEqualTo("attach-appdynamics-java-agent");
    assertThat(initContainer.getImage()).isEqualTo("test/java-agent");
    assertThat(initContainer.getCommand()).isEqualTo(List.of("test"));
  }

  private Namespace createDefaultNamespace() {
    return new NamespaceBuilder()
        .withNewMetadata()
        .withName("default")
        .endMetadata()
        .build();
  }

  private Deployment createDeploymentWithoutLabels(Namespace namespace) {
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

  private AppdynamicsOperatorCustomResource createAppDynOperatorCRD() {
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
        .withNamespace("default")
        .build());
    resource.setSpec(new AppdynamicsOperatorSpec());
    resource.getSpec().setController(controllerSpec);
    resource.getSpec().setAgent(agentSpec);

    return resource;
  }
}
