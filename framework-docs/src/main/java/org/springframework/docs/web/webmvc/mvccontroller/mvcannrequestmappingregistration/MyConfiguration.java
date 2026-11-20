/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvccontroller.mvcannrequestmappingregistration;

import java.lang.reflect.Method;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

// tag::snippet[]
@Configuration
public class MyConfiguration {

	// Inject the target handler and the handler mapping for controllers
	@Autowired
	public void setHandlerMapping(RequestMappingHandlerMapping mapping, UserHandler handler)
			throws NoSuchMethodException {

		// Prepare the request mapping meta data
		RequestMappingInfo info = RequestMappingInfo
				.paths("/user/{id}").methods(RequestMethod.GET).build();

		// Get the handler method
		Method method = UserHandler.class.getMethod("getUser", Long.class);

		// Add the registration
		mapping.registerMapping(info, handler, method);
	}
}
// end::snippet[]

