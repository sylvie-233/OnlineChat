package com.sylvie233.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * OnlineChat 启动入口
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.sylvie233")
public class OnlineChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlineChatApplication.class, args);
        System.out.println("""

                ╔══════════════════════════════════════════════╗
                ║     OnlineChat IM Server 启动成功!           ║
                ║     Netty WebSocket: ws://localhost:9090/ws  ║
                ║     HTTP API:       http://localhost:8080     ║
                ║     API Docs:       http://localhost:8080/doc.html ║
                ╚══════════════════════════════════════════════╝
                """);
    }
}
