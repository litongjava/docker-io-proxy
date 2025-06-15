package com.litongjava.docker.io.proxy.utils;

import java.util.Map;

import okhttp3.Request;

public class HttpProxyUtils {
  public static void buildHeader(Map<String, String> headers, Request.Builder rb) {
    for (Map.Entry<String, String> h : headers.entrySet()) {
      String key = h.getKey();
      if ("host".equalsIgnoreCase(key) || "connection".equalsIgnoreCase(key)
      //
          || "keep-alive".equalsIgnoreCase(key) || "proxy-authenticate".equalsIgnoreCase(key)
          //
          || "proxy-authorization".equalsIgnoreCase(key) || "ti".equalsIgnoreCase(key)
          //
          || "trailer".equalsIgnoreCase(key) || "transfer-encoding".equalsIgnoreCase(key)
          //
          || "upgrade".equalsIgnoreCase(key)) {
        continue;
      }
      rb.header(key, h.getValue());
    }
  }
}
