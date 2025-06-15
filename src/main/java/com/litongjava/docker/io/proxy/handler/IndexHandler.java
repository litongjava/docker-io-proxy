package com.litongjava.docker.io.proxy.handler;

import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;

public class IndexHandler {
  public HttpResponse index(HttpRequest request) {
    HttpResponse response = TioRequestContext.getResponse();
    return response;
  }
}
