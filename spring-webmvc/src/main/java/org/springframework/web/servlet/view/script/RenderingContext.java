/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Locale;
import java.util.function.Function;

import org.springframework.context.ApplicationContext;

/**
 * Context passed to {@link ScriptTemplateView} render function in order to make
 * the application context, the locale, the template loader and the url available on
 * scripting side.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class RenderingContext {

	private final ApplicationContext applicationContext;

	private final Locale locale;

	private final Function<String, String> templateLoader;

	private final String url;


	/**
	 * Create a new {@code RenderingContext}.
	 *
	 * @param applicationContext the application context
	 * @param locale the locale of the rendered template
	 * @param templateLoader a function that takes a template path as input and returns
	 * the template content as a String
	 * @param url the URL of the rendered template
	 */
	public RenderingContext(ApplicationContext applicationContext, Locale locale,
			Function<String, String> templateLoader, String url) {

		this.applicationContext = applicationContext;
		this.locale = locale;
		this.templateLoader = templateLoader;
		this.url = url;
	}


	/**
	 * Return the application context.
	 */
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	/**
	 * Return the locale of the rendered template.
	 */
	public Locale getLocale() {
		return this.locale;
	}

	/**
	 * Return a function that takes a template path as input and returns the template
	 * content as a String.
	 */
	public Function<String, String> getTemplateLoader() {
		return this.templateLoader;
	}

	/**
	 * Return the URL of the rendered template.
	 */
	public String getUrl() {
		return this.url;
	}

}
