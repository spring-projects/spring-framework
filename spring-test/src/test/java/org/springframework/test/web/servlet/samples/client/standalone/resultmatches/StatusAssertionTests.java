/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.test.web.servlet.samples.client.standalone.resultmatches;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.core.annotation.AliasFor;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.I_AM_A_TEAPOT;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.standalone.resultmatchers.StatusAssertionTests}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
@TestInstance(PER_CLASS)
class StatusAssertionTests {

	private final WebTestClient testClient =
			MockMvcWebTestClient.bindToController(new StatusController()).build();


	@Test
	void statusInt() {
		testClient.get().uri("/teaPot").exchange().expectStatus().isEqualTo(I_AM_A_TEAPOT.value());
		testClient.get().uri("/created").exchange().expectStatus().isEqualTo(CREATED.value());
		testClient.get().uri("/createdWithComposedAnnotation").exchange().expectStatus().isEqualTo(CREATED.value());
		testClient.get().uri("/badRequest").exchange().expectStatus().isEqualTo(BAD_REQUEST.value());
		testClient.get().uri("/throwsException").exchange().expectStatus().isEqualTo(I_AM_A_TEAPOT.value());
	}

	@Test
	void httpStatus() {
		testClient.get().uri("/created").exchange().expectStatus().isCreated();
		testClient.get().uri("/createdWithComposedAnnotation").exchange().expectStatus().isCreated();
		testClient.get().uri("/badRequest").exchange().expectStatus().isBadRequest();
	}

	@Test
	void matcher() {
		testClient.get().uri("/badRequest").exchange().expectStatus().value(equalTo(BAD_REQUEST.value()));
	}


	@RequestMapping
	@ResponseStatus
	@Retention(RetentionPolicy.RUNTIME)
	@interface Get {

		@AliasFor(annotation = RequestMapping.class, attribute = "path")
		String[] path() default {};

		@AliasFor(annotation = ResponseStatus.class, attribute = "code")
		HttpStatus status() default INTERNAL_SERVER_ERROR;
	}

	@RestController
	@ResponseStatus(I_AM_A_TEAPOT)
	private static class StatusController {

		@RequestMapping("/teaPot")
		void teaPot() {
		}

		@RequestMapping("/created")
		@ResponseStatus(CREATED)
		void created(){
		}

		@Get(path = "/createdWithComposedAnnotation", status = CREATED)
		void createdWithComposedAnnotation() {
		}

		@RequestMapping("/badRequest")
		@ResponseStatus(code = BAD_REQUEST, reason = "Expired token")
		void badRequest(){
		}

		@RequestMapping("/notImplemented")
		@ResponseStatus(NOT_IMPLEMENTED)
		void notImplemented(){
		}

		@RequestMapping("/throwsException")
		@ResponseStatus(NOT_IMPLEMENTED)
		void throwsException() {
			throw new IllegalStateException();
		}

		@ExceptionHandler
		void exceptionHandler(IllegalStateException ex) {
		}
	}

}
