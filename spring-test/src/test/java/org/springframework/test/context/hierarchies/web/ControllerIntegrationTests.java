/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
import org.springframework.test.context.aot.DisabledInAotMode;
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
	@ContextConfiguration(name = "root", classes = AppConfig.class),
	@ContextConfiguration(name = "dispatcher", classes = WebConfig.class)
})
@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
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
