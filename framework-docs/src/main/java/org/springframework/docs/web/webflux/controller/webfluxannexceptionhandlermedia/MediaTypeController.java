/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.docs.web.webflux.controller.webfluxannexceptionhandlermedia;

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
