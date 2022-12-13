/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.web.servlet.samples.client.standalone;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.Person;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.standalone.AsyncTests}.
 *
 * @author Rossen Stoyanchev
 */
class AsyncTests {

	private final WebTestClient testClient =
			MockMvcWebTestClient.bindToController(new AsyncController()).build();


	@Test
	void callable() {
		this.testClient.get()
				.uri("/1?callable=true")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody().json("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}");
	}

	@Test
	void streaming() {
		this.testClient.get()
				.uri("/1?streaming=true")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("name=Joe");
	}

	@Test
	void streamingSlow() {
		this.testClient.get()
				.uri("/1?streamingSlow=true")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("name=Joe&someBoolean=true");
	}

	@Test
	void streamingJson() {
		this.testClient.get()
				.uri("/1?streamingJson=true")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody().json("{\"name\":\"Joe\",\"someDouble\":0.5}");
	}

	@Test
	void deferredResult() {
		this.testClient.get()
				.uri("/1?deferredResult=true")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody().json("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}");
	}

	@Test
	void deferredResultWithImmediateValue() {
		this.testClient.get()
				.uri("/1?deferredResultWithImmediateValue=true")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody().json("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}");
	}

	@Test
	void deferredResultWithDelayedError() {
		this.testClient.get()
				.uri("/1?deferredResultWithDelayedError=true")
				.exchange()
				.expectStatus().is5xxServerError()
				.expectBody(String.class).isEqualTo("Delayed Error");
	}

	@Test
	void listenableFuture() {
		this.testClient.get()
				.uri("/1?listenableFuture=true")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody().json("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}");
	}

	@Test
	void completableFutureWithImmediateValue() throws Exception {
		this.testClient.get()
				.uri("/1?completableFutureWithImmediateValue=true")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody().json("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}");
	}


	@RestController
	@RequestMapping(path = "/{id}", produces = "application/json")
	private static class AsyncController {

		@GetMapping(params = "callable")
		Callable<Person> getCallable() {
			return () -> new Person("Joe");
		}

		@GetMapping(params = "streaming")
		StreamingResponseBody getStreaming() {
			return os -> os.write("name=Joe".getBytes(StandardCharsets.UTF_8));
		}

		@GetMapping(params = "streamingSlow")
		StreamingResponseBody getStreamingSlow() {
			return os -> {
				os.write("name=Joe".getBytes());
				try {
					Thread.sleep(200);
					os.write("&someBoolean=true".getBytes(StandardCharsets.UTF_8));
				}
				catch (InterruptedException e) {
					/* no-op */
				}
			};
		}

		@GetMapping(params = "streamingJson")
		ResponseEntity<StreamingResponseBody> getStreamingJson() {
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
					.body(os -> os.write("{\"name\":\"Joe\",\"someDouble\":0.5}".getBytes(StandardCharsets.UTF_8)));
		}

		@GetMapping(params = "deferredResult")
		DeferredResult<Person> getDeferredResult() {
			DeferredResult<Person> result = new DeferredResult<>();
			delay(100, () -> result.setResult(new Person("Joe")));
			return result;
		}

		@GetMapping(params = "deferredResultWithImmediateValue")
		DeferredResult<Person> getDeferredResultWithImmediateValue() {
			DeferredResult<Person> result = new DeferredResult<>();
			result.setResult(new Person("Joe"));
			return result;
		}

		@GetMapping(params = "deferredResultWithDelayedError")
		DeferredResult<Person> getDeferredResultWithDelayedError() {
			DeferredResult<Person> result = new DeferredResult<>();
			delay(100, () -> result.setErrorResult(new RuntimeException("Delayed Error")));
			return result;
		}

		@GetMapping(params = "listenableFuture")
		@SuppressWarnings("deprecation")
		org.springframework.util.concurrent.ListenableFuture<Person> getListenableFuture() {
			org.springframework.util.concurrent.ListenableFutureTask<Person> futureTask =
					new org.springframework.util.concurrent.ListenableFutureTask<>(() -> new Person("Joe"));
			delay(100, futureTask);
			return futureTask;
		}

		@GetMapping(params = "completableFutureWithImmediateValue")
		CompletableFuture<Person> getCompletableFutureWithImmediateValue() {
			CompletableFuture<Person> future = new CompletableFuture<>();
			future.complete(new Person("Joe"));
			return future;
		}

		@ExceptionHandler(Exception.class)
		@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
		String errorHandler(Exception ex) {
			return ex.getMessage();
		}

		private void delay(long millis, Runnable task) {
			Mono.delay(Duration.ofMillis(millis)).doOnTerminate(task).subscribe();
		}
	}

}
