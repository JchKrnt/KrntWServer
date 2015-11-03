package org.kurento.tutorial.n_one2manycall;

import org.kurento.client.KurentoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;


/**
 * Created by jingbiaowang on 2015/8/27
 * <p>
 * Video call many of 1 to N demo (main).
 * .
 *
 * @author jch.
 * @since sohu 1.0
 */

@Configuration
@EnableWebSocket
@EnableAutoConfiguration
public class NOne3ManyCallApp implements WebSocketConfigurer {

    final static String DEFAULT_KMS_WS_URI = "ws://localhost:8888/kurento";
    public static final Logger logger = LoggerFactory.getLogger(NOne3ManyCallApp.class);

    @Bean
    public NCallHandler callHandler() {
        return new NCallHandler();
    }

    @Bean
    public KurentoClient kurentoClient() {
        return KurentoClient.create(System.getProperty("kms.ws.uri", DEFAULT_KMS_WS_URI));
    }

//    @Bean
//    public RoomManager roomManager() {
//        return new RoomManager();
//    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        logger.debug("**** register webSocket Handler.");
        registry.addHandler(callHandler(), "/live");
    }

    /**
     * spring 入口。
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        new SpringApplication(NOne3ManyCallApp.class).run(args);
    }
}
