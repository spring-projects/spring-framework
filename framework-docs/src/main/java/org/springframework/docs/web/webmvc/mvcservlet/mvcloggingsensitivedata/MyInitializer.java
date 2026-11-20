/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcservlet.mvcloggingsensitivedata;

import jakarta.servlet.ServletRegistration;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

// tag::snippet[]
public class MyInitializer
		extends AbstractAnnotationConfigDispatcherServletInitializer {

	// @fold:on
	@Override
	protected Class<?>[] getRootConfigClasses() {
		/**/throw new UnsupportedOperationException();
	}

	@Override
	protected Class<?>[] getServletConfigClasses() {
		/**/throw new UnsupportedOperationException();
	}

	@Override
	protected String[] getServletMappings() {
		/**/throw new UnsupportedOperationException();
	}

	// @fold:off
	@Override
	protected void customizeRegistration(ServletRegistration.Dynamic registration) {
		registration.setInitParameter("enableLoggingRequestDetails", "true");
	}

}
// end::snippet[]
