package com.marmouset.visitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.marmouset.AppdynamicsOperatorCustomResource;
import com.marmouset.reconciler.AppdynamicsOperatorReconciler;

import io.fabric8.kubernetes.api.builder.Visitor;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;

public class InitContainerVisitor implements Visitor<DeploymentBuilder> {

  private static final Logger log = LoggerFactory.getLogger(InitContainerVisitor.class);

  private final AppdynamicsOperatorCustomResource customResource;

  public InitContainerVisitor(AppdynamicsOperatorCustomResource customResource) {
    this.customResource = customResource;
  }

  @Override
  public void visit(DeploymentBuilder builder) {
    // Add an init container to the deployment
    builder.editSpec()
        .editTemplate()
        .editSpec()
        .addNewInitContainer()
        .withName(customResource.getSpec().getInitContainerName())
        .withImage(customResource.getSpec().getAgent().getJavaAgentImage())
        .withCommand(customResource.getSpec().getAgent().getInitContainerCommand())
        .endInitContainer()
        .endSpec();
  }

}
