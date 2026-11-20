/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.setup;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Tests for {@link DefaultMockMvcBuilder}.
 *
 * @author Rob Winch
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @author Stephane Nicoll
 */
class DefaultMockMvcBuilderTests {

	private final MockServletContext servletContext = new MockServletContext();

	@Test
	void webAppContextSetupWithNullWac() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				webAppContextSetup(null))
			.withMessage("WebApplicationContext is required");
	}

	@Test
	void webAppContextSetupWithNullServletContext() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				webAppContextSetup(new StubWebApplicationContext(null)))
			.withMessage("WebApplicationContext must have a ServletContext");
	}

	/**
	 * See SPR-12553 and SPR-13075.
	 */
	@Test
	void rootWacServletContainerAttributePreviouslySet() {
		StubWebApplicationContext child = new StubWebApplicationContext(this.servletContext);
		this.servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, child);

		DefaultMockMvcBuilder builder = webAppContextSetup(child);
		assertThat(WebApplicationContextUtils.getRequiredWebApplicationContext(this.servletContext)).isSameAs(builder.initWebAppContext());
	}

	/**
	 * See SPR-12553 and SPR-13075.
	 */
	@Test
	void rootWacServletContainerAttributePreviouslySetWithContextHierarchy() {
		StubWebApplicationContext root = new StubWebApplicationContext(this.servletContext);

		this.servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, root);

		StaticWebApplicationContext child = new StaticWebApplicationContext();
		child.setParent(root);
		child.setServletContext(this.servletContext);

		DefaultMockMvcBuilder builder = webAppContextSetup(child);
		assertThat(WebApplicationContextUtils.getRequiredWebApplicationContext(this.servletContext)).isSameAs(builder.initWebAppContext().getParent());
	}

	/**
	 * See SPR-12553 and SPR-13075.
	 */
	@Test
	void rootWacServletContainerAttributeNotPreviouslySet() {
		StubWebApplicationContext root = new StubWebApplicationContext(this.servletContext);
		DefaultMockMvcBuilder builder = webAppContextSetup(root);
		WebApplicationContext wac = builder.initWebAppContext();
		assertThat(wac).isSameAs(root);
		assertThat(WebApplicationContextUtils.getRequiredWebApplicationContext(this.servletContext)).isSameAs(root);
	}

	/**
	 * See SPR-12553 and SPR-13075.
	 */
	@Test
	void rootWacServletContainerAttributeNotPreviouslySetWithContextHierarchy() {
		StaticApplicationContext ear = new StaticApplicationContext();
		StaticWebApplicationContext root = new StaticWebApplicationContext();
		root.setParent(ear);
		root.setServletContext(this.servletContext);
		StaticWebApplicationContext dispatcher = new StaticWebApplicationContext();
		dispatcher.setParent(root);
		dispatcher.setServletContext(this.servletContext);

		DefaultMockMvcBuilder builder = webAppContextSetup(dispatcher);
		WebApplicationContext wac = builder.initWebAppContext();

		assertThat(wac).isSameAs(dispatcher);
		assertThat(wac.getParent()).isSameAs(root);
		assertThat(wac.getParent().getParent()).isSameAs(ear);
		assertThat(WebApplicationContextUtils.getRequiredWebApplicationContext(this.servletContext)).isSameAs(root);
	}

	/**
	 * See /SPR-14277
	 */
	@Test
	void dispatcherServletCustomizer() {
		StubWebApplicationContext root = new StubWebApplicationContext(this.servletContext);
		DefaultMockMvcBuilder builder = webAppContextSetup(root);
		builder.addDispatcherServletCustomizer(ds -> ds.setContextId("test-id"));
		builder.dispatchOptions(true);
		MockMvc mvc = builder.build();
		DispatcherServlet ds = (DispatcherServlet) new DirectFieldAccessor(mvc)
				.getPropertyValue("servlet");
		assertThat(ds.getContextId()).isEqualTo("test-id");
	}

	@Test
	void dispatcherServletCustomizerProcessedInOrder() {
		StubWebApplicationContext root = new StubWebApplicationContext(this.servletContext);
		DefaultMockMvcBuilder builder = webAppContextSetup(root);
		builder.addDispatcherServletCustomizer(ds -> ds.setContextId("test-id"));
		builder.addDispatcherServletCustomizer(ds -> ds.setContextId("override-id"));
		builder.dispatchOptions(true);
		MockMvc mvc = builder.build();
		DispatcherServlet ds = (DispatcherServlet) new DirectFieldAccessor(mvc)
				.getPropertyValue("servlet");
		assertThat(ds.getContextId()).isEqualTo("override-id");
	}

}
