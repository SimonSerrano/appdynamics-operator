package com.marmouset;

import io.fabric8.kubernetes.client.CustomResource;

import com.marmouset.spec.AppdynamicsOperatorSpec;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("com.marmouset")
@Version("v1")
@Kind("Instrumentation")
public class AppdynamicsOperatorCustomResource
        extends CustomResource<AppdynamicsOperatorSpec, AppdynamicsOperatorStatus> implements Namespaced {
}
