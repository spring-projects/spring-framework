/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.reactive.result.view.script;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.script.Invocable;
import javax.script.ScriptEngine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.support.StaticApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;
import static org.junit.jupiter.api.condition.JRE.JAVA_15;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ScriptTemplateView}.
 *
 * @author Sebastien Deleuze
 */
@DisabledForJreRange(min = JAVA_15) // Nashorn JavaScript engine removed in Java 15
public class ScriptTemplateViewTests {

	private ScriptTemplateView view;

	private ScriptTemplateConfigurer configurer;

	private StaticApplicationContext context;


	@BeforeEach
	public void setup() {
		this.configurer = new ScriptTemplateConfigurer();
		this.context = new StaticApplicationContext();
		this.context.getBeanFactory().registerSingleton("scriptTemplateConfigurer", this.configurer);
		this.view = new ScriptTemplateView();
	}


	@Test
	public void missingTemplate() throws Exception {
		this.context.refresh();
		this.view.setResourceLoaderPath("classpath:org/springframework/web/reactive/result/view/script/");
		this.view.setUrl("missing.txt");
		this.view.setEngine(mock(InvocableScriptEngine.class));
		this.configurer.setRenderFunction("render");
		this.view.setApplicationContext(this.context);
		assertThat(this.view.checkResourceExists(Locale.ENGLISH)).isFalse();
	}

	@Test
	public void missingScriptTemplateConfig() throws Exception {
		assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(() ->
				this.view.setApplicationContext(new StaticApplicationContext()))
			.withMessageContaining("ScriptTemplateConfig");
	}

