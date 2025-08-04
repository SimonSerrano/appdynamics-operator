package com.marmouset.spec;

import com.marmouset.utils.Constant;

public class AppdynamicsOperatorSpec {
    private AppDynamicsControllerSpec controller = new AppDynamicsControllerSpec();
    private AppDynamicsAgentSpec agent = new AppDynamicsAgentSpec();
    private String initContainerName = Constant.K8S_DEFAULT_INIT_CONTAINER_NAME;

    public String getConfigMapName() {
        return Constant.K8S_CONFIG_MAP_NAME;
    }

    public String getInitContainerName() {
        return initContainerName;
    }

    public void setInitContainerName(String initContainerName) {
        this.initContainerName = initContainerName;
    }

    public AppDynamicsControllerSpec getController() {
        return controller;
    }

    public void setController(AppDynamicsControllerSpec controller) {
        this.controller = controller;
    }

    public AppDynamicsAgentSpec getAgent() {
        return agent;
    }

    public void setAgent(AppDynamicsAgentSpec agent) {
        this.agent = agent;
    }

}
