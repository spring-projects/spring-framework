/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.servlet.view.freemarker;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.view.AbstractTemplateView;
import org.springframework.web.servlet.view.DummyMacroRequestContext;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Darren Davison
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 25.01.2005
 */
@SuppressWarnings("deprecation")
public class FreeMarkerMacroTests {

	private static final String TEMPLATE_FILE = "test.ftl";

	private final StaticWebApplicationContext wac = new StaticWebApplicationContext();

	private final MockServletContext servletContext = new MockServletContext();

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	private final FreeMarkerConfigurer fc = new FreeMarkerConfigurer();

	private Path templateLoaderPath;


	@BeforeEach
	void setUp() throws Exception {
		this.templateLoaderPath = Files.createTempDirectory("servlet-").toAbsolutePath();

		fc.setTemplateLoaderPaths("classpath:/", "file://" + this.templateLoaderPath);
		fc.setServletContext(servletContext);
		fc.afterPropertiesSet();

		wac.setServletContext(servletContext);
		wac.getDefaultListableBeanFactory().registerSingleton("freeMarkerConfigurer", fc);
		wac.refresh();

		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
	}


	@Test
	void testExposeSpringMacroHelpers() throws Exception {
		FreeMarkerView fv = new FreeMarkerView() {
			@Override
			@SuppressWarnings("rawtypes")
			protected void processTemplate(Template template, SimpleHash fmModel, HttpServletResponse response)
					throws TemplateException {
				Map model = fmModel.toMap();
				assertThat(model.get(FreeMarkerView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE)).isInstanceOf(RequestContext.class);
				RequestContext rc = (RequestContext) model.get(FreeMarkerView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE);
				BindStatus status = rc.getBindStatus("tb.name");
				assertThat(status.getExpression()).isEqualTo("name");
				assertThat(status.getValue()).isEqualTo("juergen");
			}
		};
		fv.setUrl(TEMPLATE_FILE);
		fv.setApplicationContext(wac);

		Map<String, Object> model = new HashMap<>();
		model.put("tb", new TestBean("juergen", 99));
		fv.render(model, request, response);
	}

	@Test
	void testSpringMacroRequestContextAttributeUsed() {
		final String helperTool = "wrongType";

		FreeMarkerView fv = new FreeMarkerView() {
			@Override
			protected void processTemplate(Template template, SimpleHash model, HttpServletResponse response) {
				throw new AssertionError();
			}
		};
		fv.setUrl(TEMPLATE_FILE);
		fv.setApplicationContext(wac);

		Map<String, Object> model = new HashMap<>();
		model.put(FreeMarkerView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE, helperTool);

		try {
			fv.render(model, request, response);
		}
		catch (Exception ex) {
			assertThat(ex).isInstanceOf(ServletException.class);
			assertThat(ex.getMessage()).contains(FreeMarkerView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE);
		}
	}

	@Test
	void testName() throws Exception {
		assertThat(getMacroOutput("NAME")).isEqualTo("Darren");
	}

	@Test
	void testMessage() throws Exception {
		assertThat(getMacroOutput("MESSAGE")).isEqualTo("Howdy Mundo");
	}

	@Test
	void testDefaultMessage() throws Exception {
		assertThat(getMacroOutput("DEFAULTMESSAGE")).isEqualTo("hi planet");
	}

	@Test
	void testMessageArgs() throws Exception {
		assertThat(getMacroOutput("MESSAGEARGS")).isEqualTo("Howdy[World]");
	}

	@Test
	void testMessageArgsWithDefaultMessage() throws Exception {
		assertThat(getMacroOutput("MESSAGEARGSWITHDEFAULTMESSAGE")).isEqualTo("Hi");
	}

	@Test
	void testUrl() throws Exception {
		assertThat(getMacroOutput("URL")).isEqualTo("/springtest/aftercontext.html");
	}

	@Test
	void testUrlParams() throws Exception {
		assertThat(getMacroOutput("URLPARAMS")).isEqualTo("/springtest/aftercontext/bar?spam=bucket");
	}

	@Test
	void testForm1() throws Exception {
		assertThat(getMacroOutput("FORM1")).isEqualTo("<input type=\"text\" id=\"name\" name=\"name\" value=\"Darren\" >");
	}

	@Test
	void testForm2() throws Exception {
		assertThat(getMacroOutput("FORM2")).isEqualTo("<input type=\"text\" id=\"name\" name=\"name\" value=\"Darren\" class=\"myCssClass\" >");
	}

	@Test
	void testForm3() throws Exception {
		assertThat(getMacroOutput("FORM3")).isEqualTo("<textarea id=\"name\" name=\"name\" >\nDarren</textarea>");
	}

	@Test
	void testForm4() throws Exception {
		assertThat(getMacroOutput("FORM4")).isEqualTo("<textarea id=\"name\" name=\"name\" rows=10 cols=30>\nDarren</textarea>");
	}

	// TODO verify remaining output for forms 5, 6, 7, 8, and 14 (fix whitespace)

