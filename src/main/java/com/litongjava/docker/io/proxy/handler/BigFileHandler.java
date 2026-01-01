package com.litongjava.docker.io.proxy.handler;

import java.io.File;

import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.server.handler.HttpRequestHandler;

public class BigFileHandler implements HttpRequestHandler {

  @Override
  public HttpResponse handle(HttpRequest httpRequest) throws Exception {
    HttpResponse response = TioRequestContext.getResponse();
    String file = "255445ddd42abd3da5bfdc129ad52cf5.mp4";
    File cacheFile = new File(file);
    response.setFileBody(cacheFile);
    return response;
  }
}
