package com.litongjava.docker.io.proxy.handler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import com.litongjava.docker.io.proxy.consts.DockerHubConst;
import com.litongjava.docker.io.proxy.utils.HttpProxyUtils;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.server.handler.HttpRequestHandler;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
public class DockerV2DataHandler implements HttpRequestHandler{

  private static final OkHttpClient httpClient = new OkHttpClient.Builder().followRedirects(true).build();

  /**
   * 统一处理 /v2/ 开头的所有 HTTP 方法
   */

  @Override
  public HttpResponse handle(HttpRequest request) throws Exception {
    HttpResponse response = TioRequestContext.getResponse();
    String method = request.getMethod().toString(); // GET, HEAD, POST…
    String uri = request.getRequestURI(); // /v2/.../manifests/...
    String query = request.getRequestLine().getQueryString(); // tag=latest

    // 1. 构建上游 URL
    String upstreamUrl = DockerHubConst.UPSTREAM_REGISTRY + uri + (query != null && !query.isEmpty() ? "?" + query : "");
    log.info("[Proxy] Upstream URL: {}", upstreamUrl);

    // 2. 构造本地缓存路径 —— 用 String.replace 而不是 replaceAll
    String pathWithoutLeadingSlash = uri.startsWith("/") ? uri.substring(1) : uri;

    // 先把所有 '/' → 本地分隔符
    String safePath = pathWithoutLeadingSlash.replace('/', File.separatorChar);
    // 再把 ':' → 本地分隔符，这样 sha256:abc → sha256\abc
    safePath = safePath.replace(':', File.separatorChar);

    File cacheFile = new File(DockerHubConst.CACHE_DIR, safePath);

    // 3. 如果是 GET 且缓存命中，直接返回
    if ("GET".equalsIgnoreCase(method) && uri.contains("/blobs/") && cacheFile.exists()) {
      log.info("Cache HIT: {}", cacheFile.getAbsolutePath());
      response.setStatus(200);
      response.setFileBody(cacheFile);
      return response;
    }

    // 4. 构造并发起上游请求
    RequestBody body = null;
    if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
      byte[] reqBytes = request.getBody() != null ? request.getBody() : new byte[0];
      body = RequestBody.create(reqBytes);
    }
    Request.Builder reqBuilder = new Request.Builder().url(upstreamUrl).method(method, body);

    // 5. 透传客户端所有 Header（包含 Authorization、Accept…）
    Map<String, String> headers = request.getHeaders();

    HttpProxyUtils.buildHeader(headers, reqBuilder);

    // 6. 执行请求并同步返回
    try (Response upstreamResp = httpClient.newCall(reqBuilder.build()).execute()) {
      // 状态码
      int code = upstreamResp.code();
      log.info("code:{}", code);
      response.setStatus(code);
      // 响应体（非 HEAD）
      if (!"HEAD".equalsIgnoreCase(method) && upstreamResp.body() != null) {
        // 缓存写文件
        if ("GET".equalsIgnoreCase(method) && code == 200) {
          String contentType = upstreamResp.header("Content-Type", "");

          boolean isBlob = uri.contains("/blobs/");

          if (isBlob) {
            // 写缓存
            File parent = cacheFile.getParentFile();
            if (!parent.exists()) {
              parent.mkdirs();
            }
            byte[] respBytes = upstreamResp.body().bytes();
            try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
              fos.write(respBytes);
            }
            response.setFileBody(cacheFile);

            log.info("Cache WRITE: {} ({})", cacheFile.getAbsolutePath(), contentType);
          } else {
            // 响应头
            Headers respH = upstreamResp.headers();
            for (String name : respH.names()) {
              response.setHeader(name, respH.get(name));
            }
            //响应体
            String string = upstreamResp.body().string();
            String charset = request.getCharset();
            response.setBody(string.getBytes(charset));
            log.info("Skip cache for unsupported type: {}", contentType);
          }
        }
      } else {
        Headers respH = upstreamResp.headers();
        for (String name : respH.names()) {
          response.setHeader(name, respH.get(name));
        }
      }
    } catch (IOException e) {
      // 上游失败则 502
      response.setStatus(502);
      String err = "Bad Gateway: " + e.getMessage();
      response.setBody(err.getBytes());
      log.error("Proxy 错误: {}", upstreamUrl, e);
    }

    return response;
  }
}
