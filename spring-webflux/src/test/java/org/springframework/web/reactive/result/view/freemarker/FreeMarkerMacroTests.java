/*
 * Copyright 2002-2024 the original author or authors.
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

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.template.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.result.view.BindStatus;
import org.springframework.web.reactive.result.view.DummyMacroRequestContext;
import org.springframework.web.reactive.result.view.RequestContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Darren Davison
 * @author Juergen Hoeller
 * @author Issam El-atif
 * @author Sam Brannen
 * @since 5.2
 */
class FreeMarkerMacroTests {

	private static final String TEMPLATE_FILE = "test-macro.ftl";

	private final MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path"));

	private final GenericApplicationContext applicationContext = new GenericApplicationContext();

	private Configuration freeMarkerConfig;

	private Path templateLoaderPath;


	@BeforeEach
	void setUp() throws Exception {
		this.templateLoaderPath = Files.createTempDirectory("webflux-").toAbsolutePath();

		this.applicationContext.refresh();

		FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
		configurer.setTemplateLoaderPaths("classpath:/", "file://" + this.templateLoaderPath);
		this.freeMarkerConfig = configurer.createConfiguration();
	}

	@Test
	void springMacroRequestContextIsAutomaticallyExposedAsModelAttribute() throws Exception {
		storeTemplateInTempDir("<@spring.bind \"testBean.name\"/>\nHi ${spring.status.value}");

		FreeMarkerView view = new FreeMarkerView() {

			@Override
			protected Mono<Void> renderInternal(Map<String, Object> renderAttributes,
					MediaType contentType, ServerWebExchange exchange) {

				Object value = renderAttributes.get(SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE);
				assertThat(value).isInstanceOf(RequestContext.class);
				BindStatus status = ((RequestContext) value).getBindStatus("testBean.name");
				assertThat(status.getExpression()).isEqualTo("name");
				assertThat(status.getValue()).isEqualTo("Dilbert");

				return super.renderInternal(renderAttributes, contentType, exchange);
			}
		};

		view.setApplicationContext(this.applicationContext);
		view.setBeanName("myView");
		view.setUrl("tmp.ftl");
		view.setConfiguration(this.freeMarkerConfig);

		Map<String, ?> model = singletonMap("testBean", new TestBean("Dilbert", 99));

		StepVerifier.create(view.render(model, null, this.exchange)
						.then(Mono.fromCallable(this::getOutput)))
				.assertNext(l -> assertThat(l).containsExactly("Hi Dilbert"));
	}

	@Test
	void name() throws Exception {
		testMacroOutput("NAME", "Darren");
	}

	private void testMacroOutput(String name, String... contents) throws Exception {
		StepVerifier.create(getMacroOutput(name))
				.assertNext(list -> assertThat(list).containsExactly(contents))
				.verifyComplete();

	}

	@Test
	@DisabledForJreRange(min = JRE.JAVA_21)
	public void age() throws Exception {
		testMacroOutput("AGE", "99");
	}

	@Test
	void message() throws Exception {
		testMacroOutput("MESSAGE", "Howdy Mundo");
	}

	@Test
	void defaultMessage() throws Exception {
		testMacroOutput("DEFAULTMESSAGE", "hi planet");
	}

	@Test
	void messageArgs() throws Exception {
		testMacroOutput("MESSAGEARGS", "Howdy[World]");
	}

	@Test
	void messageArgsWithDefaultMessage() throws Exception {
		testMacroOutput("MESSAGEARGSWITHDEFAULTMESSAGE", "Hi");
	}

	@Test
	void url() throws Exception {
		testMacroOutput("URL", "/springtest/aftercontext.html");
	}

	@Test
	void urlParams() throws Exception {
		testMacroOutput("URLPARAMS", "/springtest/aftercontext/bar?spam=bucket");
	}

	@Test
	void formInput() throws Exception {
		testMacroOutput("FORM1", "<input type=\"text\" id=\"name\" name=\"name\" value=\"Darren\" >");
	}

