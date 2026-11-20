/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
