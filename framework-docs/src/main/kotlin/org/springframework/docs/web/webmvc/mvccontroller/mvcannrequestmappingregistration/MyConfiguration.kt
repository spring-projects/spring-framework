/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvccontroller.mvcannrequestmappingregistration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

// tag::snippet[]
@Configuration
class MyConfiguration {

	// Inject the target handler and the handler mapping for controllers
	@Autowired
	fun setHandlerMapping(mapping: RequestMappingHandlerMapping, handler: UserHandler) {

		// Get the handler method
		val info = RequestMappingInfo.paths("/user/{id}").methods(RequestMethod.GET).build()

		// Get the handler method
		val method = UserHandler::class.java.getMethod("getUser", Long::class.java)

		// Add the registration
		mapping.registerMapping(info, handler, method)
	}
}
// end::snippet[]

