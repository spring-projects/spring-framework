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

package org.springframework.test.context.hierarchies.web;

import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.hierarchies.web.ControllerIntegrationTests.AppConfig;
import org.springframework.test.context.hierarchies.web.ControllerIntegrationTests.WebConfig;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 3.2.2
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextHierarchy({
	//
	@ContextConfiguration(name = "root", classes = AppConfig.class),
	@ContextConfiguration(name = "dispatcher", classes = WebConfig.class) //
})
class ControllerIntegrationTests {

	@Configuration
	static class AppConfig {

		@Bean
		String foo() {
			return "foo";
		}
	}

	@Configuration
	static class WebConfig {

		@Bean
		String bar() {
			return "bar";
		}
	}


	// -------------------------------------------------------------------------

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private String foo;

	@Autowired
	private String bar;


	@Test
	void verifyRootWacSupport() {
		assertThat(foo).isEqualTo("foo");
		assertThat(bar).isEqualTo("bar");

		ApplicationContext parent = wac.getParent();
		assertThat(parent).isNotNull();
		assertThat(parent).isInstanceOf(WebApplicationContext.class);
		WebApplicationContext root = (WebApplicationContext) parent;
		assertThat(root.getBeansOfType(String.class).containsKey("bar")).isFalse();

		ServletContext childServletContext = wac.getServletContext();
		assertThat(childServletContext).isNotNull();
		ServletContext rootServletContext = root.getServletContext();
		assertThat(rootServletContext).isNotNull();
		assertThat(rootServletContext).isSameAs(childServletContext);

		assertThat(rootServletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).isSameAs(root);
		assertThat(childServletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).isSameAs(root);
	}

}
