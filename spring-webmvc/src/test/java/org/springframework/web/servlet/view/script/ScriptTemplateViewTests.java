/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.view.script;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.script.Invocable;
import javax.script.ScriptEngine;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link ScriptTemplateView}.
 *
 * @author Sebastien Deleuze
 */
public class ScriptTemplateViewTests {

	private ScriptTemplateView view;

	private ScriptTemplateConfigurer configurer;

	private StaticWebApplicationContext wac;


	@Before
	public void setup() {
		this.configurer = new ScriptTemplateConfigurer();
		this.wac = new StaticWebApplicationContext();
		this.wac.getBeanFactory().registerSingleton("scriptTemplateConfigurer", this.configurer);
		this.view = new ScriptTemplateView();
	}

	@Test
	public void missingScriptTemplateConfig() throws Exception {
		try {
			this.view.setApplicationContext(new StaticApplicationContext());
		}
		catch (ApplicationContextException ex) {
			assertTrue(ex.getMessage().contains("ScriptTemplateConfig"));
			return;
		}
		fail();
	}

	@Test
	public void detectScriptTemplateConfigWithEngine() {
		InvocableScriptEngine engine = mock(InvocableScriptEngine.class);
		this.configurer.setEngine(engine);
		this.configurer.setRenderObject("Template");
		this.configurer.setRenderFunction("render");
		this.configurer.setContentType(MediaType.TEXT_PLAIN_VALUE);
		this.configurer.setCharset(Charset.forName("ISO-8859-1"));
		this.configurer.setSharedEngine(true);

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.view);
		this.view.setApplicationContext(this.wac);
		assertEquals(engine, accessor.getPropertyValue("engine"));
		assertEquals("Template", accessor.getPropertyValue("renderObject"));
		assertEquals("render", accessor.getPropertyValue("renderFunction"));
		assertEquals(MediaType.TEXT_PLAIN_VALUE, accessor.getPropertyValue("contentType"));
		assertEquals(Charset.forName("ISO-8859-1"), accessor.getPropertyValue("charset"));
		assertEquals(true, accessor.getPropertyValue("sharedEngine"));
	}

	@Test
	public void detectScriptTemplateConfigWithEngineName() {
		this.configurer.setEngineName("nashorn");
		this.configurer.setRenderObject("Template");
		this.configurer.setRenderFunction("render");

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.view);
		this.view.setApplicationContext(this.wac);
		assertEquals("nashorn", accessor.getPropertyValue("engineName"));
		assertNotNull(accessor.getPropertyValue("engine"));
		assertEquals("Template", accessor.getPropertyValue("renderObject"));
		assertEquals("render", accessor.getPropertyValue("renderFunction"));
		assertEquals(MediaType.TEXT_HTML_VALUE, accessor.getPropertyValue("contentType"));
		assertEquals(Charset.forName("UTF-8"), accessor.getPropertyValue("charset"));
	}

	@Test
	public void customEngineAndRenderFunction() throws Exception {
		ScriptEngine engine = mock(InvocableScriptEngine.class);
		given(engine.get("key")).willReturn("value");
		this.view.setEngine(engine);
		this.view.setRenderFunction("render");
		this.view.setApplicationContext(this.wac);
		engine = this.view.getEngine();
		assertNotNull(engine);
		assertEquals("value", engine.get("key"));
		DirectFieldAccessor accessor = new DirectFieldAccessor(this.view);
		assertNull(accessor.getPropertyValue("renderObject"));
		assertEquals("render", accessor.getPropertyValue("renderFunction"));
		assertEquals(Charset.forName("UTF-8"), accessor.getPropertyValue("charset"));
	}

	@Test
	public void nonSharedEngine() throws Exception {
		int iterations = 20;
		this.view.setEngineName("nashorn");
		this.view.setRenderFunction("render");
		this.view.setSharedEngine(false);
		this.view.setApplicationContext(this.wac);
		ExecutorService executor = Executors.newFixedThreadPool(4);
		List<Future<Boolean>> results = new ArrayList<>();
		for (int i = 0; i < iterations; i++) {
			results.add(executor.submit(() -> view.getEngine() != null));
		}
		assertEquals(iterations, results.size());
		for (int i = 0; i < iterations; i++) {
			assertTrue(results.get(i).get());
		}
		executor.shutdown();
	}

	@Test
	public void nonInvocableScriptEngine() throws Exception {
		try {
			this.view.setEngine(mock(ScriptEngine.class));
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			assertThat(ex.getMessage(), containsString("instance"));
			return;
		}
	}

	@Test
	public void noRenderFunctionDefined() {
		this.view.setEngine(mock(InvocableScriptEngine.class));
		try {
			this.view.setApplicationContext(this.wac);
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			assertThat(ex.getMessage(), containsString("renderFunction"));
		}
	}

	@Test
	public void engineAndEngineNameBothDefined() {
		this.view.setEngine(mock(InvocableScriptEngine.class));
		this.view.setEngineName("test");
		this.view.setRenderFunction("render");
		try {
			this.view.setApplicationContext(this.wac);
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			assertThat(ex.getMessage(), containsString("'engine' or 'engineName'"));
		}
	}

	@Test
	public void engineSetterAndNonSharedEngine() {
		this.view.setEngine(mock(InvocableScriptEngine.class));
		this.view.setRenderFunction("render");
		this.view.setSharedEngine(false);
		try {
			this.view.setApplicationContext(this.wac);
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			assertThat(ex.getMessage(), containsString("sharedEngine"));
			return;
		}
		fail();
	}

	@Test // SPR-14210
	public void resourceLoaderPath() throws Exception {
		MockServletContext servletContext = new MockServletContext();
		this.wac.setServletContext(servletContext);
		this.wac.refresh();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.wac);
		MockHttpServletResponse response = new MockHttpServletResponse();
		Map<String, Object> model = new HashMap<String, Object>();
		InvocableScriptEngine engine = mock(InvocableScriptEngine.class);
		when(engine.invokeFunction(any(), any(), any(), any())).thenReturn("foo");
		this.view.setEngine(engine);
		this.view.setRenderFunction("render");
		this.view.setApplicationContext(this.wac);
		this.view.setUrl("org/springframework/web/servlet/view/script/empty.txt");
		this.view.render(model, request, response);
		assertEquals("foo", response.getContentAsString());

		response = new MockHttpServletResponse();
		this.view.setResourceLoaderPath("classpath:org/springframework/web/servlet/view/script/");
		this.view.setUrl("empty.txt");
		this.view.render(model, request, response);
		assertEquals("foo", response.getContentAsString());

		response = new MockHttpServletResponse();
		this.view.setResourceLoaderPath("classpath:org/springframework/web/servlet/view/script");
		this.view.setUrl("empty.txt");
		this.view.render(model, request, response);
		assertEquals("foo", response.getContentAsString());
	}

	@Test // SPR-13379
	public void contentType() throws Exception {
		MockServletContext servletContext = new MockServletContext();
		this.wac.setServletContext(servletContext);
		this.wac.refresh();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.wac);
		MockHttpServletResponse response = new MockHttpServletResponse();
		Map<String, Object> model = new HashMap<String, Object>();
		this.view.setEngine(mock(InvocableScriptEngine.class));
		this.view.setRenderFunction("render");
		this.view.setResourceLoaderPath("classpath:org/springframework/web/servlet/view/script/");
		this.view.setUrl("empty.txt");
		this.view.setApplicationContext(this.wac);

		this.view.render(model, request, response);
		assertEquals(MediaType.TEXT_HTML_VALUE + ";charset=" +
				Charset.forName("UTF-8"), response.getHeader(HttpHeaders.CONTENT_TYPE));

		response = new MockHttpServletResponse();
		this.view.setContentType(MediaType.TEXT_PLAIN_VALUE);
		this.view.render(model, request, response);
		assertEquals(MediaType.TEXT_PLAIN_VALUE + ";charset=" +
				Charset.forName("UTF-8"), response.getHeader(HttpHeaders.CONTENT_TYPE));

		response = new MockHttpServletResponse();
		this.view.setCharset(Charset.forName("ISO-8859-1"));
		this.view.render(model, request, response);
		assertEquals(MediaType.TEXT_PLAIN_VALUE + ";charset=" +
				Charset.forName("ISO-8859-1"), response.getHeader(HttpHeaders.CONTENT_TYPE));

	}


	private interface InvocableScriptEngine extends ScriptEngine, Invocable {
	}

}
