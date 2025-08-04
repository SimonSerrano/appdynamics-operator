package com.marmouset.reconciler;

import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Workflow;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.javaoperatorsdk.operator.api.reconciler.dependent.ReconcileResult;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResourceConfigBuilder;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.Mappers;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.config.informer.InformerEventSourceConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Constants;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.marmouset.AppdynamicsOperatorCustomResource;
import com.marmouset.visitor.EnvVarsVisitor;
import com.marmouset.visitor.InitContainerVisitor;

@ControllerConfiguration
public class AppdynamicsOperatorReconciler implements Reconciler<AppdynamicsOperatorCustomResource> {

  public static final String DEPLOYMENT_INJECT_FLAG = "com.appdynamics/inject-java";
  public static final String DEPLOYMENT_APP_NAME = "com.appdynamics/appname";

  private static final Logger log = LoggerFactory.getLogger(AppdynamicsOperatorReconciler.class);

  public AppdynamicsOperatorReconciler() {
  }

  @Override
  public List<EventSource<?, AppdynamicsOperatorCustomResource>> prepareEventSources(
      EventSourceContext<AppdynamicsOperatorCustomResource> context) {
    EventSource<Deployment, AppdynamicsOperatorCustomResource> deploymentEventSource = new InformerEventSource<Deployment, AppdynamicsOperatorCustomResource>(
        InformerEventSourceConfiguration.from(Deployment.class, AppdynamicsOperatorCustomResource.class)
            .withLabelSelector(DEPLOYMENT_APP_NAME)
            .build(),
        context);
    return List.of(deploymentEventSource);
  }

  public UpdateControl<AppdynamicsOperatorCustomResource> reconcile(AppdynamicsOperatorCustomResource appdynOpCR,
      Context<AppdynamicsOperatorCustomResource> context) {
    var client = context.getClient();
    var deployments = client.apps().deployments()
        .inAnyNamespace()
        .withLabel(DEPLOYMENT_APP_NAME)
        .list().getItems();

    log.debug("Found {} deployments with label {}", deployments.size(), DEPLOYMENT_APP_NAME);
    deployments.forEach((d) -> processDeployment(appdynOpCR, client, d));

    return UpdateControl.patchResource(appdynOpCR);
  }

  @Override
  public ErrorStatusUpdateControl<AppdynamicsOperatorCustomResource> updateErrorStatus(
      AppdynamicsOperatorCustomResource resource, Context<AppdynamicsOperatorCustomResource> context,
      Exception e) {
    log.error(e.getMessage(), e);
    return Reconciler.super.updateErrorStatus(resource, context, e);
  }

  protected void processDeployment(AppdynamicsOperatorCustomResource appdynOpCR, KubernetesClient client,
      Deployment deployment) {
    var annotations = deployment.getMetadata().getAnnotations();
    if (annotations.containsKey(DEPLOYMENT_INJECT_FLAG)
        && Boolean.parseBoolean(annotations.get(DEPLOYMENT_INJECT_FLAG))) {
      log.info("Deployment {} has annotation {}=true, enabling instrumentation", deployment.getMetadata().getName(),
          DEPLOYMENT_INJECT_FLAG);
      log.debug("Adding initContainer and env variables to deployment named {}", deployment.getMetadata().getName());
      var desired = deployment.edit().accept(
          new InitContainerVisitor(appdynOpCR),
          new EnvVarsVisitor(appdynOpCR, deployment.getMetadata().getLabels().get(DEPLOYMENT_APP_NAME),
              deployment.getMetadata().getNamespace()))
          .build();
      client.resource(desired).update();
    }

    log.info("Deployment {} does not have annotation {} or is not set to true, instrumentation is disabled",
        deployment.getMetadata().getName(),
        DEPLOYMENT_INJECT_FLAG);

  }

}
