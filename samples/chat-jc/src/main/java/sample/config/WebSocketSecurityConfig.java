/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package sample.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

/**
 * @author Rob Winch
 */
@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
            // matches any destination that starts with /app/
            .simpDestMatchers("/app/**").authenticated()
            // matches any destination for SimpMessageType.SUBSCRIBE that starts with /user/ or /topic/friends/
            .simpDestSubscribeMatchers("/user/**","/topic/friends/*").authenticated()

            // (i.e. NOT SimpMessageType.SUBSCRIBE and SimpMessageType.MESSAGE)
            .nullDestMatcher().authenticated()

            // (i.e. cannot send messages directly to /topic/, /queue/, /user/)
            // (i.e. cannot subscribe to /topic/messages/* to get messages sent to /topic/messages-user<id>)
            .anyMessage().denyAll();
    }
}