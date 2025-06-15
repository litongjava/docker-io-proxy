package com.litongjava.docker.io.proxy.handler;

import java.io.IOException;
import java.util.Map;

import com.litongjava.docker.io.proxy.consts.DockerHubConst;
import com.litongjava.docker.io.proxy.utils.HttpProxyUtils;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DockerV2RootHandler {
  private static final OkHttpClient HTTP = new OkHttpClient.Builder().followRedirects(true).build();

  public HttpResponse index(HttpRequest req) throws IOException {
    String upstreamUrl = DockerHubConst.UPSTREAM_REGISTRY + req.getRequestURI() + (req.getRequestLine().getQueryString() == null ? "" : "?" + req.getRequestLine().getQueryString());
    Request.Builder rb = new Request.Builder().url(upstreamUrl).method(req.getMethod().toString(), null);

    Map<String, String> headers = req.getHeaders();
    // 透传头
    HttpProxyUtils.buildHeader(headers, rb);

    try (Response upr = HTTP.newCall(rb.build()).execute()) {
      HttpResponse resp = TioRequestContext.getResponse();
      resp.setStatus(upr.code());

      Headers respH = upr.headers();
      for (String name : respH.names()) {
        resp.setHeader(name, respH.get(name));
      }
      if (!"HEAD".equalsIgnoreCase(req.getMethod().toString()) && upr.body() != null) {
        resp.setBody(upr.body().bytes());
      }
      return resp;
    }
  }

}
