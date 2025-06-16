package com.litongjava.docker.io.proxy.config;

import com.litongjava.context.BootConfiguration;
import com.litongjava.docker.io.proxy.handler.BigFileHandler;
import com.litongjava.docker.io.proxy.handler.DockerV2AuthHandler;
import com.litongjava.docker.io.proxy.handler.DockerV2DataHandler;
import com.litongjava.docker.io.proxy.handler.DockerV2RootHandler;
import com.litongjava.docker.io.proxy.handler.IndexHandler;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.http.server.router.HttpRequestRouter;

public class AppConfig implements BootConfiguration {

  public void config() {

    TioBootServer server = TioBootServer.me();
    HttpRequestRouter requestRouter = server.getRequestRouter();
    IndexHandler indexHandler = new IndexHandler();
    requestRouter.add("/", indexHandler::index);

    DockerV2RootHandler V2RootHandler = new DockerV2RootHandler();
    requestRouter.add("/v2/", V2RootHandler::index);

    DockerV2DataHandler indexV2Handler = new DockerV2DataHandler();
    requestRouter.add("/v2/*", indexV2Handler::index);

    DockerV2AuthHandler dockerV2AuthHandler = new DockerV2AuthHandler();
    requestRouter.add("/token", dockerV2AuthHandler::index);
    
    BigFileHandler bigFile = new BigFileHandler();
    requestRouter.add("/file", bigFile::index);
  }
}
