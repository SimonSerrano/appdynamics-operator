package com.marmouset.dependentresource;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.marmouset.AppdynamicsOperatorCustomResource;
import com.marmouset.exception.DeploymentReconciliationException;
import com.marmouset.reconciler.AppdynamicsOperatorReconciler;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Constants;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.Updater;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;

public class DeploymentDependentResource
    extends KubernetesDependentResource<Deployment, AppdynamicsOperatorCustomResource>
    implements Updater<Deployment, AppdynamicsOperatorCustomResource> {
  public static final String APPDYNAMICS_AGENT_APPLICATION_NAME = "APPDYNAMICS_AGENT_APPLICATION_NAME";
  public static final String APPDYNAMICS_AGENT_TIER_NAME = "APPDYNAMICS_AGENT_TIER_NAME";

  private static final Logger log = LoggerFactory.getLogger(DeploymentDependentResource.class);

  @Override
  protected Deployment desired(AppdynamicsOperatorCustomResource primary,
      Context<AppdynamicsOperatorCustomResource> context) {
    var deploymentOpt = context.getSecondaryResource(Deployment.class);
    var deployment = deploymentOpt.orElseThrow(() -> new DeploymentReconciliationException("Deployment not found"));

    log.debug("Updating deployment : {}", deployment.getMetadata().getName());

    var namespace = deployment.getMetadata().getNamespace();
    var appNameOpt = Optional.ofNullable(deployment.getMetadata().getLabels().get("com.appdynamics/appname"));

    var appName = appNameOpt
        .orElseThrow(() -> new DeploymentReconciliationException("Label com.appdynamics/appname not found"));

    var podSpec = deployment.getSpec().getTemplate().getSpec();

    var containers = podSpec.getContainers();

    if (containers.size() == 0) {
      throw new DeploymentReconciliationException("Container not found");
    }

    var container = containers.get(0);
    var env = container.getEnv();
    env.addAll(buildEnvVars(namespace, appName));
    container.setEnv(env);

    var envFromSource = container.getEnvFrom();
    envFromSource.add(buildEnvVarFromSource(primary));
    container.setEnvFrom(envFromSource);

    var initContainers = deployment.getSpec().getTemplate().getSpec().getInitContainers();
    var initContainer = buildInitContainer(primary);
    initContainers.add(initContainer);
    podSpec.setInitContainers(initContainers);

    return deployment;
  }

  private EnvFromSource buildEnvVarFromSource(AppdynamicsOperatorCustomResource primary) {
    return new EnvFromSourceBuilder()
        .withNewConfigMapRef()
        .withName(primary.getSpec().getConfigMapName())
        .endConfigMapRef()
        .build();
  }

  private List<EnvVar> buildEnvVars(String namespace, String appName) {
    return List.of(
        new EnvVarBuilder()
            .withName(APPDYNAMICS_AGENT_APPLICATION_NAME)
            .withValue(appName)
            .build(),
        new EnvVarBuilder()
            .withName(APPDYNAMICS_AGENT_TIER_NAME)
            .withValue(namespace)
            .build());
  }

  private Container buildInitContainer(AppdynamicsOperatorCustomResource primary) {
    var initContainer = new ContainerBuilder()
        .withName(primary.getSpec().getInitContainerName())
        .withImage(primary.getSpec().getAgent().getJavaAgentImage())
        .withCommand(primary.getSpec().getAgent().getInitContainerCommand())
        .build();

    return initContainer;
  }

}
