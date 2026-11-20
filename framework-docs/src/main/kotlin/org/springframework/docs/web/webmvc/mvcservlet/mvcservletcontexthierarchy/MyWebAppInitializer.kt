/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcservlet.mvcservletcontexthierarchy

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer

// tag::snippet[]
class MyWebAppInitializer : AbstractAnnotationConfigDispatcherServletInitializer() {

	override fun getRootConfigClasses(): Array<Class<*>> {
		return arrayOf(RootConfig::class.java)
	}

	override fun getServletConfigClasses(): Array<Class<*>> {
		return arrayOf(App1Config::class.java)
	}

	override fun getServletMappings(): Array<String> {
		return arrayOf("/app1/*")
	}
}
// end::snippet[]

class RootConfig
class App1Config
