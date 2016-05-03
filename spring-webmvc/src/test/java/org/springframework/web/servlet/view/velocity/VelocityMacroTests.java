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

package org.springframework.web.servlet.view.velocity;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;

import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.theme.FixedThemeResolver;
import org.springframework.web.servlet.view.DummyMacroRequestContext;

import static org.junit.Assert.*;

/**
 * @author Darren Davison
 * @author Juergen Hoeller
 * @since 18.06.2004
 */
public class VelocityMacroTests {

	private static final String TEMPLATE_FILE = "test.vm";

	private StaticWebApplicationContext wac;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


	@Before
	public void setUp() throws Exception {
		wac = new StaticWebApplicationContext();
		wac.setServletContext(new MockServletContext());

		final Template expectedTemplate = new Template();
		VelocityConfig vc = new VelocityConfig() {
			@Override
			public VelocityEngine getVelocityEngine() {
				return new TestVelocityEngine(TEMPLATE_FILE, expectedTemplate);
			}
		};
		wac.getDefaultListableBeanFactory().registerSingleton("velocityConfigurer", vc);
		wac.refresh();

		request = new MockHttpServletRequest();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		request.setAttribute(DispatcherServlet.THEME_RESOLVER_ATTRIBUTE, new FixedThemeResolver());
		response = new MockHttpServletResponse();
	}

	@Test
	public void exposeSpringMacroHelpers() throws Exception {
		VelocityView vv = new VelocityView() {
			@Override
			protected void mergeTemplate(Template template, Context context, HttpServletResponse response) {
				assertTrue(context.get(VelocityView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE) instanceof RequestContext);
				RequestContext rc = (RequestContext) context.get(VelocityView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE);
				BindStatus status = rc.getBindStatus("tb.name");
				assertEquals("name", status.getExpression());
				assertEquals("juergen", status.getValue());
			}
		};
		vv.setUrl(TEMPLATE_FILE);
		vv.setApplicationContext(wac);
		vv.setExposeSpringMacroHelpers(true);

		Map<String, Object> model = new HashMap<String, Object>();
		model.put("tb", new TestBean("juergen", 99));
		vv.render(model, request, response);
	}

	@Test
	public void springMacroRequestContextAttributeUsed() {
		final String helperTool = "wrongType";

		VelocityView vv = new VelocityView() {
			@Override
			protected void mergeTemplate(Template template, Context context, HttpServletResponse response) {
				fail();
			}
		};
		vv.setUrl(TEMPLATE_FILE);
		vv.setApplicationContext(wac);
		vv.setExposeSpringMacroHelpers(true);

		Map<String, Object> model = new HashMap<String, Object>();
		model.put(VelocityView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE, helperTool);

		try {
			vv.render(model, request, response);
		}
		catch (Exception ex) {
			assertTrue(ex instanceof ServletException);
			assertTrue(ex.getMessage().contains(VelocityView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE));
		}
	}

