/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvccontroller.mvcannexceptionhandlermedia

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ExceptionHandler

@Controller
class MediaTypeController {

	// tag::mediatype[]
	@ExceptionHandler(produces = ["application/json"])
	fun handleJson(exc: IllegalArgumentException): ResponseEntity<ErrorMessage> {
		return ResponseEntity.badRequest().body(ErrorMessage(exc.message, 42))
	}

	@ExceptionHandler(produces = ["text/html"])
	fun handle(exc: IllegalArgumentException, model: Model): String {
		model.addAttribute("error", ErrorMessage(exc.message, 42))
		return "errorView"
	}
	// end::mediatype[]

	@JvmRecord
	data class ErrorMessage(val message: String?, val code: Int)
}