package com.marmouset.visitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.marmouset.AppdynamicsOperatorCustomResource;

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
    var name = customResource.getSpec().getInitContainerName();
    var image = customResource.getSpec().getAgent().getJavaAgentImage();
    var command = customResource.getSpec().getAgent().getInitContainerCommand();
    log.debug("Adding init container with name {}, image {} and command {}", name, image, command);
    builder.editSpec()
        .editTemplate()
        .editSpec()
        .addNewInitContainer()
        .withName(name)
        .withImage(image)
        .withCommand(command)
        .endInitContainer()
        .endSpec()
        .endTemplate()
        .endSpec();
  }

}
