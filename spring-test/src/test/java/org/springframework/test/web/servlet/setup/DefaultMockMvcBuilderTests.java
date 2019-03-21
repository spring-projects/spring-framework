/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.web.servlet.setup;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.DispatcherServlet;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

/**
 * Tests for {@link DefaultMockMvcBuilder}.
 *
 * @author Rob Winch
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @author Stephane Nicoll
 */
public class DefaultMockMvcBuilderTests {

	private final MockServletContext servletContext = new MockServletContext();

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void webAppContextSetupWithNullWac() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(equalTo("WebApplicationContext is required"));
		webAppContextSetup(null);
	}

	@Test
	public void webAppContextSetupWithNullServletContext() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(equalTo("WebApplicationContext must have a ServletContext"));
		webAppContextSetup(new StubWebApplicationContext(null));
	}

	/**
	 * See SPR-12553 and SPR-13075.
	 */
	@Test
	public void rootWacServletContainerAttributePreviouslySet() {
		StubWebApplicationContext child = new StubWebApplicationContext(this.servletContext);
		this.servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, child);

		DefaultMockMvcBuilder builder = webAppContextSetup(child);
		assertSame(builder.initWebAppContext(),
			WebApplicationContextUtils.getRequiredWebApplicationContext(this.servletContext));
	}

	/**
	 * See SPR-12553 and SPR-13075.
	 */
	@Test
	public void rootWacServletContainerAttributePreviouslySetWithContextHierarchy() {
		StubWebApplicationContext root = new StubWebApplicationContext(this.servletContext);

		this.servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, root);

		StaticWebApplicationContext child = new StaticWebApplicationContext();
		child.setParent(root);
		child.setServletContext(this.servletContext);

		DefaultMockMvcBuilder builder = webAppContextSetup(child);
		assertSame(builder.initWebAppContext().getParent(),
			WebApplicationContextUtils.getRequiredWebApplicationContext(this.servletContext));
	}

	/**
	 * See SPR-12553 and SPR-13075.
	 */
	@Test
	public void rootWacServletContainerAttributeNotPreviouslySet() {
		StubWebApplicationContext root = new StubWebApplicationContext(this.servletContext);
		DefaultMockMvcBuilder builder = webAppContextSetup(root);
		WebApplicationContext wac = builder.initWebAppContext();
		assertSame(root, wac);
		assertSame(root, WebApplicationContextUtils.getRequiredWebApplicationContext(this.servletContext));
	}

	/**
	 * See SPR-12553 and SPR-13075.
	 */
	@Test
	public void rootWacServletContainerAttributeNotPreviouslySetWithContextHierarchy() {
		StaticApplicationContext ear = new StaticApplicationContext();
		StaticWebApplicationContext root = new StaticWebApplicationContext();
		root.setParent(ear);
		root.setServletContext(this.servletContext);
		StaticWebApplicationContext dispatcher = new StaticWebApplicationContext();
		dispatcher.setParent(root);
		dispatcher.setServletContext(this.servletContext);

		DefaultMockMvcBuilder builder = webAppContextSetup(dispatcher);
		WebApplicationContext wac = builder.initWebAppContext();

		assertSame(dispatcher, wac);
		assertSame(root, wac.getParent());
		assertSame(ear, wac.getParent().getParent());
		assertSame(root, WebApplicationContextUtils.getRequiredWebApplicationContext(this.servletContext));
	}

	/**
	 * See /SPR-14277
	 */
	@Test
	public void dispatcherServletCustomizer() {
		StubWebApplicationContext root = new StubWebApplicationContext(this.servletContext);
		DefaultMockMvcBuilder builder = webAppContextSetup(root);
		builder.addDispatcherServletCustomizer(ds -> ds.setContextId("test-id"));
		builder.dispatchOptions(true);
		MockMvc mvc = builder.build();
		DispatcherServlet ds = (DispatcherServlet) new DirectFieldAccessor(mvc)
				.getPropertyValue("servlet");
		assertEquals("test-id", ds.getContextId());
	}

	@Test
	public void dispatcherServletCustomizerProcessedInOrder() {
		StubWebApplicationContext root = new StubWebApplicationContext(this.servletContext);
		DefaultMockMvcBuilder builder = webAppContextSetup(root);
		builder.addDispatcherServletCustomizer(ds -> ds.setContextId("test-id"));
		builder.addDispatcherServletCustomizer(ds -> ds.setContextId("override-id"));
		builder.dispatchOptions(true);
		MockMvc mvc = builder.build();
		DispatcherServlet ds = (DispatcherServlet) new DirectFieldAccessor(mvc)
				.getPropertyValue("servlet");
		assertEquals("override-id", ds.getContextId());
	}

}
