/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.web.servlet.view.freemarker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.TestBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.theme.FixedThemeResolver;
import org.springframework.web.servlet.view.DummyMacroRequestContext;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * @author Darren Davison
 * @author Juergen Hoeller
 * @since 25.01.2005
 */
public class FreeMarkerMacroTests {

	private static final String TEMPLATE_FILE = "test.ftl";

	private StaticWebApplicationContext wac;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private FreeMarkerConfigurer fc;

	@Before
	public void setUp() throws Exception {
		wac = new StaticWebApplicationContext();
		wac.setServletContext(new MockServletContext());

		// final Template expectedTemplate = new Template();
		fc = new FreeMarkerConfigurer();
		fc.setTemplateLoaderPaths(new String[] { "classpath:/", "file://" + System.getProperty("java.io.tmpdir") });
		fc.afterPropertiesSet();

		wac.getDefaultListableBeanFactory().registerSingleton("freeMarkerConfigurer", fc);
		wac.refresh();

		request = new MockHttpServletRequest();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		request.setAttribute(DispatcherServlet.THEME_RESOLVER_ATTRIBUTE, new FixedThemeResolver());
		response = new MockHttpServletResponse();
	}

	@Test
	public void testExposeSpringMacroHelpers() throws Exception {
		FreeMarkerView fv = new FreeMarkerView() {
			@Override
			protected void processTemplate(Template template, SimpleHash fmModel, HttpServletResponse response)
					throws TemplateException {
				Map model = fmModel.toMap();
				assertTrue(model.get(FreeMarkerView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE) instanceof RequestContext);
				RequestContext rc = (RequestContext) model.get(FreeMarkerView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE);
				BindStatus status = rc.getBindStatus("tb.name");
				assertEquals("name", status.getExpression());
				assertEquals("juergen", status.getValue());
			}
		};
		fv.setUrl(TEMPLATE_FILE);
		fv.setApplicationContext(wac);
		fv.setExposeSpringMacroHelpers(true);

		Map model = new HashMap();
		model.put("tb", new TestBean("juergen", 99));
		fv.render(model, request, response);
	}

	@Test
	public void testSpringMacroRequestContextAttributeUsed() {
		final String helperTool = "wrongType";

		FreeMarkerView fv = new FreeMarkerView() {
			@Override
			protected void processTemplate(Template template, SimpleHash model, HttpServletResponse response) {
				fail();
			}
		};
		fv.setUrl(TEMPLATE_FILE);
		fv.setApplicationContext(wac);
		fv.setExposeSpringMacroHelpers(true);

		Map model = new HashMap();
		model.put(FreeMarkerView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE, helperTool);

		try {
			fv.render(model, request, response);
		} catch (Exception ex) {
			assertTrue(ex instanceof ServletException);
			assertTrue(ex.getMessage().contains(FreeMarkerView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE));
		}
	}

	@Test
	public void testName() throws Exception {
		assertEquals("Darren", getMacroOutput("NAME"));
	}

	@Test
	public void testAge() throws Exception {
		assertEquals("99", getMacroOutput("AGE"));
	}

	@Test
	public void testMessage() throws Exception {
		assertEquals("Howdy Mundo", getMacroOutput("MESSAGE"));
	}

	@Test
	public void testDefaultMessage() throws Exception {
		assertEquals("hi planet", getMacroOutput("DEFAULTMESSAGE"));
	}

	@Test
	public void testMessageArgs() throws Exception {
		assertEquals("Howdy[World]", getMacroOutput("MESSAGEARGS"));
	}

	@Test
	public void testMessageArgsWithDefaultMessage() throws Exception {
		assertEquals("Hi", getMacroOutput("MESSAGEARGSWITHDEFAULTMESSAGE"));
	}

	@Test
	public void testTheme() throws Exception {
		assertEquals("Howdy! Mundo!", getMacroOutput("THEME"));
	}

	@Test
	public void testDefaultTheme() throws Exception {
		assertEquals("hi! planet!", getMacroOutput("DEFAULTTHEME"));
	}

	@Test
	public void testThemeArgs() throws Exception {
		assertEquals("Howdy![World]", getMacroOutput("THEMEARGS"));
	}

	@Test
	public void testThemeArgsWithDefaultMessage() throws Exception {
		assertEquals("Hi!", getMacroOutput("THEMEARGSWITHDEFAULTMESSAGE"));
	}

	@Test
	public void testUrl() throws Exception {
		assertEquals("/springtest/aftercontext.html", getMacroOutput("URL"));
	}

	@Test
	public void testUrlParams() throws Exception {
		assertEquals("/springtest/aftercontext/bar?spam=bucket", getMacroOutput("URLPARAMS"));
	}

	@Test
	public void testForm1() throws Exception {
		assertEquals("<input type=\"text\" id=\"name\" name=\"name\" value=\"Darren\"     >", getMacroOutput("FORM1"));
	}

