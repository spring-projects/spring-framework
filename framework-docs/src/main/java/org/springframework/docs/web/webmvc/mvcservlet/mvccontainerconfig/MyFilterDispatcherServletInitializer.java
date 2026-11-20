/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcservlet.mvccontainerconfig;

import jakarta.servlet.Filter;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.servlet.support.AbstractDispatcherServletInitializer;

// tag::snippet[]
public class MyFilterDispatcherServletInitializer extends AbstractDispatcherServletInitializer {

	@Override
	protected Filter[] getServletFilters() {
		return new Filter[] {
			new HiddenHttpMethodFilter(), new CharacterEncodingFilter() };
	}

	// @fold:on
	@Override
	protected WebApplicationContext createServletApplicationContext() {
		/**/return null;
	}

	@Override
	protected String[] getServletMappings() {
		/**/return new String[] { "/" };
	}

	@Override
	protected WebApplicationContext createRootApplicationContext() {
		/**/return null;
	}
	// @fold:off
}
// end::snippet[]
