/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcservlet.mvccontainerconfig

import jakarta.servlet.ServletContext
import org.springframework.web.WebApplicationInitializer
import org.springframework.web.context.support.XmlWebApplicationContext
import org.springframework.web.servlet.DispatcherServlet

// tag::snippet[]
class MyWebApplicationInitializer : WebApplicationInitializer {

	override fun onStartup(container: ServletContext) {
		val appContext = XmlWebApplicationContext()
		appContext.setConfigLocation("/WEB-INF/spring/dispatcher-config.xml")

		val registration = container.addServlet("dispatcher", DispatcherServlet(appContext))
		registration.setLoadOnStartup(1)
		registration.addMapping("/")
	}
}
// end::snippet[]
