/*
 * Copyright 2002-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
