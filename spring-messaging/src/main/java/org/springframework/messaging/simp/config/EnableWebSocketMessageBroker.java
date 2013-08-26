/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.messaging.simp.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;


/**
 * Add this annotation to an {@code @Configuration} class to enable broker-backed
 * messaging over WebSocket using a higher-level messaging sub-protocol.
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebSocketMessageBroker
 * public class MyWebSocketConfig {
 *
 * }
 * </pre>
 * <p>
 * Customize the imported configuration by implementing the
 * {@link WebSocketMessageBrokerConfigurer} interface:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebSocketMessageBroker
 * public class MyConfiguration implements implements WebSocketMessageBrokerConfigurer {
 *
 * 	&#064;Override
 * 	public void registerStompEndpoints(StompEndpointRegistry registry) {
 * 		registry.addEndpoint("/portfolio").withSockJS();
 * 	}
 *
 * 	&#064;Bean
 * 	public void configureMessageBroker(MessageBrokerConfigurer configurer) {
 * 		configurer.enableStompBrokerRelay("/queue/", "/topic/");
 * 		configurer.setAnnotationMethodDestinationPrefixes("/app/");
 * 	}
 * }
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DelegatingWebSocketMessageBrokerConfiguration.class)
public @interface EnableWebSocketMessageBroker {
}
