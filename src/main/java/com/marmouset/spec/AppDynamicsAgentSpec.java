package com.marmouset.spec;

public class AppDynamicsAgentSpec {
  private String javaAgentImage;
  private String accountName;
  private String accountAccessKey;
  private String initContainerCommand;

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
