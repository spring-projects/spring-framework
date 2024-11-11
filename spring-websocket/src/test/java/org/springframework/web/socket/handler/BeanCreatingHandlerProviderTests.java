/*
 * Copyright 2002-2024 the original author or authors.
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
