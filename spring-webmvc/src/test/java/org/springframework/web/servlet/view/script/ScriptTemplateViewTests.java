/*
 * Copyright 2002-2015 the original author or authors.
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

import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.script.Invocable;
import javax.script.ScriptEngine;

import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.support.StaticApplicationContext;

/**
 * Unit tests for {@link ScriptTemplateView}.
 *
 * @author Sebastien Deleuze
 */
public class ScriptTemplateViewTests {

	private ScriptTemplateView view;

	private ScriptTemplateConfigurer configurer;

	private StaticApplicationContext applicationContext;

	private static final String RESOURCE_LOADER_PATH = "classpath:org/springframework/web/servlet/view/script/";


	@Before
	public void setup() {
		this.configurer = new ScriptTemplateConfigurer();
		this.applicationContext = new StaticApplicationContext();
		this.applicationContext.getBeanFactory().registerSingleton("scriptTemplateConfigurer", this.configurer);
		this.view = new ScriptTemplateView();
		this.view.setUrl("sampleView");
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
		this.configurer.setCharset(StandardCharsets.ISO_8859_1);
		DirectFieldAccessor accessor = new DirectFieldAccessor(this.view);
		this.view.setApplicationContext(this.applicationContext);
		assertEquals(engine, accessor.getPropertyValue("engine"));
		assertEquals("Template", accessor.getPropertyValue("renderObject"));
		assertEquals("render", accessor.getPropertyValue("renderFunction"));
		assertEquals(StandardCharsets.ISO_8859_1, accessor.getPropertyValue("charset"));
	}

	@Test
	public void detectScriptTemplateConfigWithEngineName() {
		this.configurer.setEngineName("nashorn");
		this.configurer.setRenderObject("Template");
		this.configurer.setRenderFunction("render");
		this.configurer.setCharset(StandardCharsets.ISO_8859_1);
		this.configurer.setSharedEngine(true);
		DirectFieldAccessor accessor = new DirectFieldAccessor(this.view);
		this.view.setApplicationContext(this.applicationContext);
		assertEquals("nashorn", accessor.getPropertyValue("engineName"));
		assertNotNull(accessor.getPropertyValue("engine"));
		assertEquals("Template", accessor.getPropertyValue("renderObject"));
		assertEquals("render", accessor.getPropertyValue("renderFunction"));
		assertEquals(StandardCharsets.ISO_8859_1, accessor.getPropertyValue("charset"));
		assertEquals(true, accessor.getPropertyValue("sharedEngine"));
	}

	@Test
	public void customEngineAndRenderFunction() throws Exception {
		ScriptEngine engine = mock(InvocableScriptEngine.class);
		given(engine.get("key")).willReturn("value");
		this.view.setEngine(engine);
		this.view.setRenderFunction("render");
		this.view.setApplicationContext(this.applicationContext);
		engine = this.view.getEngine();
		assertNotNull(engine);
		assertEquals("value", engine.get("key"));
		DirectFieldAccessor accessor = new DirectFieldAccessor(this.view);
		assertNull(accessor.getPropertyValue("renderObject"));
		assertEquals("render", accessor.getPropertyValue("renderFunction"));
		assertEquals(StandardCharsets.UTF_8, accessor.getPropertyValue("charset"));
	}

	@Test
	public void nonSharedEngine() throws Exception {
		int iterations = 20;
		this.view.setEngineName("nashorn");
		this.view.setRenderFunction("render");
		this.view.setSharedEngine(false);
		this.view.setApplicationContext(this.applicationContext);
		ExecutorService executor = Executors.newFixedThreadPool(4);
		List<Future<Boolean>> results = new ArrayList<>();
		for(int i = 0; i < iterations; i++) {
			results.add(executor.submit(() -> view.getEngine() != null));
		}
		assertEquals(iterations, results.size());
		for(int i = 0; i < iterations; i++) {
			assertTrue(results.get(i).get());
		}
		executor.shutdown();
	}

	@Test
	public void nonInvocableScriptEngine() throws Exception {
		try {
			this.view.setEngine(mock(ScriptEngine.class));
		} catch(IllegalArgumentException ex) {
			assertThat(ex.getMessage(), containsString("instance"));
			return;
		}
		fail();
	}

	@Test
	public void noRenderFunctionDefined() {
		this.view.setEngine(mock(InvocableScriptEngine.class));
		try {
			this.view.setApplicationContext(this.applicationContext);
		} catch(IllegalStateException ex) {
			assertThat(ex.getMessage(), containsString("renderFunction"));
			return;
		}
		fail();
	}

	@Test
	public void engineAndEngineNameBothDefined() {
		this.view.setEngine(mock(InvocableScriptEngine.class));
		this.view.setEngineName("test");
		this.view.setRenderFunction("render");
		try {
			this.view.setApplicationContext(this.applicationContext);
		} catch(IllegalStateException ex) {
			assertThat(ex.getMessage(), containsString("engine or engineName"));
			return;
		}
		fail();
	}

	@Test
	public void engineSetterAndNonSharedEngine() {
		this.view.setEngine(mock(InvocableScriptEngine.class));
		this.view.setRenderFunction("render");
		this.view.setSharedEngine(false);
		try {
			this.view.setApplicationContext(this.applicationContext);
		} catch(IllegalStateException ex) {
			assertThat(ex.getMessage(), containsString("sharedEngine"));
			return;
		}
		fail();
	}

	@Test
	public void parentLoader() {
		this.view.setEngine(mock(InvocableScriptEngine.class));
		this.view.setRenderFunction("render");
		this.view.setResourceLoaderPath(RESOURCE_LOADER_PATH);
		this.view.setApplicationContext(this.applicationContext);
		ClassLoader classLoader = this.view.createClassLoader();
		assertNotNull(classLoader);
		URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
		assertThat(Arrays.asList(urlClassLoader.getURLs()), Matchers.hasSize(1));
		assertThat(Arrays.asList(urlClassLoader.getURLs()).get(0).toString(),
				Matchers.endsWith("org/springframework/web/servlet/view/script/"));
		this.view.setResourceLoaderPath(RESOURCE_LOADER_PATH + ",classpath:org/springframework/web/servlet/view/");
		classLoader = this.view.createClassLoader();
		assertNotNull(classLoader);
		urlClassLoader = (URLClassLoader) classLoader;
		assertThat(Arrays.asList(urlClassLoader.getURLs()), Matchers.hasSize(2));
		assertThat(Arrays.asList(urlClassLoader.getURLs()).get(0).toString(), Matchers.endsWith("org/springframework/web/servlet/view/script/"));
		assertThat(Arrays.asList(urlClassLoader.getURLs()).get(1).toString(), Matchers.endsWith("org/springframework/web/servlet/view/"));
	}


	private interface InvocableScriptEngine extends ScriptEngine, Invocable {
	}

}
