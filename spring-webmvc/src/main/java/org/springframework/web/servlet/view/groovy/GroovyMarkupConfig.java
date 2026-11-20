/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.view.groovy;

import groovy.text.markup.MarkupTemplateEngine;

/**
 * Interface to be implemented by objects that configure and manage a Groovy
 * {@link MarkupTemplateEngine} for automatic lookup in a web environment.
 * Detected and used by {@link GroovyMarkupView}.
 *
 * @author Brian Clozel
 * @since 4.1
 * @see GroovyMarkupConfigurer
 */
public interface GroovyMarkupConfig {

	/**
	 * Return the Groovy {@link MarkupTemplateEngine} for the current
	 * web application context. May be unique to one servlet, or shared
	 * in the root context.
	 * @return the Groovy MarkupTemplateEngine engine
	 */
	MarkupTemplateEngine getTemplateEngine();

}
