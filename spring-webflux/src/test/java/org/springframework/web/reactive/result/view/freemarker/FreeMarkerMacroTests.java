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

package org.springframework.web.reactive.result.view.freemarker;

import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import freemarker.template.Configuration;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.ModelMap;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.result.view.DummyMacroRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Darren Davison
 * @author Juergen Hoeller
 * @author Issam El-atif
 */
public class FreeMarkerMacroTests {

	private MockServerWebExchange exchange;

	private Configuration freeMarkerConfig;

	@Before
	public void setUp() throws Exception {
		this.exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path"));
		FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
		configurer.setTemplateLoaderPaths("classpath:/", "file://" + System.getProperty("java.io.tmpdir"));
		this.freeMarkerConfig = configurer.createConfiguration();
	}

	@Test
	public void testName() throws Exception {
		assertThat(getMacroOutput("NAME")).isEqualTo("Darren");
	}

	@Test
	public void testAge() throws Exception {
		assertThat(getMacroOutput("AGE")).isEqualTo("99");
	}

	@Test
	public void testMessage() throws Exception {
		assertThat(getMacroOutput("MESSAGE")).isEqualTo("Howdy Mundo");
	}

	@Test
	public void testDefaultMessage() throws Exception {
		assertThat(getMacroOutput("DEFAULTMESSAGE")).isEqualTo("hi planet");
	}

	@Test
	public void testMessageArgs() throws Exception {
		assertThat(getMacroOutput("MESSAGEARGS")).isEqualTo("Howdy[World]");
	}

	@Test
	public void testMessageArgsWithDefaultMessage() throws Exception {
		assertThat(getMacroOutput("MESSAGEARGSWITHDEFAULTMESSAGE")).isEqualTo("Hi");
	}

	@Test
	public void testUrl() throws Exception {
		assertThat(getMacroOutput("URL")).isEqualTo("/springtest/aftercontext.html");
	}

	@Test
	public void testUrlParams() throws Exception {
		assertThat(getMacroOutput("URLPARAMS")).isEqualTo("/springtest/aftercontext/bar?spam=bucket");
	}

	@Test
	public void testForm1() throws Exception {
		assertThat(getMacroOutput("FORM1")).isEqualTo("<input type=\"text\" id=\"name\" name=\"name\" value=\"Darren\"     >");
	}

	@Test
	public void testForm2() throws Exception {
		assertThat(getMacroOutput("FORM2")).isEqualTo("<input type=\"text\" id=\"name\" name=\"name\" value=\"Darren\" class=\"myCssClass\"    >");
	}

	@Test
	public void testForm3() throws Exception {
		assertThat(getMacroOutput("FORM3")).isEqualTo("<textarea id=\"name\" name=\"name\" >\nDarren</textarea>");
	}

	@Test
	public void testForm4() throws Exception {
		assertThat(getMacroOutput("FORM4")).isEqualTo("<textarea id=\"name\" name=\"name\" rows=10 cols=30>\nDarren</textarea>");
	}

	@Test
	public void testForm9() throws Exception {
		assertThat(getMacroOutput("FORM9")).isEqualTo("<input type=\"password\" id=\"name\" name=\"name\" value=\"\"     >");
	}

	@Test
	public void testForm10() throws Exception {
		assertThat(getMacroOutput("FORM10")).isEqualTo("<input type=\"hidden\" id=\"name\" name=\"name\" value=\"Darren\"     >");
	}

	@Test
	public void testForm11() throws Exception {
		assertThat(getMacroOutput("FORM11")).isEqualTo("<input type=\"text\" id=\"name\" name=\"name\" value=\"Darren\"     >");
	}

	@Test
	public void testForm12() throws Exception {
		assertThat(getMacroOutput("FORM12")).isEqualTo("<input type=\"hidden\" id=\"name\" name=\"name\" value=\"Darren\"     >");
	}

	@Test
	public void testForm13() throws Exception {
		assertThat(getMacroOutput("FORM13")).isEqualTo("<input type=\"password\" id=\"name\" name=\"name\" value=\"\"     >");
	}

