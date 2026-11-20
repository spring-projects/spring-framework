/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvccontroller.mvcannexceptionhandlermedia;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Controller
public class MediaTypeController {

	// tag::mediatype[]
	@ExceptionHandler(produces = "application/json")
	public ResponseEntity<ErrorMessage> handleJson(IllegalArgumentException exc) {
		return ResponseEntity.badRequest().body(new ErrorMessage(exc.getMessage(), 42));
	}

	@ExceptionHandler(produces = "text/html")
	public String handle(IllegalArgumentException exc, Model model) {
		model.addAttribute("error", new ErrorMessage(exc.getMessage(), 42));
		return "errorView";
	}
	// end::mediatype[]

	static record ErrorMessage(String message, int code) {

	}
}
