/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcservlet

import jakarta.servlet.ServletContext
import org.springframework.web.WebApplicationInitializer
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext
import org.springframework.web.servlet.DispatcherServlet

// tag::snippet[]
class MyWebApplicationInitializer : WebApplicationInitializer {

	override fun onStartup(servletContext: ServletContext) {

		// Load Spring web application configuration
		val context = AnnotationConfigWebApplicationContext()
		context.register(AppConfig::class.java)

		// Create and register the DispatcherServlet
		val servlet = DispatcherServlet(context)
		val registration = servletContext.addServlet("app", servlet)
		registration.setLoadOnStartup(1)
		registration.addMapping("/app/*")
	}
}
// end::snippet[]
