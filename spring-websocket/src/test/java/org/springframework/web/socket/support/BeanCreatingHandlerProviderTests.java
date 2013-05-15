/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.support;

import org.junit.Test;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.*;



/**
 * Test fixture for {@link BeanCreatingHandlerProvider}.
 *
 * @author Rossen Stoyanchev
 */
public class BeanCreatingHandlerProviderTests {


	@Test
	public void getHandlerSimpleInstantiation() {

		BeanCreatingHandlerProvider<SimpleEchoHandler> provider =
				new BeanCreatingHandlerProvider<SimpleEchoHandler>(SimpleEchoHandler.class);

		assertNotNull(provider.getHandler());
	}

	@Test
	public void getHandlerWithBeanFactory() {

		@SuppressWarnings("resource")
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(Config.class);

		BeanCreatingHandlerProvider<EchoHandler> provider =
				new BeanCreatingHandlerProvider<EchoHandler>(EchoHandler.class);
		provider.setBeanFactory(context.getBeanFactory());

		assertNotNull(provider.getHandler());
	}

	@Test(expected=BeanInstantiationException.class)
	public void getHandlerNoBeanFactory() {

		BeanCreatingHandlerProvider<EchoHandler> provider =
				new BeanCreatingHandlerProvider<EchoHandler>(EchoHandler.class);

		provider.getHandler();
	}


	@Configuration
	static class Config {

		@Bean
		public EchoService echoService() {
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

	private static class EchoService {	}

}
