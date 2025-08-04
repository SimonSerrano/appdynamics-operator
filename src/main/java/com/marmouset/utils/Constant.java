package com.marmouset.utils;

public class Constant {
  // K8S
  public static final String K8S_DEPLOYMENT_INJECT_FLAG = "com.appdynamics/inject-java";
  public static final String K8S_DEPLOYMENT_APP_NAME = "com.appdynamics/appname";
  public static final String K8S_CONFIG_MAP_NAME = "appdynamics-config-map";
  public static final String K8S_DEFAULT_INIT_CONTAINER_NAME = "attach-appdynamics-java-agent";
  public static final Boolean K8S_DEFAULT_APPDYNAMICS_SSL_ENABLED = true;

  // APPDYNAMICS
  public static final String APPDYN_ENV_VAR_AGENT_APPLICATION_NAME = "APPDYNAMICS_AGENT_APPLICATION_NAME";
  public static final String APPDYN_ENV_VAR_AGENT_TIER_NAME = "APPDYNAMICS_AGENT_TIER_NAME";
  public static final String APPDYN_ENV_VAR_CONTROLLER_HOST_NAME = "APPDYNAMICS_CONTROLLER_HOST_NAME";
  public static final String APPDYN_ENV_VAR_CONTROLLER_PORT = "APPDYNAMICS_CONTROLLER_PORT";
  public static final String APPDYN_ENV_VAR_CONTROLLER_SSL_ENABLED = "APPDYNAMICS_CONTROLLER_SSL_ENABLED";
  public static final String APPDYN_ENV_VAR_AGENT_ACCOUNT_NAME = "APPDYNAMICS_AGENT_ACCOUNT_NAME";
  public static final String APPDYN_ENV_VAR_AGENT_ACCOUNT_ACCESS_KEY = "APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY";
  public static final String APPDYN_ENV_VAR_AGENT_NODE_NAME = "APPDYNAMICS_AGENT_NODE_NAME";
  public static final String APPDYN_DEFAULT_JAVA_AGENT_IMAGE = "";
  public static final String APPDYN_DEFAULT_JAVA_AGENT_COMMAND = "";
}