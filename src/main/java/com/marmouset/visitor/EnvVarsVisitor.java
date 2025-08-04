package com.marmouset.visitor;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.marmouset.AppdynamicsOperatorCustomResource;
import com.marmouset.utils.Constant;

import io.fabric8.kubernetes.api.builder.Visitor;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;

public class EnvVarsVisitor implements Visitor<DeploymentBuilder> {

  private static final Logger log = LoggerFactory.getLogger(EnvVarsVisitor.class);

  private final AppdynamicsOperatorCustomResource cr;
  private final String appName;
  private final String namespace;

  public EnvVarsVisitor(AppdynamicsOperatorCustomResource cr, String appName, String namespace) {
    this.cr = cr;
    this.appName = appName;
    this.namespace = namespace;
  }

  @Override
  public void visit(DeploymentBuilder builder) {

    builder
        .editSpec()
        .editTemplate()
        .editSpec()
        .editContainer(0)
        .addAllToEnv(buildEnvVars())
        .addToEnvFrom(buildEnvVarFromSource())
        .endContainer()
        .endSpec()
        .endTemplate()
        .endSpec();
  }

  private EnvFromSource buildEnvVarFromSource() {
    log.debug("Setting config map ref with name {}", cr.getSpec().getConfigMapName());
    return new EnvFromSourceBuilder()
        .withNewConfigMapRef()
        .withName(cr.getSpec().getConfigMapName())
        .endConfigMapRef()
        .build();
  }

  private List<EnvVar> buildEnvVars() {
    log.debug("Setting env var {} with value {}", Constant.APPDYN_ENV_VAR_AGENT_APPLICATION_NAME, appName);
    log.debug("Setting env var {} with value {}", Constant.APPDYN_ENV_VAR_AGENT_TIER_NAME, namespace);
    return List.of(
        new EnvVarBuilder()
            .withName(Constant.APPDYN_ENV_VAR_AGENT_APPLICATION_NAME)
            .withValue(appName)
            .build(),
        new EnvVarBuilder()
            .withName(Constant.APPDYN_ENV_VAR_AGENT_TIER_NAME)
            .withValue(namespace)
            .build());
  }
}