	@Test
	void testForm9() throws Exception {
		assertThat(getMacroOutput("FORM9")).isEqualTo("<input type=\"password\" id=\"name\" name=\"name\" value=\"\" >");
	}

	@Test
	void testForm10() throws Exception {
		assertThat(getMacroOutput("FORM10")).isEqualTo("<input type=\"hidden\" id=\"name\" name=\"name\" value=\"Darren\" >");
	}

	@Test
	void testForm11() throws Exception {
		assertThat(getMacroOutput("FORM11")).isEqualTo("<input type=\"text\" id=\"name\" name=\"name\" value=\"Darren\" >");
	}

	@Test
	void testForm12() throws Exception {
		assertThat(getMacroOutput("FORM12")).isEqualTo("<input type=\"hidden\" id=\"name\" name=\"name\" value=\"Darren\" >");
	}

	@Test
	void testForm13() throws Exception {
		assertThat(getMacroOutput("FORM13")).isEqualTo("<input type=\"password\" id=\"name\" name=\"name\" value=\"\" >");
	}

	@Test
	void testForm15() throws Exception {
		String output = getMacroOutput("FORM15");
		assertThat(output).as("Wrong output: " + output)
				.startsWith("<input type=\"hidden\" name=\"_name\" value=\"on\"/>");
		assertThat(output).as("Wrong output: " + output)
				.contains("<input type=\"checkbox\" id=\"name\" name=\"name\" />");
	}

	@Test
	void testForm16() throws Exception {
		String output = getMacroOutput("FORM16");
		assertThat(output).as("Wrong output: " + output)
				.startsWith("<input type=\"hidden\" name=\"_jedi\" value=\"on\"/>");
		assertThat(output).as("Wrong output: " + output)
				.contains("<input type=\"checkbox\" id=\"jedi\" name=\"jedi\" checked=\"checked\" />");
	}

	@Test
	void testForm17() throws Exception {
		assertThat(getMacroOutput("FORM17")).isEqualTo("<input type=\"text\" id=\"spouses0.name\" name=\"spouses[0].name\" value=\"Fred\" >");
	}

	@Test
	void testForm18() throws Exception {
		String output = getMacroOutput("FORM18");
		assertThat(output).as("Wrong output: " + output)
				.startsWith("<input type=\"hidden\" name=\"_spouses[0].jedi\" value=\"on\"/>");
		assertThat(output).as("Wrong output: " + output)
				.contains("<input type=\"checkbox\" id=\"spouses0.jedi\" name=\"spouses[0].jedi\" checked=\"checked\" />");
	}


	private String getMacroOutput(String name) throws Exception {
		String macro = fetchMacro(name);
		assertThat(macro).isNotNull();
		storeTemplateInTempDir(macro);

		DummyMacroRequestContext rc = new DummyMacroRequestContext(request);
		Map<String, String> msgMap = new HashMap<>();
		msgMap.put("hello", "Howdy");
		msgMap.put("world", "Mundo");
		rc.setMessageMap(msgMap);
		rc.setContextPath("/springtest");

		TestBean darren = new TestBean("Darren", 99);
		TestBean fred = new TestBean("Fred");
		fred.setJedi(true);
		darren.setSpouse(fred);
		darren.setJedi(true);
		darren.setStringArray(new String[] {"John", "Fred"});
		request.setAttribute("command", darren);

		Map<String, String> names = new HashMap<>();
		names.put("Darren", "Darren Davison");
		names.put("John", "John Doe");
		names.put("Fred", "Fred Bloggs");
		names.put("Rob&Harrop", "Rob Harrop");

		Configuration config = fc.getConfiguration();
		Map<String, Object> model = new HashMap<>();
		model.put("command", darren);
		model.put(AbstractTemplateView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE, rc);
		model.put("msgArgs", new Object[] { "World" });
		model.put("nameOptionMap", names);
		model.put("options", names.values());

		FreeMarkerView view = new FreeMarkerView();
		view.setBeanName("myView");
		view.setUrl("tmp.ftl");
		view.setExposeSpringMacroHelpers(false);
		view.setConfiguration(config);
		view.setServletContext(servletContext);

		view.render(model, request, response);

		return getOutput();
	}

	private static String fetchMacro(String name) throws Exception {
		for (String macro : loadMacros()) {
			if (macro.startsWith(name)) {
				return macro.substring(macro.indexOf("\n")).trim();
			}
		}
		return null;
	}

	private static String[] loadMacros() throws IOException {
		ClassPathResource resource = new ClassPathResource("test.ftl", FreeMarkerMacroTests.class);
		assertThat(resource.exists()).isTrue();
		String all = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream()));
		all = all.replace("\r\n", "\n");
		return StringUtils.delimitedListToStringArray(all, "\n\n");
	}

	private void storeTemplateInTempDir(String macro) throws IOException {
		Files.writeString(this.templateLoaderPath.resolve("tmp.ftl"),
				"<#import \"spring.ftl\" as spring />\n" + macro
		);
	}

	private String getOutput() throws IOException {
		String output = response.getContentAsString();
		output = output.replace("\r\n", "\n").replaceAll(" +"," ");
		return output.trim();
	}

}
