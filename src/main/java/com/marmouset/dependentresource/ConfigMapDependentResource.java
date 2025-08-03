package com.marmouset.dependentresource;

import java.util.HashMap;

import com.marmouset.AppdynamicsOperatorCustomResource;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

// @KubernetesDependent
public class ConfigMapDependentResource
    extends CRUDKubernetesDependentResource<ConfigMap, AppdynamicsOperatorCustomResource> {

  public static final String APPDYNAMICS_CONTROLLER_HOST_NAME = "APPDYNAMICS_CONTROLLER_HOST_NAME";
  public static final String APPDYNAMICS_CONTROLLER_PORT = "APPDYNAMICS_CONTROLLER_PORT";
  public static final String APPDYNAMICS_CONTROLLER_SSL_ENABLED = "APPDYNAMICS_CONTROLLER_SSL_ENABLED";
  public static final String APPDYNAMICS_AGENT_ACCOUNT_NAME = "APPDYNAMICS_AGENT_ACCOUNT_NAME";
  public static final String APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY = "APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY";
  public static final String APPDYNAMICS_AGENT_NODE_NAME = "APPDYNAMICS_AGENT_NODE_NAME";

  @Override
  protected ConfigMap desired(AppdynamicsOperatorCustomResource primary,
      Context<AppdynamicsOperatorCustomResource> context) {
    var data = new HashMap<String, String>();
    data.put(APPDYNAMICS_CONTROLLER_HOST_NAME, primary.getSpec().getController().getHost());
    data.put(APPDYNAMICS_CONTROLLER_PORT, String.valueOf(primary.getSpec().getController().getPort()));
    data.put(APPDYNAMICS_CONTROLLER_SSL_ENABLED, primary.getSpec().getController().isSsl() ? "True" : "False");
    data.put(APPDYNAMICS_AGENT_ACCOUNT_NAME, primary.getSpec().getAgent().getAccountName());
    data.put(APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY, primary.getSpec().getAgent().getAccountAccessKey());
    data.put(APPDYNAMICS_AGENT_NODE_NAME, "${HOSTNAME}");
    return new ConfigMapBuilder()
        .withMetadata(
            new ObjectMetaBuilder()
                .withName(primary.getMetadata().getName())
                .withNamespace(primary.getMetadata().getNamespace())
                .build())
        .withData(data)
        .build();
  }
}