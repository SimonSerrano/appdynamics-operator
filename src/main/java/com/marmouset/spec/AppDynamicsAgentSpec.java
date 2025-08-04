package com.marmouset.spec;

import com.marmouset.utils.Constant;

public class AppDynamicsAgentSpec {
  private String javaAgentImage = Constant.APPDYN_DEFAULT_JAVA_AGENT_IMAGE;
  private String accountName;
  private String accountAccessKey;
  private String initContainerCommand = Constant.APPDYN_DEFAULT_JAVA_AGENT_COMMAND;

  public String getJavaAgentImage() {
    return javaAgentImage;
  }

  public void setJavaAgentImage(String javaAgentImage) {
    this.javaAgentImage = javaAgentImage;
  }

  public String getAccountName() {
    return accountName;
  }

  public void setAccountName(String accountName) {
    this.accountName = accountName;
  }

  public String getAccountAccessKey() {
    return accountAccessKey;
  }

  public void setAccountAccessKey(String accountAccess) {
    this.accountAccessKey = accountAccess;
  }

  public String getInitContainerCommand() {
    return initContainerCommand;
  }

  public void setInitContainerCommand(String initContainerCommand) {
    this.initContainerCommand = initContainerCommand;
  }

}