	@Test
	void formInputWithCss() throws Exception {
		testMacroOutput("FORM2", "<input type=\"text\" id=\"name\" name=\"name\" value=\"Darren\" class=\"myCssClass\" >");
	}

	@Test
	void formTextarea() throws Exception {
		testMacroOutput("FORM3", "<textarea id=\"name\" name=\"name\" >Darren</textarea>");
	}

	@Test
	void formTextareaWithCustomRowsAndColumns() throws Exception {
		testMacroOutput("FORM4", "<textarea id=\"name\" name=\"name\" rows=10 cols=30>Darren</textarea>");
	}

	@Test
	void formSingleSelectFromMap() throws Exception {
		testMacroOutput("FORM5",
				"<select id=\"name\" name=\"name\" >", //
				"<option value=\"Rob&amp;Harrop\">Rob Harrop</option>", //
				"<option value=\"John\">John Doe</option>", //
				"<option value=\"Fred\">Fred Bloggs</option>", //
				"<option value=\"Darren\" selected=\"selected\">Darren Davison</option>", //
				"</select>");
	}

	@Test
	void formSingleSelectFromList() throws Exception {
		testMacroOutput("FORM14",
				"<select id=\"name\" name=\"name\" >", //
				"<option value=\"Rob Harrop\">Rob Harrop</option>", //
				"<option value=\"Darren Davison\">Darren Davison</option>", //
				"<option value=\"John Doe\">John Doe</option>", //
				"<option value=\"Fred Bloggs\">Fred Bloggs</option>", //
				"</select>");
	}

	@Test
	void formMultiSelect() throws Exception {
		testMacroOutput("FORM6",
				"<select multiple=\"multiple\" id=\"spouses\" name=\"spouses\" >", //
				"<option value=\"Rob&amp;Harrop\">Rob Harrop</option>", //
				"<option value=\"John\">John Doe</option>", //
				"<option value=\"Fred\" selected=\"selected\">Fred Bloggs</option>", //
				"<option value=\"Darren\">Darren Davison</option>", //
				"</select>");
	}

	@Test
	void formRadioButtons() throws Exception {
		testMacroOutput("FORM7",
				"<input type=\"radio\" id=\"name0\" name=\"name\" value=\"Rob&amp;Harrop\" >", //
				"<label for=\"name0\">Rob Harrop</label>", //
				"<input type=\"radio\" id=\"name1\" name=\"name\" value=\"John\" >", //
				"<label for=\"name1\">John Doe</label>", //
				"<input type=\"radio\" id=\"name2\" name=\"name\" value=\"Fred\" >", //
				"<label for=\"name2\">Fred Bloggs</label>", //
				"<input type=\"radio\" id=\"name3\" name=\"name\" value=\"Darren\" checked=\"checked\" >", //
				"<label for=\"name3\">Darren Davison</label>");
	}

	@Test
	void formCheckboxForStringProperty() throws Exception {
		testMacroOutput("FORM15",
				"<input type=\"hidden\" name=\"_name\" value=\"on\"/>",
				"<input type=\"checkbox\" id=\"name\" name=\"name\" />");
	}

	@Test
	void formCheckboxForBooleanProperty() throws Exception {
		testMacroOutput("FORM16",
				"<input type=\"hidden\" name=\"_jedi\" value=\"on\"/>",
				"<input type=\"checkbox\" id=\"jedi\" name=\"jedi\" checked=\"checked\" />");
	}

	@Test
	void formCheckboxForNestedPath() throws Exception {
		testMacroOutput("FORM18",
				"<input type=\"hidden\" name=\"_spouses[0].jedi\" value=\"on\"/>",
				"<input type=\"checkbox\" id=\"spouses0.jedi\" name=\"spouses[0].jedi\" checked=\"checked\" />");
	}

