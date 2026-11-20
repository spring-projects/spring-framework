/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
