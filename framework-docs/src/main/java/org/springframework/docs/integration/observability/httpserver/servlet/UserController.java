/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.observability.httpserver.servlet;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.filter.ServerHttpObservationFilter;

@Controller
public class UserController {

	@ExceptionHandler(MissingUserException.class)
	ResponseEntity<Void> handleMissingUser(HttpServletRequest request, MissingUserException exception) {
		// We want to record this exception with the observation
		ServerHttpObservationFilter.findObservationContext(request)
				.ifPresent(context -> context.setError(exception));
		return ResponseEntity.notFound().build();
	}

	// @fold:on
	@SuppressWarnings("serial")
	static class MissingUserException extends RuntimeException {
	}
	// @fold:off

}
