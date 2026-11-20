/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcservlet.mvcloggingsensitivedata

import jakarta.servlet.ServletRegistration

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer

// tag::snippet[]
class MyInitializer : AbstractAnnotationConfigDispatcherServletInitializer() {

	// @fold:on
	override fun getRootConfigClasses(): Array<Class<*>>? {
		/**/TODO("Not yet implemented")
	}

	override fun getServletConfigClasses(): Array<Class<*>>? {
		/**/TODO("Not yet implemented")
	}

	override fun getServletMappings(): Array<String> {
		/**/TODO("Not yet implemented")
	}

	// @fold:off
	override fun customizeRegistration(registration: ServletRegistration.Dynamic) {
		registration.setInitParameter("enableLoggingRequestDetails", "true")
	}
}
// end::snippet[]