	@Test
	public void allMacros() throws Exception {
		DummyMacroRequestContext rc = new DummyMacroRequestContext(request);
		Map<String, String> msgMap = new HashMap<String, String>();
		msgMap.put("hello", "Howdy");
		msgMap.put("world", "Mundo");
		rc.setMessageMap(msgMap);
		Map<String, String> themeMsgMap = new HashMap<String, String>();
		themeMsgMap.put("hello", "Howdy!");
		themeMsgMap.put("world", "Mundo!");
		rc.setThemeMessageMap(themeMsgMap);
		rc.setContextPath("/springtest");

		TestBean tb = new TestBean("Darren", 99);
		tb.setJedi(true);
		tb.setStringArray(new String[] {"John", "Fred"});
		request.setAttribute("command", tb);

		Map<String, String> names = new HashMap<String, String>();
		names.put("Darren", "Darren Davison");
		names.put("John", "John Doe");
		names.put("Fred", "Fred Bloggs");

		VelocityConfigurer vc = new VelocityConfigurer();
		vc.setPreferFileSystemAccess(false);
		VelocityEngine ve = vc.createVelocityEngine();

		Map<String, Object> model = new HashMap<String, Object>();
		model.put("command", tb);
		model.put("springMacroRequestContext", rc);
		model.put("nameOptionMap", names);

		VelocityView view = new VelocityView();
		view.setBeanName("myView");
		view.setUrl("org/springframework/web/servlet/view/velocity/test.vm");
		view.setEncoding("UTF-8");
		view.setExposeSpringMacroHelpers(false);
		view.setVelocityEngine(ve);

		view.render(model, request, response);

		// tokenize output and ignore whitespace
		String output = response.getContentAsString();
		System.out.println(output);
		String[] tokens = StringUtils.tokenizeToStringArray(output, "\t\n");

		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].equals("NAME")) assertEquals("Darren", tokens[i + 1]);
			if (tokens[i].equals("AGE")) assertEquals("99", tokens[i + 1]);
			if (tokens[i].equals("MESSAGE")) assertEquals("Howdy Mundo", tokens[i + 1]);
			if (tokens[i].equals("DEFAULTMESSAGE")) assertEquals("hi planet", tokens[i + 1]);
			if (tokens[i].equals("THEME")) assertEquals("Howdy! Mundo!", tokens[i + 1]);
			if (tokens[i].equals("DEFAULTTHEME")) assertEquals("hi! planet!", tokens[i + 1]);
			if (tokens[i].equals("URL")) assertEquals("/springtest/aftercontext.html", tokens[i + 1]);
			if (tokens[i].equals("FORM1")) assertEquals("<input type=\"text\" id=\"name\" name=\"name\" value=\"Darren\" >", tokens[i + 1]);
			if (tokens[i].equals("FORM2")) assertEquals("<input type=\"text\" id=\"name\" name=\"name\" value=\"Darren\" class=\"myCssClass\">", tokens[i + 1]);
			if (tokens[i].equals("FORM3")) assertEquals("<textarea id=\"name\" name=\"name\" >", tokens[i + 1]);
			if (tokens[i].equals("FORM3")) assertEquals("Darren</textarea>", tokens[i + 2]);
			if (tokens[i].equals("FORM4")) assertEquals("<textarea id=\"name\" name=\"name\" rows=10 cols=30>", tokens[i + 1]);
			if (tokens[i].equals("FORM4")) assertEquals("Darren</textarea>", tokens[i + 2]);
			//TODO verify remaining output (fix whitespace)
			if (tokens[i].equals("FORM9")) assertEquals("<input type=\"password\" id=\"name\" name=\"name\" value=\"\" >", tokens[i + 1]);
			if (tokens[i].equals("FORM10")) assertEquals("<input type=\"hidden\" id=\"name\" name=\"name\" value=\"Darren\" >", tokens[i + 1]);
			if (tokens[i].equals("FORM15")) assertEquals("<input type=\"hidden\" name=\"_name\" value=\"on\"/>", tokens[i + 1]);
			if (tokens[i].equals("FORM15")) assertEquals("<input type=\"checkbox\" id=\"name\" name=\"name\" />", tokens[i + 2]);
			if (tokens[i].equals("FORM16")) assertEquals("<input type=\"hidden\" name=\"_jedi\" value=\"on\"/>", tokens[i + 1]);
			if (tokens[i].equals("FORM16")) assertEquals("<input type=\"checkbox\" id=\"jedi\" name=\"jedi\" checked=\"checked\" />", tokens[i + 2]);
		}
	}

	// SPR-5172

	@Test
	public void idContainsBraces() throws Exception {
		DummyMacroRequestContext rc = new DummyMacroRequestContext(request);
		Map<String, String> msgMap = new HashMap<String, String>();
		msgMap.put("hello", "Howdy");
		msgMap.put("world", "Mundo");
		rc.setMessageMap(msgMap);
		Map<String, String> themeMsgMap = new HashMap<String, String>();
		themeMsgMap.put("hello", "Howdy!");
		themeMsgMap.put("world", "Mundo!");
		rc.setThemeMessageMap(themeMsgMap);
		rc.setContextPath("/springtest");

		TestBean darren = new TestBean("Darren", 99);
		TestBean fred = new TestBean("Fred");
		fred.setJedi(true);
		darren.setSpouse(fred);
		darren.setJedi(true);
		darren.setStringArray(new String[] {"John", "Fred"});
		request.setAttribute("command", darren);

		Map<String, String> names = new HashMap<String, String>();
		names.put("Darren", "Darren Davison");
		names.put("John", "John Doe");
		names.put("Fred", "Fred Bloggs");

		VelocityConfigurer vc = new VelocityConfigurer();
		vc.setPreferFileSystemAccess(false);
		VelocityEngine ve = vc.createVelocityEngine();

		Map<String, Object> model = new HashMap<String, Object>();
		model.put("command", darren);
		model.put("springMacroRequestContext", rc);
		model.put("nameOptionMap", names);

		VelocityView view = new VelocityView();
		view.setBeanName("myView");
		view.setUrl("org/springframework/web/servlet/view/velocity/test-spr5172.vm");
		view.setEncoding("UTF-8");
		view.setExposeSpringMacroHelpers(false);
		view.setVelocityEngine(ve);

		view.render(model, request, response);

		// tokenize output and ignore whitespace
		String output = response.getContentAsString();
		String[] tokens = StringUtils.tokenizeToStringArray(output, "\t\n");

		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].equals("FORM1")) assertEquals("<input type=\"text\" id=\"spouses0.name\" name=\"spouses[0].name\" value=\"Fred\" >", tokens[i + 1]); //
			if (tokens[i].equals("FORM2")) assertEquals("<textarea id=\"spouses0.name\" name=\"spouses[0].name\" >", tokens[i + 1]);
			if (tokens[i].equals("FORM2")) assertEquals("Fred</textarea>", tokens[i + 2]);
			if (tokens[i].equals("FORM3")) assertEquals("<select id=\"spouses0.name\" name=\"spouses[0].name\" >", tokens[i + 1]);
			if (tokens[i].equals("FORM4")) assertEquals("<select multiple=\"multiple\" id=\"spouses\" name=\"spouses\" >", tokens[i + 1]);
			if (tokens[i].equals("FORM5")) assertEquals("<input type=\"radio\" name=\"spouses[0].name\" value=\"Darren\"", tokens[i + 1]);
			if (tokens[i].equals("FORM6")) assertEquals("<input type=\"password\" id=\"spouses0.name\" name=\"spouses[0].name\" value=\"\" >", tokens[i + 1]);
			if (tokens[i].equals("FORM7")) assertEquals("<input type=\"hidden\" id=\"spouses0.name\" name=\"spouses[0].name\" value=\"Fred\" >", tokens[i + 1]);
			if (tokens[i].equals("FORM8")) assertEquals("<input type=\"hidden\" name=\"_spouses0.name\" value=\"on\"/>", tokens[i + 1]);
			if (tokens[i].equals("FORM8")) assertEquals("<input type=\"checkbox\" id=\"spouses0.name\" name=\"spouses[0].name\" />", tokens[i + 2]);
			if (tokens[i].equals("FORM9")) assertEquals("<input type=\"hidden\" name=\"_spouses0.jedi\" value=\"on\"/>", tokens[i + 1]);
			if (tokens[i].equals("FORM9")) assertEquals("<input type=\"checkbox\" id=\"spouses0.jedi\" name=\"spouses[0].jedi\" checked=\"checked\" />", tokens[i + 2]);
		}
	}
}
