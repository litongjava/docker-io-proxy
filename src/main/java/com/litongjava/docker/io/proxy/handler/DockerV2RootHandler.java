package com.litongjava.docker.io.proxy.handler;

import java.io.IOException;

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
    // 构造 upstream URL
    String upstreamUrl = DockerHubConst.UPSTREAM_REGISTRY + req.getRequestURI() + (req.getRequestLine().getQueryString() == null ? "" : "?" + req.getRequestLine().getQueryString());

    Request.Builder rb = new Request.Builder().url(upstreamUrl).method(req.getMethod().toString(), null);

    // 透传客户端的 header
    HttpProxyUtils.buildHeader(req.getHeaders(), rb);

    try (Response upr = HTTP.newCall(rb.build()).execute()) {
      HttpResponse resp = TioRequestContext.getResponse();
      resp.setStatus(upr.code());

      Headers respH = upr.headers();
      for (String name : respH.names()) {
        String value = respH.get(name);

        // 拦截并重写 WWW-Authenticate
        if ("WWW-Authenticate".equalsIgnoreCase(name) && value != null) {
          // 解析出 original realm 和其他参数
          // 示例：Bearer realm="https://auth.docker.io/token",service="registry.docker.io"
          String newRealm = buildLocalRealm(req);
          // 保留其他部分(service=...) 不变
          String rest = value.replaceAll("(?i)realm=\"[^\"]+\"", "realm=\"" + newRealm + "\"");
          resp.setHeader(name, rest);
        } else {
          resp.setHeader(name, value);
        }
      }

      // 转 body
      if (!"HEAD".equalsIgnoreCase(req.getMethod().toString()) && upr.body() != null) {
        resp.setBody(upr.body().bytes());
      }
      return resp;
    }
  }

  /**
   * 根据请求的 Host 和 X-Forwarded-Proto 头，拼出本地 token 服务地址
   */
  private String buildLocalRealm(HttpRequest req) {
    // 1. 先看有没有代理头
    String scheme = req.getHeader("x-forwarded-proto");
    // 2. 如果没有，就退回到你跟 Tio 之间实际的协议（通常是 http）
    if (scheme == null || scheme.isEmpty()) {
      scheme = "http";
    }

    // 3. 再拿 Host 头
    String host = req.getHost();

    // 4. 拼出绝对 URL
    return scheme + "://" + host + "/token";
  }

}
