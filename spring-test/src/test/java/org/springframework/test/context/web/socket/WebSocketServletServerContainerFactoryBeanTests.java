/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.web.socket;

import jakarta.websocket.server.ServerContainer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that validate support for {@link ServletServerContainerFactoryBean}
 * in conjunction with {@link WebAppConfiguration @WebAppConfiguration} and the
 * Spring TestContext Framework.
 *
 * @author Sam Brannen
 * @since 4.3.1
 */
@SpringJUnitWebConfig
class WebSocketServletServerContainerFactoryBeanTests {

	@Test
	void servletServerContainerFactoryBeanSupport(@Autowired ServerContainer serverContainer) {
		assertThat(serverContainer.getDefaultMaxTextMessageBufferSize()).isEqualTo(42);
	}

	/*
	 * @Nested test class to verify that the MockServerContainerContextCustomizerFactory
	 * properly supports finding @WebAppConfiguration on an enclosing class.
	 */
	@Nested
	class NestedTests {

		@Test  // gh-29037
		void servletServerContainerFactoryBeanSupport(@Autowired ServerContainer serverContainer) {
			assertThat(serverContainer.getDefaultMaxTextMessageBufferSize()).isEqualTo(42);
		}

	}


	@Configuration
	@EnableWebSocket
	static class WebSocketConfig {

		@Bean
		ServletServerContainerFactoryBean createWebSocketContainer() {
			ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
			container.setMaxTextMessageBufferSize(42);
			return container;
		}
	}

}
