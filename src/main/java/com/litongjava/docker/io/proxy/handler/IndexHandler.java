package com.litongjava.docker.io.proxy.handler;

import java.util.HashMap;
import java.util.Map;

import com.litongjava.model.body.RespBodyVo;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;

public class IndexHandler {
  public HttpResponse index(HttpRequest request) {
    // 如有需要，手动提取参数
    // 例如：String param = request.getParam("key");

    Map<String, String> data = new HashMap<>();
    RespBodyVo respVo = RespBodyVo.ok(data);
    return TioRequestContext.getResponse().setJson(respVo);
  }
}
