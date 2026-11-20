/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcservlet.mvcanncustomerservletcontainererrorpage

import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// tag::snippet[]
@RestController
class ErrorController {

	@RequestMapping(path = ["/error"])
	fun handle(request: HttpServletRequest): Map<String, Any> {
		val map = HashMap<String, Any>()
		map["status"] = request.getAttribute("jakarta.servlet.error.status_code")!!
		map["reason"] = request.getAttribute("jakarta.servlet.error.message")!!
		return map
	}
}
// end::snippet[]
