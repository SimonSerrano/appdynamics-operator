package com.marmouset.reconciler;

import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.config.informer.InformerEventSourceConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.marmouset.AppdynamicsOperatorCustomResource;
import com.marmouset.utils.Constant;
import com.marmouset.visitor.ConfigMapVisitor;
import com.marmouset.visitor.EnvVarsVisitor;
import com.marmouset.visitor.InitContainerVisitor;

@ControllerConfiguration
public class AppdynamicsOperatorReconciler implements Reconciler<AppdynamicsOperatorCustomResource> {

  private static final Logger log = LoggerFactory.getLogger(AppdynamicsOperatorReconciler.class);

  public AppdynamicsOperatorReconciler() {
  }

  @Override
  public List<EventSource<?, AppdynamicsOperatorCustomResource>> prepareEventSources(
      EventSourceContext<AppdynamicsOperatorCustomResource> context) {
    EventSource<Deployment, AppdynamicsOperatorCustomResource> deploymentEventSource = new InformerEventSource<Deployment, AppdynamicsOperatorCustomResource>(
        InformerEventSourceConfiguration.from(Deployment.class, AppdynamicsOperatorCustomResource.class)
            .withLabelSelector(Constant.K8S_DEPLOYMENT_APP_NAME)
            .build(),
        context);
    return List.of(deploymentEventSource);
  }

  public UpdateControl<AppdynamicsOperatorCustomResource> reconcile(AppdynamicsOperatorCustomResource appdynOpCR,
      Context<AppdynamicsOperatorCustomResource> context) {
    var client = context.getClient();
    var deployments = client.apps().deployments()
        .inAnyNamespace()
        .withLabel(Constant.K8S_DEPLOYMENT_APP_NAME)
        .list().getItems();

    log.debug("Found {} deployments with label {}", deployments.size(), Constant.K8S_DEPLOYMENT_APP_NAME);
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
    if (annotations.containsKey(Constant.K8S_DEPLOYMENT_INJECT_FLAG)
        && Boolean.parseBoolean(annotations.get(Constant.K8S_DEPLOYMENT_INJECT_FLAG))) {
      log.info("Deployment {} has annotation {}=true, enabling instrumentation", deployment.getMetadata().getName(),
          Constant.K8S_DEPLOYMENT_INJECT_FLAG);
      log.debug("Adding initContainer and env variables to deployment named {}", deployment.getMetadata().getName());

      var ns = deployment.getMetadata().getNamespace();

      var desiredDeployment = deployment.edit().accept(
          new InitContainerVisitor(appdynOpCR),
          new EnvVarsVisitor(
              appdynOpCR,
              deployment.getMetadata().getLabels().get(Constant.K8S_DEPLOYMENT_APP_NAME),
              ns))
          .build();
      client.resource(desiredDeployment).update();

      log.debug("Adding config map to namespace {}", ns);

      var configMap = client.configMaps().inNamespace(ns).withName(appdynOpCR.getSpec().getConfigMapName()).get();

      if (Objects.isNull(configMap)) {
        log.info("Config map {} does not exist in namespace {}, creating it", appdynOpCR.getSpec().getConfigMapName(),
            ns);
        client.resource(new ConfigMapBuilder().accept(new ConfigMapVisitor(appdynOpCR.getSpec(), ns)).build()).create();
      } else {
        var desiredConfigMap = configMap.edit().accept(new ConfigMapVisitor(appdynOpCR.getSpec(), ns)).build();
        client.resource(desiredConfigMap).update();
      }

    }

    log.info("Deployment {} does not have annotation {} or is not set to true, instrumentation is disabled",
        deployment.getMetadata().getName(),
        Constant.K8S_DEPLOYMENT_INJECT_FLAG);

  }

}
