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
import java.util.Arrays;
import javax.script.Invocable;
import javax.script.ScriptEngine;

import org.hamcrest.Matchers;
import static org.junit.Assert.*;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.springframework.context.support.StaticApplicationContext;

/**
 * Unit tests for {@link ScriptTemplateConfigurer}.
 *
 * @author Sebastien Deleuze
 */
public class ScriptTemplateConfigurerTests {

	private static final String RESOURCE_LOADER_PATH = "classpath:org/springframework/web/servlet/view/script/";

	private StaticApplicationContext applicationContext;

	private ScriptTemplateConfigurer configurer;


	@Before
	public void setup() throws Exception {
		this.applicationContext = new StaticApplicationContext();
		this.configurer = new ScriptTemplateConfigurer();
		this.configurer.setResourceLoaderPath(RESOURCE_LOADER_PATH);
	}

	@Test
	public void customEngineAndRenderFunction() throws Exception {
		this.configurer.setApplicationContext(this.applicationContext);
		ScriptEngine engine = mock(InvocableScriptEngine.class);
		given(engine.get("key")).willReturn("value");
		this.configurer.setEngine(engine);
		this.configurer.setRenderFunction("render");
		this.configurer.afterPropertiesSet();

		engine = this.configurer.getEngine();
		assertNotNull(engine);
		assertEquals("value", engine.get("key"));
		assertNull(this.configurer.getRenderObject());
		assertEquals("render", this.configurer.getRenderFunction());
		assertEquals(StandardCharsets.UTF_8, this.configurer.getCharset());
	}

	@Test(expected = IllegalArgumentException.class)
	public void nonInvocableScriptEngine() throws Exception {
		this.configurer.setApplicationContext(this.applicationContext);
		ScriptEngine engine = mock(ScriptEngine.class);
		this.configurer.setEngine(engine);
	}

	@Test(expected = IllegalStateException.class)
	public void noRenderFunctionDefined() throws Exception {
		this.configurer.setApplicationContext(this.applicationContext);
		ScriptEngine engine = mock(InvocableScriptEngine.class);
		this.configurer.setEngine(engine);
		this.configurer.afterPropertiesSet();
	}

	@Test
	public void parentLoader() throws Exception {

		this.configurer.setApplicationContext(this.applicationContext);

		ClassLoader classLoader = this.configurer.createClassLoader();
		assertNotNull(classLoader);
		URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
		assertThat(Arrays.asList(urlClassLoader.getURLs()), Matchers.hasSize(1));
		assertThat(Arrays.asList(urlClassLoader.getURLs()).get(0).toString(),
				Matchers.endsWith("org/springframework/web/servlet/view/script/"));

		this.configurer.setResourceLoaderPath(RESOURCE_LOADER_PATH + ",classpath:org/springframework/web/servlet/view/");
		classLoader = this.configurer.createClassLoader();
		assertNotNull(classLoader);
		urlClassLoader = (URLClassLoader) classLoader;
		assertThat(Arrays.asList(urlClassLoader.getURLs()), Matchers.hasSize(2));
		assertThat(Arrays.asList(urlClassLoader.getURLs()).get(0).toString(),
				Matchers.endsWith("org/springframework/web/servlet/view/script/"));
		assertThat(Arrays.asList(urlClassLoader.getURLs()).get(1).toString(),
				Matchers.endsWith("org/springframework/web/servlet/view/"));
	}


	private interface InvocableScriptEngine extends ScriptEngine, Invocable {
	}

}
