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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.servlet.ServletContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContextException;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.WebApplicationContext;


/**
 * Unit tests for {@link ScriptTemplateView}.
 *
 * @author Sebastien Deleuze
 */
public class ScriptTemplateViewTests {

	private WebApplicationContext webAppContext;

	private ServletContext servletContext;

	@Before
	public void setup() {
		this.webAppContext = mock(WebApplicationContext.class);
		this.servletContext = new MockServletContext();
		this.servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.webAppContext);
	}

	@Test
	public void missingScriptTemplateConfig() throws Exception {
		ScriptTemplateView view = new ScriptTemplateView();
		given(this.webAppContext.getBeansOfType(ScriptTemplateConfig.class, true, false))
				.willReturn(new HashMap<String, ScriptTemplateConfig>());

		view.setUrl("sampleView");
		try {
			view.setApplicationContext(this.webAppContext);
			fail();
		}
		catch (ApplicationContextException ex) {
			assertTrue(ex.getMessage().contains("ScriptTemplateConfig"));
		}
	}

	@Test
	public void dectectScriptTemplateConfig() throws Exception {
		InvocableScriptEngine engine = mock(InvocableScriptEngine.class);
		ScriptTemplateConfigurer configurer = new ScriptTemplateConfigurer();
		configurer.setEngine(engine);
		configurer.setRenderObject("Template");
		configurer.setRenderFunction("render");
		configurer.setCharset(StandardCharsets.ISO_8859_1);
		Map<String, ScriptTemplateConfig> configMap =  new HashMap<String, ScriptTemplateConfig>();
		configMap.put("scriptTemplateConfigurer", configurer);
		ScriptTemplateView view = new ScriptTemplateView();
		given(this.webAppContext.getBeansOfType(ScriptTemplateConfig.class, true, false)).willReturn(configMap);

		DirectFieldAccessor accessor = new DirectFieldAccessor(view);
		view.setUrl("sampleView");
		view.setApplicationContext(this.webAppContext);
		assertEquals(engine, accessor.getPropertyValue("engine"));
		assertEquals(StandardCharsets.ISO_8859_1, accessor.getPropertyValue("charset"));
		assertEquals("Template", accessor.getPropertyValue("renderObject"));
		assertEquals("render", accessor.getPropertyValue("renderFunction"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void nonInvocableScriptEngine() throws Exception {
		ScriptEngine engine = mock(ScriptEngine.class);
		ScriptTemplateConfigurer configurer = new ScriptTemplateConfigurer();
		configurer.setEngine(engine);
		fail();
	}

	private interface InvocableScriptEngine extends ScriptEngine, Invocable {
	}

}
