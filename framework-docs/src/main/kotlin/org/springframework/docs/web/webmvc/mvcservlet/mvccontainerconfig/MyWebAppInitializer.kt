/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcservlet.mvccontainerconfig

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer

// tag::snippet[]
class MyWebAppInitializer : AbstractAnnotationConfigDispatcherServletInitializer() {

	override fun getRootConfigClasses(): Array<Class<*>>? {
		return null
	}

	override fun getServletConfigClasses(): Array<Class<*>>? {
		return arrayOf(MyWebConfig::class.java)
	}

	override fun getServletMappings(): Array<String> {
		return arrayOf("/")
	}
}
// end::snippet[]

class MyWebConfig
