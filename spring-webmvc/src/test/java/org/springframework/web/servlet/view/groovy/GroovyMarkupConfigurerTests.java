/*
 * Copyright 2002-2014 the original author or authors.
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

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Locale;

import groovy.text.TemplateEngine;
import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticApplicationContext;

import static org.junit.Assert.*;

/**
 * Unit tests for
 * {@link org.springframework.web.servlet.view.groovy.GroovyMarkupConfigurer}.
 *
 * @author Brian Clozel
 */
public class GroovyMarkupConfigurerTests {

	private static final String RESOURCE_LOADER_PATH = "classpath:org/springframework/web/servlet/view/groovy/";

	private StaticApplicationContext applicationContext;

	private static final String TEMPLATE_PREFIX = "org/springframework/web/servlet/view/groovy/";

	private GroovyMarkupConfigurer configurer;


	@Before
	public void setup() throws Exception {
		this.applicationContext = new StaticApplicationContext();
		this.configurer = new GroovyMarkupConfigurer();
		this.configurer.setResourceLoaderPath(RESOURCE_LOADER_PATH);
	}

	@Test
	public void defaultTemplateEngine() throws Exception {
		this.configurer.setApplicationContext(this.applicationContext);
		this.configurer.afterPropertiesSet();

		TemplateEngine engine = this.configurer.getTemplateEngine();
		assertNotNull(engine);
		assertEquals(MarkupTemplateEngine.class, engine.getClass());

		MarkupTemplateEngine markupEngine = (MarkupTemplateEngine) engine;
		TemplateConfiguration configuration = markupEngine.getTemplateConfiguration();
		assertNotNull(configuration);
		assertEquals(GroovyMarkupConfigurer.class, configuration.getClass());
	}

	@Test
	public void customTemplateEngine() throws Exception {
		this.configurer.setApplicationContext(this.applicationContext);
		this.configurer.setTemplateEngine(new TestTemplateEngine());
		this.configurer.afterPropertiesSet();

		TemplateEngine engine = this.configurer.getTemplateEngine();
		assertNotNull(engine);
		assertEquals(TestTemplateEngine.class, engine.getClass());
	}

	@Test
	public void customTemplateConfiguration() throws Exception {
		this.configurer.setApplicationContext(this.applicationContext);
		this.configurer.setCacheTemplates(false);
		this.configurer.afterPropertiesSet();

		TemplateEngine engine = this.configurer.getTemplateEngine();
		assertNotNull(engine);
		assertEquals(MarkupTemplateEngine.class, engine.getClass());

		MarkupTemplateEngine markupEngine = (MarkupTemplateEngine) engine;
		TemplateConfiguration configuration = markupEngine.getTemplateConfiguration();
		assertNotNull(configuration);
		assertFalse(configuration.isCacheTemplates());
	}

	@Test
	@SuppressWarnings("resource")
	public void parentLoader() throws Exception {

		this.configurer.setApplicationContext(this.applicationContext);

		ClassLoader classLoader = this.configurer.createTemplateClassLoader();
		assertNotNull(classLoader);
		URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
		assertThat(Arrays.asList(urlClassLoader.getURLs()), Matchers.hasSize(1));
		assertThat(Arrays.asList(urlClassLoader.getURLs()).get(0).toString(),
				Matchers.endsWith("org/springframework/web/servlet/view/groovy/"));

		this.configurer.setResourceLoaderPath(RESOURCE_LOADER_PATH + ",classpath:org/springframework/web/servlet/view/");
		classLoader = this.configurer.createTemplateClassLoader();
		assertNotNull(classLoader);
		urlClassLoader = (URLClassLoader) classLoader;
		assertThat(Arrays.asList(urlClassLoader.getURLs()), Matchers.hasSize(2));
		assertThat(Arrays.asList(urlClassLoader.getURLs()).get(0).toString(),
				Matchers.endsWith("org/springframework/web/servlet/view/groovy/"));
		assertThat(Arrays.asList(urlClassLoader.getURLs()).get(1).toString(),
				Matchers.endsWith("org/springframework/web/servlet/view/"));
	}

	private class TestTemplateEngine extends MarkupTemplateEngine {

		public TestTemplateEngine() {
			super(new TemplateConfiguration());
		}
	}

	@Test
	public void resolveSampleTemplate() throws Exception {
		URL url = this.configurer.resolveTemplate(getClass().getClassLoader(), TEMPLATE_PREFIX + "test.tpl");
		Assert.assertNotNull(url);
	}

	@Test
	public void resolveI18nFullLocale() throws Exception {
		LocaleContextHolder.setLocale(Locale.GERMANY);
		URL url = this.configurer.resolveTemplate(getClass().getClassLoader(), TEMPLATE_PREFIX + "i18n.tpl");
		Assert.assertNotNull(url);
		Assert.assertThat(url.getPath(), Matchers.containsString("i18n_de_DE.tpl"));
	}

	@Test
	public void resolveI18nPartialLocale() throws Exception {
		LocaleContextHolder.setLocale(Locale.FRANCE);
		URL url = this.configurer.resolveTemplate(getClass().getClassLoader(), TEMPLATE_PREFIX + "i18n.tpl");
		Assert.assertNotNull(url);
		Assert.assertThat(url.getPath(), Matchers.containsString("i18n_fr.tpl"));
	}

	@Test
	public void resolveI18nDefaultLocale() throws Exception {
		LocaleContextHolder.setLocale(Locale.US);
		URL url = this.configurer.resolveTemplate(getClass().getClassLoader(), TEMPLATE_PREFIX + "i18n.tpl");
		Assert.assertNotNull(url);
		Assert.assertThat(url.getPath(), Matchers.containsString("i18n.tpl"));
	}

	@Test(expected = IOException.class)
	public void failMissingTemplate() throws Exception {
		LocaleContextHolder.setLocale(Locale.US);
		this.configurer.resolveTemplate(getClass().getClassLoader(), TEMPLATE_PREFIX + "missing.tpl");
		Assert.fail();
	}
}
