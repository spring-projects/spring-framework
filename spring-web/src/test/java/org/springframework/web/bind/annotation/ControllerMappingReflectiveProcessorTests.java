/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.bind.annotation;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.http.HttpEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ControllerMappingReflectiveProcessor}.
 *
 * @author Sebastien Deleuze
 */
public class ControllerMappingReflectiveProcessorTests {

	private final ControllerMappingReflectiveProcessor processor = new ControllerMappingReflectiveProcessor();

	private final ReflectionHints hints = new ReflectionHints();

	@Test
	void registerReflectiveHintsForMethodWithResponseBody() throws NoSuchMethodException {
		Method method = SampleController.class.getDeclaredMethod("get");
		processor.registerReflectionHints(hints, method);
		assertThat(hints.typeHints()).satisfiesExactlyInAnyOrder(
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(Response.class));
					assertThat(typeHint.getMemberCategories()).containsExactlyInAnyOrder(
							MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
							MemberCategory.DECLARED_FIELDS);
					assertThat(typeHint.methods()).satisfiesExactlyInAnyOrder(
							hint -> assertThat(hint.getName()).isEqualTo("getMessage"),
							hint -> assertThat(hint.getName()).isEqualTo("setMessage"));
				},
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class)));
	}

	@Test
	void registerReflectiveHintsForMethodWithRequestBody() throws NoSuchMethodException {
		Method method = SampleController.class.getDeclaredMethod("post", Request.class);
		processor.registerReflectionHints(hints, method);
		assertThat(hints.typeHints()).satisfiesExactlyInAnyOrder(
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(Request.class));
					assertThat(typeHint.getMemberCategories()).containsExactlyInAnyOrder(
							MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
							MemberCategory.DECLARED_FIELDS);
					assertThat(typeHint.methods()).satisfiesExactlyInAnyOrder(
							hint -> assertThat(hint.getName()).isEqualTo("getMessage"),
							hint -> assertThat(hint.getName()).isEqualTo("setMessage"));
				},
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class)));
	}

	@Test
	void registerReflectiveHintsForMethodWithModelAttribute() throws NoSuchMethodException {
		Method method = SampleController.class.getDeclaredMethod("postForm", Request.class);
		processor.registerReflectionHints(hints, method);
		assertThat(hints.typeHints()).satisfiesExactlyInAnyOrder(
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(Request.class));
					assertThat(typeHint.getMemberCategories()).containsExactlyInAnyOrder(
							MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
							MemberCategory.DECLARED_FIELDS);
					assertThat(typeHint.methods()).satisfiesExactlyInAnyOrder(
							hint -> assertThat(hint.getName()).isEqualTo("getMessage"),
							hint -> assertThat(hint.getName()).isEqualTo("setMessage"));
				},
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class)));
	}

	@Test
	void registerReflectiveHintsForMethodWithRestController() throws NoSuchMethodException {
		Method method = SampleRestController.class.getDeclaredMethod("get");
		processor.registerReflectionHints(hints, method);
		assertThat(hints.typeHints()).satisfiesExactlyInAnyOrder(
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleRestController.class)),
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(Response.class));
					assertThat(typeHint.getMemberCategories()).containsExactlyInAnyOrder(
							MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
							MemberCategory.DECLARED_FIELDS);
					assertThat(typeHint.methods()).satisfiesExactlyInAnyOrder(
							hint -> assertThat(hint.getName()).isEqualTo("getMessage"),
							hint -> assertThat(hint.getName()).isEqualTo("setMessage"));
				},
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class)));
	}

	@Test
	void registerReflectiveHintsForMethodWithString() throws NoSuchMethodException {
		Method method = SampleController.class.getDeclaredMethod("message");
		processor.registerReflectionHints(hints, method);
		assertThat(hints.typeHints()).satisfiesExactlyInAnyOrder(
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class));
					assertThat(typeHint.constructors()).isEmpty();
					assertThat(typeHint.fields()).isEmpty();
					assertThat(typeHint.methods()).isEmpty();
				});
	}

	@Test
	void registerReflectiveHintsForClassWithMapping() {
		processor.registerReflectionHints(hints, SampleControllerWithClassMapping.class);
		assertThat(hints.typeHints()).singleElement().satisfies(typeHint ->
				assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleControllerWithClassMapping.class)));
	}

	@Test
	void registerReflectiveHintsForMethodReturningHttpEntity() throws NoSuchMethodException {
		Method method = SampleController.class.getDeclaredMethod("getHttpEntity");
		processor.registerReflectionHints(hints, method);
		assertThat(hints.typeHints()).satisfiesExactlyInAnyOrder(
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(Response.class));
					assertThat(typeHint.getMemberCategories()).containsExactlyInAnyOrder(
							MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
							MemberCategory.DECLARED_FIELDS);
					assertThat(typeHint.methods()).satisfiesExactlyInAnyOrder(
							hint -> assertThat(hint.getName()).isEqualTo("getMessage"),
							hint -> assertThat(hint.getName()).isEqualTo("setMessage"));
				},
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class)));
	}

	@Test
	void registerReflectiveHintsForMethodReturningRawHttpEntity() throws NoSuchMethodException {
		Method method = SampleController.class.getDeclaredMethod("getRawHttpEntity");
		processor.registerReflectionHints(hints, method);
		assertThat(hints.typeHints()).singleElement().satisfies(
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleController.class)));
	}

	@Test
	void registerReflectiveHintsForMethodWithHttpEntityParameter() throws NoSuchMethodException {
		Method method = SampleController.class.getDeclaredMethod("postHttpEntity", HttpEntity.class);
		processor.registerReflectionHints(hints, method);
		assertThat(hints.typeHints()).satisfiesExactlyInAnyOrder(
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(Request.class));
					assertThat(typeHint.getMemberCategories()).containsExactlyInAnyOrder(
							MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
							MemberCategory.DECLARED_FIELDS);
					assertThat(typeHint.methods()).satisfiesExactlyInAnyOrder(
							hint -> assertThat(hint.getName()).isEqualTo("getMessage"),
							hint -> assertThat(hint.getName()).isEqualTo("setMessage"));
				},
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class)));
	}

	@Test
	void registerReflectiveHintsForMethodWithRawHttpEntityParameter() throws NoSuchMethodException {
		Method method = SampleController.class.getDeclaredMethod("postRawHttpEntity", HttpEntity.class);
		processor.registerReflectionHints(hints, method);
		assertThat(hints.typeHints()).singleElement().satisfies(
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleController.class)));
	}

	@Test
	void registerReflectiveHintsForMethodWithPartToConvert() throws NoSuchMethodException {
		Method method = SampleController.class.getDeclaredMethod("postPartToConvert", Request.class);
		processor.registerReflectionHints(hints, method);
		assertThat(hints.typeHints()).satisfiesExactlyInAnyOrder(
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleController.class)),
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(Request.class));
					assertThat(typeHint.getMemberCategories()).containsExactlyInAnyOrder(
							MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
							MemberCategory.DECLARED_FIELDS);
					assertThat(typeHint.methods()).satisfiesExactlyInAnyOrder(
							hint -> assertThat(hint.getName()).isEqualTo("getMessage"),
							hint -> assertThat(hint.getName()).isEqualTo("setMessage"));
				},
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class)));
	}

	static class SampleController {

		@GetMapping
		@ResponseBody
		Response get() {
			return new Response("response");
		}

		@PostMapping
		void post(@RequestBody Request request) {
		}

		@PostMapping
		void postForm(@ModelAttribute Request request) {
		}

		@GetMapping
		@ResponseBody
		String message() {
			return "";
		}

		@GetMapping
		HttpEntity<Response> getHttpEntity() {
			return new HttpEntity<>(new Response("response"));
		}

		@GetMapping
		@SuppressWarnings({ "rawtypes", "unchecked" })
		HttpEntity getRawHttpEntity() {
			return new HttpEntity(new Response("response"));
		}

		@PostMapping
		void postHttpEntity(HttpEntity<Request>  entity) {
		}

		@PostMapping
		@SuppressWarnings({ "rawtypes", "unchecked" })
		void postRawHttpEntity(HttpEntity  entity) {
		}

		@PostMapping
		void postPartToConvert(@RequestPart Request request) {
		}

	}

	@RestController
	static class SampleRestController {

		@GetMapping
		Response get() {
			return new Response("response");
		}
	}

	@RequestMapping("/prefix")
	static class SampleControllerWithClassMapping {
	}

	static class Request {

		private String message;

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	static class Response {

		private String message;

		public Response(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}
}
