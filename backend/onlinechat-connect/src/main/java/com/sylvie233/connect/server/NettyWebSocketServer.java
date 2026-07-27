package com.sylvie233.connect.server;

import com.sylvie233.connect.handler.WebSocketHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * Netty WebSocket 服务器
 * <p>独立于 Spring MVC，监听独立端口</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NettyWebSocketServer {

    @Value("${im.websocket.port:9090}")
    private int port;

    @Value("${im.websocket.path:/ws}")
    private String websocketPath;

    private final WebSocketHandler webSocketHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    @PostConstruct
    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 心跳检测: 读空闲 60s 触发
                        pipeline.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
                        // HTTP 编解码
                        pipeline.addLast(new HttpServerCodec());
                        // 大数据流支持
                        pipeline.addLast(new ChunkedWriteHandler());
                        // HTTP 消息聚合
                        pipeline.addLast(new HttpObjectAggregator(65536));
                        // WebSocket 协议升级
                        pipeline.addLast(new WebSocketServerProtocolHandler(websocketPath,
                                null, true, 65536));
                        // 业务处理器
                        pipeline.addLast(webSocketHandler);
                    }
                });

        ChannelFuture future = bootstrap.bind(port).sync();
        serverChannel = future.channel();
        log.info("Netty WebSocket 服务启动成功，端口: {}, 路径: {}", port, websocketPath);
    }

    @PreDestroy
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("Netty WebSocket 服务已关闭");
    }
}
