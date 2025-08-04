package com.marmouset.visitor;

import java.util.HashMap;

import com.marmouset.spec.AppdynamicsOperatorSpec;
import com.marmouset.utils.Constant;

import io.fabric8.kubernetes.api.builder.Visitor;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;

public class ConfigMapVisitor implements Visitor<ConfigMapBuilder> {

  private final AppdynamicsOperatorSpec spec;
  private final String namespace;

  public ConfigMapVisitor(AppdynamicsOperatorSpec spec, String namespace) {
    this.spec = spec;
    this.namespace = namespace;
  }

  @Override
  public void visit(ConfigMapBuilder builder) {
    var data = new HashMap<String, String>();
    data.put(Constant.APPDYN_ENV_VAR_CONTROLLER_HOST_NAME, spec.getController().getHost());
    data.put(Constant.APPDYN_ENV_VAR_CONTROLLER_PORT, String.valueOf(spec.getController().getPort()));
    data.put(Constant.APPDYN_ENV_VAR_CONTROLLER_SSL_ENABLED, spec.getController().isSsl() ? "True" : "False");
    data.put(Constant.APPDYN_ENV_VAR_AGENT_ACCOUNT_NAME, spec.getAgent().getAccountName());
    data.put(Constant.APPDYN_ENV_VAR_AGENT_ACCOUNT_ACCESS_KEY, spec.getAgent().getAccountAccessKey());
    data.put(Constant.APPDYN_ENV_VAR_AGENT_NODE_NAME, "${HOSTNAME}");
    builder.editMetadata()
        .withName(spec.getConfigMapName())
        .withNamespace(namespace)
        .endMetadata()
        .addToData(data);
  }

}
