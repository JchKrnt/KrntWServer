/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package org.kurento.tutorial.one2manycall;

import org.kurento.client.KurentoClient;
import org.kurento.tutorial.n_one2manycall.NCallHandler;
import org.kurento.tutorial.n_one2manycall.RoomManager;
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
 * Video call 1 to N demo (main).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.0
 */
@Configuration
@EnableWebSocket
@EnableAutoConfiguration
public class One2ManyCallApp implements WebSocketConfigurer {

    public static final Logger logger = LoggerFactory.getLogger("N_Kurento");
    final static String DEFAULT_KMS_WS_URI = "ws://localhost:8888/kurento";

    @Bean
    public CallHandler callHandler() {
        return new CallHandler();
    }

    @Bean
    public NCallHandler callNHandler() {
        return new NCallHandler();
    }

    @Bean
    public KurentoClient kurentoClient() {
        return KurentoClient.create(System.getProperty("kms.ws.uri",
                DEFAULT_KMS_WS_URI));
    }

    @Bean
    public RoomManager roomManager() {
        return new RoomManager();
    }

    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(callNHandler(), "/live");
        registry.addHandler(callHandler(), "/call");

    }

    public static void main(String[] args) throws Exception {
        new SpringApplication(One2ManyCallApp.class).run(args);
    }

}
