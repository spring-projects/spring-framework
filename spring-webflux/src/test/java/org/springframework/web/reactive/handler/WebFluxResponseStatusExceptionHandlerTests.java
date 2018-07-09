/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.handler;

import java.time.Duration;

import org.junit.Test;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.reactive.handler.WebFluxResponseStatusExceptionHandler;
import org.springframework.web.server.handler.ResponseStatusExceptionHandler;
import org.springframework.web.server.handler.ResponseStatusExceptionHandlerTests;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link WebFluxResponseStatusExceptionHandler}.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 */
public class WebFluxResponseStatusExceptionHandlerTests extends ResponseStatusExceptionHandlerTests {

	@Override
	protected ResponseStatusExceptionHandler createResponseStatusExceptionHandler() {
		return new WebFluxResponseStatusExceptionHandler();
	}


	@Test
	public void handleAnnotatedException() {
		Throwable ex = new CustomException();
		this.handler.handle(this.exchange, ex).block(Duration.ofSeconds(5));
		assertEquals(HttpStatus.I_AM_A_TEAPOT, this.exchange.getResponse().getStatusCode());
	}

	@Test
	public void handleNestedAnnotatedException() {
		Throwable ex = new Exception(new CustomException());
		this.handler.handle(this.exchange, ex).block(Duration.ofSeconds(5));
		assertEquals(HttpStatus.I_AM_A_TEAPOT, this.exchange.getResponse().getStatusCode());
	}


	@SuppressWarnings("serial")
	@ResponseStatus(HttpStatus.I_AM_A_TEAPOT)
	private static class CustomException extends Exception {
	}

}
