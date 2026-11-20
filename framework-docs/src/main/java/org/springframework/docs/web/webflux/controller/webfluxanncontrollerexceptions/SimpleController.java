/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webflux.controller.webfluxanncontrollerexceptions;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Controller
public class SimpleController {

	@ExceptionHandler(IOException.class)
	public ResponseEntity<String> handle() {
		return ResponseEntity.internalServerError().body("Could not read file storage");
	}

}