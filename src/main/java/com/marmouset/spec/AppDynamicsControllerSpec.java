package com.marmouset.spec;

import com.marmouset.utils.Constant;

public class AppDynamicsControllerSpec {
  private String host;
  private int port;
  private boolean ssl = Constant.K8S_DEFAULT_APPDYNAMICS_SSL_ENABLED;

  public boolean isSsl() {
    return ssl;
  }

  public void setSsl(boolean ssl) {
    this.ssl = ssl;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

}
