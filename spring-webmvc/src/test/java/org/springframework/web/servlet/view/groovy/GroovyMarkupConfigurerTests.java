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

package org.springframework.web.servlet.view.groovy;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;

import groovy.text.TemplateEngine;
import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * Tests for {@link GroovyMarkupConfigurer}.
 *
 * @author Brian Clozel
 */
class GroovyMarkupConfigurerTests {

	private static final String RESOURCE_LOADER_PATH = "classpath:org/springframework/web/servlet/view/groovy/";

	private StaticApplicationContext applicationContext;

	private static final String TEMPLATE_PREFIX = "org/springframework/web/servlet/view/groovy/";

	private GroovyMarkupConfigurer configurer;


	@BeforeEach
	void setup() {
		this.applicationContext = new StaticApplicationContext();
		this.configurer = new GroovyMarkupConfigurer();
		this.configurer.setResourceLoaderPath(RESOURCE_LOADER_PATH);
	}

	@Test
	void defaultTemplateEngine() throws Exception {
		this.configurer.setApplicationContext(this.applicationContext);
		this.configurer.afterPropertiesSet();

		MarkupTemplateEngine engine = this.configurer.getTemplateEngine();
		assertThat(engine).isNotNull();
		assertThat(engine.getClass()).isEqualTo(MarkupTemplateEngine.class);

		TemplateConfiguration configuration = engine.getTemplateConfiguration();
		assertThat(configuration).isNotNull();
		assertThat(configuration.getClass()).isEqualTo(GroovyMarkupConfigurer.class);
	}

	@Test
	void customTemplateEngine() throws Exception {
		this.configurer.setApplicationContext(this.applicationContext);
		this.configurer.setTemplateEngine(new TestTemplateEngine());
		this.configurer.afterPropertiesSet();

		TemplateEngine engine = this.configurer.getTemplateEngine();
		assertThat(engine).isNotNull();
		assertThat(engine.getClass()).isEqualTo(TestTemplateEngine.class);
	}

	@Test
	void customTemplateConfiguration() throws Exception {
		this.configurer.setApplicationContext(this.applicationContext);
		this.configurer.setCacheTemplates(false);
		this.configurer.afterPropertiesSet();

		MarkupTemplateEngine engine = this.configurer.getTemplateEngine();
		assertThat(engine).isNotNull();
		assertThat(engine.getClass()).isEqualTo(MarkupTemplateEngine.class);

		TemplateConfiguration configuration = engine.getTemplateConfiguration();
		assertThat(configuration).isNotNull();
		assertThat(configuration.isCacheTemplates()).isFalse();
	}

	@Test
	void parentLoader() throws Exception {

		this.configurer.setApplicationContext(this.applicationContext);

		ClassLoader classLoader = this.configurer.createTemplateClassLoader();
		assertThat(classLoader).isNotNull();
		URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
		assertThat(urlClassLoader.getURLs()).hasSize(1);
		assertThat(urlClassLoader.getURLs()[0].toString())
				.endsWith("org/springframework/web/servlet/view/groovy/");

		this.configurer.setResourceLoaderPath(RESOURCE_LOADER_PATH + ",classpath:org/springframework/web/servlet/view/");
		classLoader = this.configurer.createTemplateClassLoader();
		assertThat(classLoader).isNotNull();
		urlClassLoader = (URLClassLoader) classLoader;
		assertThat(urlClassLoader.getURLs()).hasSize(2);
		assertThat(urlClassLoader.getURLs()[0].toString())
				.endsWith("org/springframework/web/servlet/view/groovy/");
		assertThat(urlClassLoader.getURLs()[1].toString())
				.endsWith("org/springframework/web/servlet/view/");
	}

	private static class TestTemplateEngine extends MarkupTemplateEngine {

		public TestTemplateEngine() {
			super(new TemplateConfiguration());
		}
	}

	@Test
	void resolveSampleTemplate() throws Exception {
		URL url = this.configurer.resolveTemplate(getClass().getClassLoader(), TEMPLATE_PREFIX + "test.tpl");
		assertThat(url).isNotNull();
	}

	@Test
	void resolveI18nFullLocale() throws Exception {
		LocaleContextHolder.setLocale(Locale.GERMANY);
		URL url = this.configurer.resolveTemplate(getClass().getClassLoader(), TEMPLATE_PREFIX + "i18n.tpl");
		assertThat(url).isNotNull();
		assertThat(url.getPath()).contains("i18n_de_DE.tpl");
	}

	@Test
	void resolveI18nPartialLocale() throws Exception {
		LocaleContextHolder.setLocale(Locale.FRANCE);
		URL url = this.configurer.resolveTemplate(getClass().getClassLoader(), TEMPLATE_PREFIX + "i18n.tpl");
		assertThat(url).isNotNull();
		assertThat(url.getPath()).contains("i18n_fr.tpl");
	}

	@Test
	void resolveI18nDefaultLocale() throws Exception {
		LocaleContextHolder.setLocale(Locale.US);
		URL url = this.configurer.resolveTemplate(getClass().getClassLoader(), TEMPLATE_PREFIX + "i18n.tpl");
		assertThat(url).isNotNull();
		assertThat(url.getPath()).contains("i18n.tpl");
	}

	@Test
	void failMissingTemplate() {
		LocaleContextHolder.setLocale(Locale.US);
		assertThatIOException().isThrownBy(() ->
			this.configurer.resolveTemplate(getClass().getClassLoader(), TEMPLATE_PREFIX + "missing.tpl"));
	}

}
