/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcservlet.mvccontainerconfig

import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.support.XmlWebApplicationContext
import org.springframework.web.servlet.support.AbstractDispatcherServletInitializer

// tag::snippet[]
class MyXmlDispatcherServletInitializer : AbstractDispatcherServletInitializer() {

	override fun createRootApplicationContext(): WebApplicationContext? {
		return null
	}

	override fun createServletApplicationContext(): WebApplicationContext {
		return XmlWebApplicationContext().apply {
			setConfigLocation("/WEB-INF/spring/dispatcher-config.xml")
		}
	}

	override fun getServletMappings(): Array<String> {
		return arrayOf("/")
	}
}
// end::snippet[]
