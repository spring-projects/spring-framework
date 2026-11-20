/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcservlet.mvcmultipartresolverstandard;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletRegistration;
import org.jspecify.annotations.Nullable;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

// tag::snippet[]
public class AppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

	// @fold:on
	@Override
	protected String[] getServletMappings() {
		/**/throw new UnsupportedOperationException();
	}

	@Override
	protected Class<?> @Nullable [] getRootConfigClasses() {
		/**/throw new UnsupportedOperationException();
	}

	@Override
	protected Class<?> @Nullable [] getServletConfigClasses() {
		/**/throw new UnsupportedOperationException();
	}

	// @fold:off
	@Override
	protected void customizeRegistration(ServletRegistration.Dynamic registration) {

		// Optionally also set maxFileSize, maxRequestSize, fileSizeThreshold
		registration.setMultipartConfig(new MultipartConfigElement("/tmp"));
	}
}
// end::snippet[]
