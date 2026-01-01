package com.litongjava.docker.io.proxy.handler;

import com.litongjava.docker.io.proxy.consts.DockerHubConst;
import com.litongjava.docker.io.proxy.utils.HttpProxyUtils;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.MimeType;
import com.litongjava.tio.http.server.handler.HttpRequestHandler;

import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
public class DockerV2AuthHandler implements HttpRequestHandler {
  private static final OkHttpClient HTTP = new OkHttpClient.Builder().followRedirects(true).build();

  @Override
  public HttpResponse handle(HttpRequest req) throws Exception {
    // 从原请求中取 path 和 query
    String path = req.getRequestURI(); // 应该是 "/token"
    String query = req.getRequestLine().getQueryString();

    // 构造 upstream auth URL
    HttpUrl upstreamUrl = HttpUrl.parse(DockerHubConst.UPSTREAM_TOKEN).newBuilder().encodedPath(path).encodedQuery(query).build();
    log.info("upstreamUrl:{}", upstreamUrl);

    Request.Builder rb = new Request.Builder().url(upstreamUrl).method(req.getMethod().toString(), null);

    // 透传所有头
    HttpProxyUtils.buildHeader(req.getHeaders(), rb);

    try (Response upr = HTTP.newCall(rb.build()).execute()) {
      HttpResponse resp = TioRequestContext.getResponse();
      int code = upr.code();
      log.info("code:{}", code);
      resp.setStatus(code);

      // 透传 upstream headers（Content-Type: application/json 等）
//      Headers headers = upr.headers();
//      Map<String, List<String>> multimap = headers.toMultimap();
//      for (Entry<String, List<String>> h : multimap.entrySet()) {
//        resp.setHeader(h.getKey(), String.join(",", h.getValue()));
//      }

      // 把 body 原样返回
      if (upr.body() != null) {
        String charset = req.getCharset();
        resp.setString(upr.body().string(), charset, MimeType.APPLICATION_JSON.toString());
      }
      return resp;
    }
  }

}
