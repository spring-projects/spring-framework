/*
 * Copyright 2002-2019 the original author or authors.
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 3.2.2
 */
@ContextHierarchy(@ContextConfiguration)
public class DispatcherWacRootWacEarTests extends RootWacEarTests {

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private String ear;

	@Autowired
	private String root;

	@Autowired
	private String dispatcher;


	@Disabled("Superseded by verifyDispatcherWacConfig()")
	@Test
	@Override
	void verifyEarConfig() {
		/* no-op */
	}

	@Disabled("Superseded by verifyDispatcherWacConfig()")
	@Test
	@Override
	void verifyRootWacConfig() {
		/* no-op */
	}

	@Test
	void verifyDispatcherWacConfig() {
		ApplicationContext parent = wac.getParent();
		assertThat(parent).isNotNull();
		boolean condition = parent instanceof WebApplicationContext;
		assertThat(condition).isTrue();

		ApplicationContext grandParent = parent.getParent();
		assertThat(grandParent).isNotNull();
		boolean condition1 = grandParent instanceof WebApplicationContext;
		assertThat(condition1).isFalse();

		ServletContext dispatcherServletContext = wac.getServletContext();
		assertThat(dispatcherServletContext).isNotNull();
		ServletContext rootServletContext = ((WebApplicationContext) parent).getServletContext();
		assertThat(rootServletContext).isNotNull();
		assertThat(rootServletContext).isSameAs(dispatcherServletContext);

		assertThat(rootServletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).isSameAs(parent);
		assertThat(dispatcherServletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).isSameAs(parent);

		assertThat(ear).isEqualTo("ear");
		assertThat(root).isEqualTo("root");
		assertThat(dispatcher).isEqualTo("dispatcher");
	}

}
