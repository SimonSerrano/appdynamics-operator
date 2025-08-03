package com.marmouset;

public class AppdynamicsOperatorStatus {
  private Boolean deploymentUpdated;

  private String appName;

  public Boolean getDeploymentUpdated() {
    return deploymentUpdated;
  }

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public void setDeploymentUpdated(Boolean deploymentUpdated) {
    this.deploymentUpdated = deploymentUpdated;
  }

  @Override
  public String toString() {
    return "AppdynamicsOperatorStatus [deploymentUpdated=" + deploymentUpdated + "]";
  }

}
