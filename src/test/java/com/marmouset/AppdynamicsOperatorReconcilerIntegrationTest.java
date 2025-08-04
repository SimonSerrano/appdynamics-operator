package com.marmouset;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.marmouset.reconciler.AppdynamicsOperatorReconciler;
import com.marmouset.spec.AppDynamicsAgentSpec;
import com.marmouset.spec.AppDynamicsControllerSpec;
import com.marmouset.spec.AppdynamicsOperatorSpec;
import com.marmouset.utils.Constant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.Map;

class AppdynamicsOperatorReconcilerIntegrationTest {

  private static final String ACCESS_KEY = "access key";
  private static final String ACCOUNT = "account";
  private static final String DEPLOYMENT_NAME = "deployment-test";
  private static final String OPERATOR_CRD_NAME = "crd-test";
  private static final String APP_NAME = "TestApp";
  private static final String IMAGE = "";
  private static final String COMMAND = "";
  private static final String NS = "default";
  private static final String CONTROLLER = "my-controller.com";
  private static final String CONTROLLER_PORT = "8080";

  @RegisterExtension
  LocallyRunOperatorExtension extension = LocallyRunOperatorExtension.builder()
      .withReconciler(AppdynamicsOperatorReconciler.class)
      .build();

  @Test
  void shouldInjectJavaAgentToDeploymentTemplate() {
    extension.create(createAppDynOperatorCRD());
    var ns = createDefaultNamespace();
    var deployment = extension.create(createDeploymentWithoutLabels(ns));

    await().untilAsserted(() -> {
      var dep = extension.get(Deployment.class, DEPLOYMENT_NAME);
      assertThat(dep).isNotNull();
      assertThat(dep.getMetadata().getLabels()).isEmpty();
    });

    var deploymentLabels = Map.of(Constant.K8S_DEPLOYMENT_APP_NAME, APP_NAME);
    var deploymentAnnotations = Map.of(Constant.K8S_DEPLOYMENT_INJECT_FLAG, "true");
    deployment.setMetadata(
        new ObjectMetaBuilder()
            .withName(DEPLOYMENT_NAME)
            .withLabels(deploymentLabels)
            .withAnnotations(deploymentAnnotations)
            .build());

    deployment = extension.replace(deployment);

    await().untilAsserted(this::assertInitContainerIsSet);
    await().untilAsserted(this::configMapIsReferenced);
    await().untilAsserted(this::envVarsAreSet);
    await().untilAsserted(this::configMapIsSet);
  }

  private void configMapIsSet() {
    var configMap = extension.get(ConfigMap.class, Constant.K8S_CONFIG_MAP_NAME);
    assertThat(configMap).isNotNull();
    assertThat(configMap.getData()).containsAllEntriesOf(Map.of(
        Constant.APPDYN_ENV_VAR_CONTROLLER_HOST_NAME, CONTROLLER,
        Constant.APPDYN_ENV_VAR_CONTROLLER_PORT, CONTROLLER_PORT,
        Constant.APPDYN_ENV_VAR_CONTROLLER_SSL_ENABLED, "True",
        Constant.APPDYN_ENV_VAR_AGENT_ACCOUNT_NAME, ACCOUNT,
        Constant.APPDYN_ENV_VAR_AGENT_ACCOUNT_ACCESS_KEY, ACCESS_KEY,
        Constant.APPDYN_ENV_VAR_AGENT_NODE_NAME, "${HOSTNAME}"));
  }

  private void configMapIsReferenced() {
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
            .withName(Constant.K8S_CONFIG_MAP_NAME)
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
        new EnvVarBuilder().withName(Constant.APPDYN_ENV_VAR_AGENT_APPLICATION_NAME).withValue(APP_NAME).build(),
        new EnvVarBuilder().withName(Constant.APPDYN_ENV_VAR_AGENT_TIER_NAME).withValue(NS).build());
  }

  private void assertInitContainerIsSet() {
    var dep = extension.get(Deployment.class, DEPLOYMENT_NAME);
    assertThat(dep).isNotNull();
    var initContainers = dep.getSpec().getTemplate().getSpec().getInitContainers();
    assertThat(initContainers).hasSize(1);
    var initContainer = initContainers.get(0);
    assertThat(initContainer.getName()).isEqualTo(Constant.K8S_DEFAULT_INIT_CONTAINER_NAME);
    assertThat(initContainer.getImage()).isEqualTo(IMAGE);
    assertThat(initContainer.getCommand()).isEqualTo(List.of(COMMAND));
  }

  private Namespace createDefaultNamespace() {
    return new NamespaceBuilder()
        .withNewMetadata()
        .withName(NS)
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
    controllerSpec.setHost(CONTROLLER);
    controllerSpec.setPort(Integer.parseInt(CONTROLLER_PORT));
    var agentSpec = new AppDynamicsAgentSpec();
    agentSpec.setAccountAccessKey(ACCESS_KEY);
    agentSpec.setAccountName(ACCOUNT);
    agentSpec.setInitContainerCommand(COMMAND);
    agentSpec.setJavaAgentImage(IMAGE);
    var resource = new AppdynamicsOperatorCustomResource();
    resource.setMetadata(new ObjectMetaBuilder()
        .withName(OPERATOR_CRD_NAME)
        .withNamespace(NS)
        .build());
    resource.setSpec(new AppdynamicsOperatorSpec());
    resource.getSpec().setController(controllerSpec);
    resource.getSpec().setAgent(agentSpec);

    return resource;
  }
}
