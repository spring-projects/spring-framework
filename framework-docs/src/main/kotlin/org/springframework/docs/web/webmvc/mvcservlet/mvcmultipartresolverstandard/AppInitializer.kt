/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcservlet.mvcmultipartresolverstandard

import jakarta.servlet.MultipartConfigElement
import jakarta.servlet.ServletRegistration

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer

// tag::snippet[]
class AppInitializer : AbstractAnnotationConfigDispatcherServletInitializer() {

	// @fold:on
	override fun getServletMappings(): Array<out String> {
		/**/TODO("Not yet implemented")
	}

	override fun getRootConfigClasses(): Array<out Class<*>>? {
		/**/TODO("Not yet implemented")
	}

	override fun getServletConfigClasses(): Array<out Class<*>>? {
		/**/TODO("Not yet implemented")
	}

	// @fold:off
	override fun customizeRegistration(registration: ServletRegistration.Dynamic) {

		// Optionally also set maxFileSize, maxRequestSize, fileSizeThreshold
		registration.setMultipartConfig(MultipartConfigElement("/tmp"))
	}

}
// end::snippet[]
