package com.marmouset.spec;

public class AppdynamicsOperatorSpec {
    private AppDynamicsControllerSpec controller = new AppDynamicsControllerSpec();
    private AppDynamicsAgentSpec agent = new AppDynamicsAgentSpec();
    private String initContainerName = "attach-appdynamics-java-agent";
    private String configMapName = "appdynamics-config-map";

    public String getConfigMapName() {
        return configMapName;
    }

    public void setConfigMapName(String configMapName) {
        this.configMapName = configMapName;
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