	@Test
	public void detectScriptTemplateConfigWithEngine() {
		InvocableScriptEngine engine = mock(InvocableScriptEngine.class);
		this.configurer.setEngine(engine);
		this.configurer.setRenderObject("Template");
		this.configurer.setRenderFunction("render");
		this.configurer.setCharset(StandardCharsets.ISO_8859_1);
		this.configurer.setSharedEngine(true);

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.view);
		this.view.setApplicationContext(this.context);
		assertThat(accessor.getPropertyValue("engine")).isEqualTo(engine);
		assertThat(accessor.getPropertyValue("renderObject")).isEqualTo("Template");
		assertThat(accessor.getPropertyValue("renderFunction")).isEqualTo("render");
		assertThat(accessor.getPropertyValue("defaultCharset")).isEqualTo(StandardCharsets.ISO_8859_1);
		assertThat(accessor.getPropertyValue("sharedEngine")).asInstanceOf(BOOLEAN).isTrue();
	}

	@Test
	public void detectScriptTemplateConfigWithEngineName() {
		this.configurer.setEngineName("nashorn");
		this.configurer.setRenderObject("Template");
		this.configurer.setRenderFunction("render");

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.view);
		this.view.setApplicationContext(this.context);
		assertThat(accessor.getPropertyValue("engineName")).isEqualTo("nashorn");
		assertThat(accessor.getPropertyValue("engine")).isNotNull();
		assertThat(accessor.getPropertyValue("renderObject")).isEqualTo("Template");
		assertThat(accessor.getPropertyValue("renderFunction")).isEqualTo("render");
		assertThat(accessor.getPropertyValue("defaultCharset")).isEqualTo(StandardCharsets.UTF_8);
	}

	@Test
	public void customEngineAndRenderFunction() throws Exception {
		ScriptEngine engine = mock(InvocableScriptEngine.class);
		given(engine.get("key")).willReturn("value");
		this.view.setEngine(engine);
		this.view.setRenderFunction("render");
		this.view.setApplicationContext(this.context);
		engine = this.view.getEngine();
		assertThat(engine).isNotNull();
		assertThat(engine.get("key")).isEqualTo("value");
		DirectFieldAccessor accessor = new DirectFieldAccessor(this.view);
		assertThat(accessor.getPropertyValue("renderObject")).isNull();
		assertThat(accessor.getPropertyValue("renderFunction")).isEqualTo("render");
		assertThat(accessor.getPropertyValue("defaultCharset")).isEqualTo(StandardCharsets.UTF_8);
	}

	@Test
	public void nonSharedEngine() throws Exception {
		int iterations = 20;
		this.view.setEngineName("nashorn");
		this.view.setRenderFunction("render");
		this.view.setSharedEngine(false);
		this.view.setApplicationContext(this.context);
		ExecutorService executor = Executors.newFixedThreadPool(4);
		List<Future<Boolean>> results = new ArrayList<>();
		for (int i = 0; i < iterations; i++) {
			results.add(executor.submit(() -> view.getEngine() != null));
		}
		assertThat(results.size()).isEqualTo(iterations);
		for (int i = 0; i < iterations; i++) {
			assertThat((boolean) results.get(i).get()).isTrue();
		}
		executor.shutdown();
	}

	@Test
	public void nonInvocableScriptEngine() throws Exception {
		this.view.setEngine(mock(ScriptEngine.class));
		this.view.setApplicationContext(this.context);
	}

	@Test
	public void nonInvocableScriptEngineWithRenderFunction() throws Exception {
		this.view.setEngine(mock(ScriptEngine.class));
		this.view.setRenderFunction("render");
		assertThatIllegalArgumentException().isThrownBy(() ->
				this.view.setApplicationContext(this.context));
	}

	@Test
	public void engineAndEngineNameBothDefined() {
		this.view.setEngine(mock(InvocableScriptEngine.class));
		this.view.setEngineName("test");
		this.view.setRenderFunction("render");
		assertThatIllegalArgumentException().isThrownBy(() ->
				this.view.setApplicationContext(this.context))
			.withMessageContaining("You should define either 'engine', 'engineSupplier', or 'engineName'.");
	}

	@Test  // gh-23258
	public void engineAndEngineSupplierBothDefined() {
		ScriptEngine engine = mock(InvocableScriptEngine.class);
		this.view.setEngineSupplier(() -> engine);
		this.view.setEngine(engine);
		this.view.setRenderFunction("render");
		assertThatIllegalArgumentException().isThrownBy(() ->
				this.view.setApplicationContext(this.context))
				.withMessageContaining("You should define either 'engine', 'engineSupplier', or 'engineName'.");
	}

	@Test  // gh-23258
	public void engineNameAndEngineSupplierBothDefined() {
		this.view.setEngineSupplier(() -> mock(InvocableScriptEngine.class));
		this.view.setEngineName("test");
		this.view.setRenderFunction("render");
		assertThatIllegalArgumentException().isThrownBy(() ->
				this.view.setApplicationContext(this.context))
				.withMessageContaining("You should define either 'engine', 'engineSupplier', or 'engineName'.");
	}

	@Test
	public void engineSetterAndNonSharedEngine() {
		this.view.setEngine(mock(InvocableScriptEngine.class));
		this.view.setRenderFunction("render");
		this.view.setSharedEngine(false);
		assertThatIllegalArgumentException().isThrownBy(() ->
				this.view.setApplicationContext(this.context))
			.withMessageContaining("sharedEngine");
	}

	@Test  // gh-23258
	public void engineSupplierWithSharedEngine() {
		this.configurer.setEngineSupplier(() -> mock(InvocableScriptEngine.class));
		this.configurer.setRenderObject("Template");
		this.configurer.setRenderFunction("render");
		this.configurer.setSharedEngine(true);

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.view);
		this.view.setApplicationContext(this.context);
		ScriptEngine engine1 = this.view.getEngine();
		ScriptEngine engine2 = this.view.getEngine();
		assertThat(engine1).isNotNull();
		assertThat(engine2).isNotNull();
		assertThat(accessor.getPropertyValue("renderObject")).isEqualTo("Template");
		assertThat(accessor.getPropertyValue("renderFunction")).isEqualTo("render");
		assertThat(accessor.getPropertyValue("sharedEngine")).asInstanceOf(BOOLEAN).isTrue();
	}

	@SuppressWarnings("unchecked")
	@Test  // gh-23258
	public void engineSupplierWithNonSharedEngine() {
		this.configurer.setEngineSupplier(() -> mock(InvocableScriptEngine.class));
		this.configurer.setRenderObject("Template");
		this.configurer.setRenderFunction("render");
		this.configurer.setSharedEngine(false);

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.view);
		this.view.setApplicationContext(this.context);
		ScriptEngine engine1 = this.view.getEngine();
		ScriptEngine engine2 = this.view.getEngine();
		assertThat(engine1).isNotNull();
		assertThat(engine2).isNotNull();
		assertThat(accessor.getPropertyValue("renderObject")).isEqualTo("Template");
		assertThat(accessor.getPropertyValue("renderFunction")).isEqualTo("render");
		assertThat(accessor.getPropertyValue("sharedEngine")).asInstanceOf(BOOLEAN).isFalse();
	}

	private interface InvocableScriptEngine extends ScriptEngine, Invocable {
	}

}
