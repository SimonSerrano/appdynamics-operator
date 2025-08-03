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
import io.fabric8.kubernetes.api.model.apps.Deployment;
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
import com.marmouset.dependentresource.DeploymentDependentResource;

@ControllerConfiguration
public class AppdynamicsOperatorReconciler implements Reconciler<AppdynamicsOperatorCustomResource> {

  public static final String DEPLOYMENT_NAME = "deployment";
  public static final String DEPLOYMENT_INJECT_FLAG = "com.appdynamics/inject-java";
  public static final String DEPLOYMENT_APP_NAME = "com.appdynamics/appname";

  private static final Logger log = LoggerFactory.getLogger(AppdynamicsOperatorReconciler.class);

  private DeploymentDependentResource deploymentDependent;

  public AppdynamicsOperatorReconciler() {
    deploymentDependent = new DeploymentDependentResource();
    // deploymentDependent.configureWith(
    // new KubernetesDependentResourceConfigBuilder<Deployment>()
    // .withKubernetesDependentInformerConfig(
    // InformerConfiguration.builder(
    // deploymentDependent.resourceType())
    // .withLabelSelector(DEPLOYMENT_LABEL_SELECTOR)
    // .withNamespaces(Constants.WATCH_ALL_NAMESPACE_SET)
    // .build())
    // .build());
  }

  @Override
  public List<EventSource<?, AppdynamicsOperatorCustomResource>> prepareEventSources(
      EventSourceContext<AppdynamicsOperatorCustomResource> context) {
    EventSource<Deployment, AppdynamicsOperatorCustomResource> deploymentEventSource = new InformerEventSource<Deployment, AppdynamicsOperatorCustomResource>(
        InformerEventSourceConfiguration.from(Deployment.class, AppdynamicsOperatorCustomResource.class)
            // .withSecondaryToPrimaryMapper(Mappers.fromAnnotation(DEPLOYMENT_APP_NAME,
            // null, null))
            .withSecondaryToPrimaryMapper(resource -> {
              final var metadata = resource.getMetadata();
              if (metadata == null) {
                return Collections.emptySet();
              } else {
                final var map = metadata.getAnnotations();
                if (map == null) {
                  return Collections.emptySet();
                }
                var name = map.get(DEPLOYMENT_APP_NAME);
                if (name == null) {
                  return Collections.emptySet();
                }
                var namespace = resource.getMetadata().getNamespace();

                return Set.of(new ResourceID(name, namespace));
              }
            })
            .withLabelSelector(DEPLOYMENT_INJECT_FLAG + "=true," + DEPLOYMENT_APP_NAME)
            .build(),
        context);
    return List.of(deploymentEventSource);
  }

  public UpdateControl<AppdynamicsOperatorCustomResource> reconcile(AppdynamicsOperatorCustomResource appdynOpCR,
      Context<AppdynamicsOperatorCustomResource> context) {
    var deploymentOpt = context.getSecondaryResource(Deployment.class);
    log.debug("Reconciling deployment : {}", deploymentOpt.orElse(null));
    ReconcileResult<Deployment> result = deploymentDependent.reconcile(appdynOpCR, context);
    log.debug("Got result while reconciling deployment : {}", result);
    return UpdateControl.noUpdate();
  }

  @Override
  public ErrorStatusUpdateControl<AppdynamicsOperatorCustomResource> updateErrorStatus(
      AppdynamicsOperatorCustomResource resource, Context<AppdynamicsOperatorCustomResource> context,
      Exception e) {
    log.error(e.getMessage(), e);
    return Reconciler.super.updateErrorStatus(resource, context, e);
  }

}
