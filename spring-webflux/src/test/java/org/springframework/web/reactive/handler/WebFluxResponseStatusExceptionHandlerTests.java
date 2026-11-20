/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.handler;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.handler.ResponseStatusExceptionHandler;
import org.springframework.web.testfixture.server.handler.AbstractResponseStatusExceptionHandlerTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebFluxResponseStatusExceptionHandler}.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 */
class WebFluxResponseStatusExceptionHandlerTests extends AbstractResponseStatusExceptionHandlerTests {

	@Override
	protected ResponseStatusExceptionHandler createResponseStatusExceptionHandler() {
		return new WebFluxResponseStatusExceptionHandler();
	}


	@Test
	void handleAnnotatedException() {
		Throwable ex = new CustomException();
		this.handler.handle(this.exchange, ex).block(Duration.ofSeconds(5));
		assertThat(this.exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.ALREADY_REPORTED);
	}

	@Test
	void handleNestedAnnotatedException() {
		Throwable ex = new Exception(new CustomException());
		this.handler.handle(this.exchange, ex).block(Duration.ofSeconds(5));
		assertThat(this.exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.ALREADY_REPORTED);
	}


	@SuppressWarnings("serial")
	@ResponseStatus(HttpStatus.ALREADY_REPORTED)
	private static class CustomException extends Exception {
	}

}
