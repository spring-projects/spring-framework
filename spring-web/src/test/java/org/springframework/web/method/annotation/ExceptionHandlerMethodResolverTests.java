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

package org.springframework.web.method.annotation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.BindException;
import java.net.SocketException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ExceptionHandlerMethodResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
class ExceptionHandlerMethodResolverTests {

	@Test
	void shouldResolveMethodFromAnnotationAttribute() {
		ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(ExceptionController.class);
		IOException exception = new IOException();
		assertThat(resolver.resolveMethod(exception).getName()).isEqualTo("handleIOException");
	}

	@Test
	void shouldResolveMethodFromMethodArgument() {
		ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(ExceptionController.class);
		IllegalArgumentException exception = new IllegalArgumentException();
		assertThat(resolver.resolveMethod(exception).getName()).isEqualTo("handleIllegalArgumentException");
	}

	@Test
	void shouldResolveMethodWithExceptionSubType() {
		ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(ExceptionController.class);
		IOException ioException = new FileNotFoundException();
		assertThat(resolver.resolveMethod(ioException).getName()).isEqualTo("handleIOException");
		SocketException bindException = new BindException();
		assertThat(resolver.resolveMethod(bindException).getName()).isEqualTo("handleSocketException");
	}

	@Test
	void shouldResolveMethodWithExceptionBestMatch() {
		ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(ExceptionController.class);
		SocketException exception = new SocketException();
		assertThat(resolver.resolveMethod(exception).getName()).isEqualTo("handleSocketException");
	}

	@Test
	void shouldNotResolveMethodWhenExceptionNoMatch() {
		ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(ExceptionController.class);
		Exception exception = new Exception();
		assertThat(resolver.resolveMethod(exception)).as("1st lookup").isNull();
		assertThat(resolver.resolveMethod(exception)).as("2nd lookup from cache").isNull();
	}

	@Test
	void ShouldResolveMethodWithExceptionCause() {
		ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(ExceptionController.class);

		SocketException bindException = new BindException();
		bindException.initCause(new FileNotFoundException());

		Exception exception = new Exception(new Exception(new Exception(bindException)));

		assertThat(resolver.resolveMethod(exception).getName()).isEqualTo("handleSocketException");
	}

	@Test
	void shouldResolveMethodFromSuperClass() {
		ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(InheritedController.class);
		IOException exception = new IOException();
		assertThat(resolver.resolveMethod(exception).getName()).isEqualTo("handleIOException");
	}

	@Test
	void shouldThrowExceptionWhenAmbiguousExceptionMapping() {
		assertThatIllegalStateException().isThrownBy(() ->
				new ExceptionHandlerMethodResolver(AmbiguousController.class));
	}

	@Test
	void shouldThrowExceptionWhenNoExceptionMapping() {
		assertThatIllegalStateException().isThrownBy(() ->
				new ExceptionHandlerMethodResolver(NoExceptionController.class));
	}

	@Test
	void shouldResolveMethodWithMediaType() {
		ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(MediaTypeController.class);
		assertThat(resolver.resolveExceptionMapping(new IllegalArgumentException(), MediaType.APPLICATION_JSON).getHandlerMethod().getName()).isEqualTo("handleJson");
		assertThat(resolver.resolveExceptionMapping(new IllegalArgumentException(), MediaType.TEXT_HTML).getHandlerMethod().getName()).isEqualTo("handleHtml");
	}

	@Test
	void shouldResolveMethodWithCompatibleMediaType() {
		ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(MediaTypeController.class);
		assertThat(resolver.resolveExceptionMapping(new IllegalArgumentException(), MediaType.parseMediaType("application/*")).getHandlerMethod().getName()).isEqualTo("handleJson");
	}

	@Test
	void shouldFavorMethodWithExplicitAcceptAll() {
		ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(MediaTypeController.class);
		assertThat(resolver.resolveExceptionMapping(new IllegalArgumentException(), MediaType.ALL).getHandlerMethod().getName()).isEqualTo("handleHtml");
	}

	@Test
	void shouldThrowExceptionWhenInvalidMediaTypeMapping() {
		assertThatIllegalStateException().isThrownBy(() ->
				new ExceptionHandlerMethodResolver(InvalidMediaTypeController.class))
				.withMessageContaining("Invalid media type [invalid-mediatype] declared on @ExceptionHandler");
	}

	@Test
	void shouldThrowExceptionWhenAmbiguousMediaTypeMapping() {
		assertThatIllegalStateException().isThrownBy(() ->
				new ExceptionHandlerMethodResolver(AmbiguousMediaTypeController.class))
				.withMessageContaining("Ambiguous @ExceptionHandler method mapped for [ExceptionHandler{exceptionType=java.lang.IllegalArgumentException, mediaType=application/json}]")
				.withMessageContaining("AmbiguousMediaTypeController.handleJson()")
				.withMessageContaining("AmbiguousMediaTypeController.handleJsonToo()");
	}

	@Test
	void shouldResolveMethodWithMediaTypeFallback() {
		ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(MixedController.class);
		assertThat(resolver.resolveExceptionMapping(new IllegalArgumentException(), MediaType.TEXT_HTML).getHandlerMethod().getName()).isEqualTo("handleOther");
	}


	@Controller
	static class ExceptionController {

		public void handle() {}

		@ExceptionHandler(IOException.class)
		public void handleIOException() {
		}

		@ExceptionHandler(SocketException.class)
		public void handleSocketException() {
		}

		@ExceptionHandler
		public void handleIllegalArgumentException(IllegalArgumentException exception) {
		}
	}


	@Controller
	static class InheritedController extends ExceptionController {

		@Override
		public void handleIOException() {
		}
	}


	@Controller
	static class AmbiguousController {

		public void handle() {}

		@ExceptionHandler({BindException.class, IllegalArgumentException.class})
		public String handle1(Exception ex, HttpServletRequest request, HttpServletResponse response) {
			return ClassUtils.getShortName(ex.getClass());
		}

		@ExceptionHandler
		public String handle2(IllegalArgumentException ex) {
			return ClassUtils.getShortName(ex.getClass());
		}
	}


	@Controller
	static class NoExceptionController {

		@ExceptionHandler
		public void handle() {
		}
	}

	@Controller
	static class MediaTypeController {

		@ExceptionHandler(exception = {IllegalArgumentException.class}, produces = "application/json")
		public void handleJson() {

		}

		@ExceptionHandler(exception = {IllegalArgumentException.class}, produces = {"text/html", "*/*"})
		public void handleHtml() {

		}

	}

	@Controller
	static class AmbiguousMediaTypeController {

		@ExceptionHandler(exception = {IllegalArgumentException.class}, produces = "application/json")
		public void handleJson() {

		}

		@ExceptionHandler(exception = {IllegalArgumentException.class}, produces = "application/json")
		public void handleJsonToo() {

		}

	}

	@Controller
	static class MixedController {

		@ExceptionHandler(exception = {IllegalArgumentException.class}, produces = "application/json")
		public void handleJson() {

		}

		@ExceptionHandler(IllegalArgumentException.class)
		public void handleOther() {

		}

	}

	@Controller
	static class InvalidMediaTypeController {

		@ExceptionHandler(exception = {IllegalArgumentException.class}, produces = "invalid-mediatype")
		public void handle() {

		}
	}

}
