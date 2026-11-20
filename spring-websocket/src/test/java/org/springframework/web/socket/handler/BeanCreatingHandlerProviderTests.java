/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.handler;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test fixture for {@link BeanCreatingHandlerProvider}.
 *
 * @author Rossen Stoyanchev
 */
class BeanCreatingHandlerProviderTests {


	@Test
	void getHandlerSimpleInstantiation() {

		BeanCreatingHandlerProvider<SimpleEchoHandler> provider =
				new BeanCreatingHandlerProvider<>(SimpleEchoHandler.class);

		assertThat(provider.getHandler()).isNotNull();
	}

	@Test
	void getHandlerWithBeanFactory() {

		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(Config.class);

		BeanCreatingHandlerProvider<EchoHandler> provider =
				new BeanCreatingHandlerProvider<>(EchoHandler.class);
		provider.setBeanFactory(context.getBeanFactory());

		assertThat(provider.getHandler()).isNotNull();
	}

	@Test
	void getHandlerNoBeanFactory() {

		BeanCreatingHandlerProvider<EchoHandler> provider =
				new BeanCreatingHandlerProvider<>(EchoHandler.class);

		assertThatExceptionOfType(BeanInstantiationException.class).isThrownBy(
				provider::getHandler);
	}


	@Configuration
	static class Config {

		@Bean
		EchoService echoService() {
			return new EchoService();
		}
	}

	public static class SimpleEchoHandler {
	}

	private static class EchoHandler {

		@SuppressWarnings("unused")
		private final EchoService service;

		@Autowired
		public EchoHandler(EchoService service) {
			this.service = service;
		}
	}

	private static class EchoService { }

}