	@Test
	public void testForm2() throws Exception {
		assertEquals("<input type=\"text\" id=\"name\" name=\"name\" value=\"Darren\" class=\"myCssClass\"    >",
				getMacroOutput("FORM2"));
	}

	@Test
	public void testForm3() throws Exception {
		assertEquals("<textarea id=\"name\" name=\"name\" >Darren</textarea>", getMacroOutput("FORM3"));
	}

	@Test
	public void testForm4() throws Exception {
		assertEquals("<textarea id=\"name\" name=\"name\" rows=10 cols=30>Darren</textarea>", getMacroOutput("FORM4"));
	}

	// TODO verify remaining output (fix whitespace)
	@Test
	public void testForm9() throws Exception {
		assertEquals("<input type=\"password\" id=\"name\" name=\"name\" value=\"\"     >", getMacroOutput("FORM9"));
	}

	@Test
	public void testForm10() throws Exception {
		assertEquals("<input type=\"hidden\" id=\"name\" name=\"name\" value=\"Darren\"     >",
				getMacroOutput("FORM10"));
	}

	@Test
	public void testForm11() throws Exception {
		assertEquals("<input type=\"text\" id=\"name\" name=\"name\" value=\"Darren\"     >", getMacroOutput("FORM11"));
	}

	@Test
	public void testForm12() throws Exception {
		assertEquals("<input type=\"hidden\" id=\"name\" name=\"name\" value=\"Darren\"     >",
				getMacroOutput("FORM12"));
	}

	@Test
	public void testForm13() throws Exception {
		assertEquals("<input type=\"password\" id=\"name\" name=\"name\" value=\"\"     >", getMacroOutput("FORM13"));
	}

	@Test
	public void testForm15() throws Exception {
		String output = getMacroOutput("FORM15");
		assertTrue("Wrong output: " + output, output.startsWith("<input type=\"hidden\" name=\"_name\" value=\"on\"/>"));
		assertTrue("Wrong output: " + output, output.contains("<input type=\"checkbox\" id=\"name\" name=\"name\" />"));
	}

	@Test
	public void testForm16() throws Exception {
		String output = getMacroOutput("FORM16");
		assertTrue("Wrong output: " + output, output.startsWith("<input type=\"hidden\" name=\"_jedi\" value=\"on\"/>"));
		assertTrue("Wrong output: " + output, output.contains("<input type=\"checkbox\" id=\"jedi\" name=\"jedi\" checked=\"checked\" />"));
	}

	@Test
	public void testForm17() throws Exception {
		assertEquals("<input type=\"text\" id=\"spouses0.name\" name=\"spouses[0].name\" value=\"Fred\"     >", getMacroOutput("FORM17"));
	}

	private String getMacroOutput(String name) throws Exception {

		String macro = fetchMacro(name);
		assertNotNull(macro);

		FileSystemResource resource = new FileSystemResource(System.getProperty("java.io.tmpdir") + "/tmp.ftl");
		FileCopyUtils.copy("<#import \"spring.ftl\" as spring />\n" + macro, new FileWriter(resource.getPath()));

		DummyMacroRequestContext rc = new DummyMacroRequestContext(request);
		Map msgMap = new HashMap();
		msgMap.put("hello", "Howdy");
		msgMap.put("world", "Mundo");
		rc.setMessageMap(msgMap);
		Map themeMsgMap = new HashMap();
		themeMsgMap.put("hello", "Howdy!");
		themeMsgMap.put("world", "Mundo!");
		rc.setThemeMessageMap(themeMsgMap);
		rc.setContextPath("/springtest");

		TestBean tb = new TestBean("Darren", 99);
		tb.setSpouse(new TestBean("Fred"));
		tb.setJedi(true);
		request.setAttribute("command", tb);

		HashMap names = new HashMap();
		names.put("Darren", "Darren Davison");
		names.put("John", "John Doe");
		names.put("Fred", "Fred Bloggs");
		names.put("Rob&Harrop", "Rob Harrop");

		Configuration config = fc.getConfiguration();
		Map model = new HashMap();
		model.put("command", tb);
		model.put("springMacroRequestContext", rc);
		model.put("msgArgs", new Object[] { "World" });
		model.put("nameOptionMap", names);
		model.put("options", names.values());

		FreeMarkerView view = new FreeMarkerView();
		view.setBeanName("myView");
		view.setUrl("tmp.ftl");
		view.setExposeSpringMacroHelpers(false);
		view.setConfiguration(config);
		view.setServletContext(new MockServletContext());

		view.render(model, request, response);

		// tokenize output and ignore whitespace
		String output = response.getContentAsString();
		return output.trim();
	}

	private String fetchMacro(String name) throws Exception {
		ClassPathResource resource = new ClassPathResource("test.ftl", getClass());
		assertTrue(resource.exists());
		String all = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream()));
		String[] macros = StringUtils.delimitedListToStringArray(all, "\n\n");
		for (String macro : macros) {
			if (macro.startsWith(name)) {
				return macro.substring(macro.indexOf("\n")).trim();
			}
		}
		return null;
	}

}
