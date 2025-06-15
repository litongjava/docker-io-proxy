package com.litongjava.docker.io.proxy;

import com.litongjava.annotation.AComponentScan;
import com.litongjava.docker.io.proxy.config.AppConfig;
import com.litongjava.tio.boot.TioApplication;

@AComponentScan
public class DockerIOProxyApp {
  public static void main(String[] args) {
    long start = System.currentTimeMillis();
    //TioApplicationWrapper.run(HelloApp.class, args);
    TioApplication.run(DockerIOProxyApp.class, new AppConfig(), args);
    long end = System.currentTimeMillis();
    System.out.println((end - start) + "ms");
  }
}
