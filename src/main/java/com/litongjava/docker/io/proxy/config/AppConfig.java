package com.litongjava.docker.io.proxy.config;
import com.litongjava.context.BootConfiguration;
import com.litongjava.docker.io.proxy.handler.IndexHandler;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.http.server.router.HttpRequestRouter;

public class AppConfig implements BootConfiguration {

  public void config() {

    TioBootServer server = TioBootServer.me();
    HttpRequestRouter requestRouter = server.getRequestRouter();

    IndexHandler indexHandler = new IndexHandler();
    requestRouter.add("/", indexHandler::index);
  }
}