	@Test
	public void testForm15() throws Exception {
		String output = getMacroOutput("FORM15");
		assertThat(output.startsWith("<input type=\"hidden\" name=\"_name\" value=\"on\"/>")).as("Wrong output: " + output).isTrue();
		assertThat(output.contains("<input type=\"checkbox\" id=\"name\" name=\"name\" />")).as("Wrong output: " + output).isTrue();
	}

	@Test
	public void testForm16() throws Exception {
		String output = getMacroOutput("FORM16");
		assertThat(output.startsWith(
				"<input type=\"hidden\" name=\"_jedi\" value=\"on\"/>")).as("Wrong output: " + output).isTrue();
		assertThat(output.contains(
				"<input type=\"checkbox\" id=\"jedi\" name=\"jedi\" checked=\"checked\" />")).as("Wrong output: " + output).isTrue();
	}

	@Test
	public void testForm17() throws Exception {
		assertThat(getMacroOutput("FORM17")).isEqualTo("<input type=\"text\" id=\"spouses0.name\" name=\"spouses[0].name\" value=\"Fred\"     >");
	}

	@Test
	public void testForm18() throws Exception {
		String output = getMacroOutput("FORM18");
		assertThat(output.startsWith(
				"<input type=\"hidden\" name=\"_spouses[0].jedi\" value=\"on\"/>")).as("Wrong output: " + output).isTrue();
		assertThat(output.contains(
				"<input type=\"checkbox\" id=\"spouses0.jedi\" name=\"spouses[0].jedi\" checked=\"checked\" />")).as("Wrong output: " + output).isTrue();
	}

	private String getMacroOutput(String name) throws Exception {
		String macro = fetchMacro(name);
		assertThat(macro).isNotNull();

		FileSystemResource resource = new FileSystemResource(System.getProperty("java.io.tmpdir") + "/tmp.ftl");
		FileCopyUtils.copy("<#import \"spring.ftl\" as spring />\n" + macro, new FileWriter(resource.getPath()));

		Map<String, String> msgMap = new HashMap<>();
		msgMap.put("hello", "Howdy");
		msgMap.put("world", "Mundo");

		TestBean darren = new TestBean("Darren", 99);
		TestBean fred = new TestBean("Fred");
		fred.setJedi(true);
		darren.setSpouse(fred);
		darren.setJedi(true);
		darren.setStringArray(new String[] {"John", "Fred"});

		Map<String, String> names = new HashMap<>();
		names.put("Darren", "Darren Davison");
		names.put("John", "John Doe");
		names.put("Fred", "Fred Bloggs");
		names.put("Rob&Harrop", "Rob Harrop");

		ModelMap model = new ExtendedModelMap();
		DummyMacroRequestContext rc = new DummyMacroRequestContext(this.exchange, model, new GenericApplicationContext());
		rc.setMessageMap(msgMap);
		rc.setContextPath("/springtest");

		model.put("command", darren);
		model.put("springMacroRequestContext", rc);
		model.put("msgArgs", new Object[] { "World" });
		model.put("nameOptionMap", names);
		model.put("options", names.values());

		FreeMarkerView view = new FreeMarkerView();
		view.setBeanName("myView");
		view.setUrl("tmp.ftl");
		view.setConfiguration(freeMarkerConfig);

		view.render(model, null, this.exchange).subscribe();

		// tokenize output and ignore whitespace
		String output = this.exchange.getResponse().getBodyAsString().block();
		output = output.replace("\r\n", "\n");
		return output.trim();
	}

	private String fetchMacro(String name) throws Exception {
		ClassPathResource resource = new ClassPathResource("test-macro.ftl", getClass());
		assertThat(resource.exists()).isTrue();
		String all = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream()));
		all = all.replace("\r\n", "\n");
		String[] macros = StringUtils.delimitedListToStringArray(all, "\n\n");
		for (String macro : macros) {
			if (macro.startsWith(name)) {
				return macro.substring(macro.indexOf("\n")).trim();
			}
		}
		return null;
	}

}
