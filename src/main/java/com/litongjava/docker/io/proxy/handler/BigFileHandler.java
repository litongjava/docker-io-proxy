package com.litongjava.docker.io.proxy.handler;

import java.io.File;

import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;

public class BigFileHandler {

  public HttpResponse index(HttpRequest request) {
    HttpResponse response = TioRequestContext.getResponse();
    String file = "F:\\video\\左程云-算法课\\03.进阶班\\255445ddd42abd3da5bfdc129ad52cf5.mp4";
    File cacheFile = new File(file);
    response.setFileBody(cacheFile);
    return response;
  }
}