	@Test
	void formCheckboxForStringArray() throws Exception {
		testMacroOutput("FORM8",
				"<input type=\"checkbox\" id=\"stringArray0\" name=\"stringArray\" value=\"Rob&amp;Harrop\" >", //
				"<label for=\"stringArray0\">Rob Harrop</label>", //
				"<input type=\"checkbox\" id=\"stringArray1\" name=\"stringArray\" value=\"John\" checked=\"checked\" >", //
				"<label for=\"stringArray1\">John Doe</label>", //
				"<input type=\"checkbox\" id=\"stringArray2\" name=\"stringArray\" value=\"Fred\" checked=\"checked\" >", //
				"<label for=\"stringArray2\">Fred Bloggs</label>", //
				"<input type=\"checkbox\" id=\"stringArray3\" name=\"stringArray\" value=\"Darren\" >", //
				"<label for=\"stringArray3\">Darren Davison</label>", //
				"<input type=\"hidden\" name=\"_stringArray\" value=\"on\"/>");
	}

	@Test
	void formPasswordInput() throws Exception {
		testMacroOutput("FORM9",
				"<input type=\"password\" id=\"name\" name=\"name\" value=\"\" >");
	}

	@Test
	void formHiddenInput() throws Exception {
		testMacroOutput("FORM10",
				"<input type=\"hidden\" id=\"name\" name=\"name\" value=\"Darren\" >");
	}

	@Test
	void formInputText() throws Exception {
		testMacroOutput("FORM11",
				"<input type=\"text\" id=\"name\" name=\"name\" value=\"Darren\" >");
	}

	@Test
	void formInputHidden() throws Exception {
		testMacroOutput("FORM12",
				"<input type=\"hidden\" id=\"name\" name=\"name\" value=\"Darren\" >");
	}

	@Test
	void formInputPassword() throws Exception {
		testMacroOutput("FORM13",
				"<input type=\"password\" id=\"name\" name=\"name\" value=\"\" >");
	}

	@Test
	void forInputWithNestedPath() throws Exception {
		testMacroOutput("FORM17",
				"<input type=\"text\" id=\"spouses0.name\" name=\"spouses[0].name\" value=\"Fred\" >");
	}

	private Mono<List<String>> getMacroOutput(String name) throws Exception {
		String macro = fetchMacro(name);
		assertThat(macro).isNotNull();
		storeTemplateInTempDir(macro);

		Map<String, String> msgMap = new HashMap<>();
		msgMap.put("hello", "Howdy");
		msgMap.put("world", "Mundo");

		TestBean darren = new TestBean("Darren", 99);
		TestBean fred = new TestBean("Fred");
		fred.setJedi(true);
		darren.setSpouse(fred);
		darren.setJedi(true);
		darren.setStringArray(new String[] { "John", "Fred" });

		Map<String, String> names = new HashMap<>();
		names.put("Darren", "Darren Davison");
		names.put("John", "John Doe");
		names.put("Fred", "Fred Bloggs");
		names.put("Rob&Harrop", "Rob Harrop");

		Map<String, Object> model = new HashMap<>();
		DummyMacroRequestContext rc = new DummyMacroRequestContext(this.exchange, model,
				this.applicationContext);
		rc.setMessageMap(msgMap);
		rc.setContextPath("/springtest");

		model.put("command", darren);
		model.put(FreeMarkerView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE, rc);
		model.put("msgArgs", new Object[] { "World" });
		model.put("nameOptionMap", names);
		model.put("options", names.values());

		FreeMarkerView view = new FreeMarkerView();
		view.setApplicationContext(this.applicationContext);
		view.setBeanName("myView");
		view.setUrl("tmp.ftl");
		view.setExposeSpringMacroHelpers(false);
		view.setConfiguration(freeMarkerConfig);

		return view.render(model, null, this.exchange).
				then(Mono.fromCallable(this::getOutput));
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
		ClassPathResource resource = new ClassPathResource(TEMPLATE_FILE, FreeMarkerMacroTests.class);
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

	private List<String> getOutput() {
		String output = this.exchange.getResponse().getBodyAsString().block();
		String[] lines = output.replace("\r\n", "\n").replaceAll(" +"," ").split("\n");
		return Arrays.stream(lines).map(String::trim).filter(line -> !line.isEmpty()).collect(toList());
	}

}
