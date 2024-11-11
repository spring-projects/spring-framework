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

package org.springframework.messaging.handler.annotation.support;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.BindException;
import java.net.SocketException;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Test fixture for {@link AnnotationExceptionHandlerMethodResolver} tests.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
class AnnotationExceptionHandlerMethodResolverTests {

	private final AnnotationExceptionHandlerMethodResolver resolver =
			new AnnotationExceptionHandlerMethodResolver(ExceptionController.class);


	@Test
	void resolveMethodFromAnnotation() {
		IOException exception = new IOException();
		assertThat(this.resolver.resolveMethod(exception).getName()).isEqualTo("handleIOException");
	}

	@Test
	void resolveMethodFromArgument() {
		IllegalArgumentException exception = new IllegalArgumentException();
		assertThat(this.resolver.resolveMethod(exception).getName()).isEqualTo("handleIllegalArgumentException");
	}

	@Test
	void resolveMethodFromArgumentWithErrorType() {
		AssertionError exception = new AssertionError();
		assertThat(this.resolver.resolveMethod(new IllegalStateException(exception)).getName()).isEqualTo("handleAssertionError");
	}

	@Test
	void resolveMethodExceptionSubType() {
		IOException ioException = new FileNotFoundException();
		assertThat(this.resolver.resolveMethod(ioException).getName()).isEqualTo("handleIOException");
		SocketException bindException = new BindException();
		assertThat(this.resolver.resolveMethod(bindException).getName()).isEqualTo("handleSocketException");
	}

	@Test
	void resolveMethodBestMatch() {
		SocketException exception = new SocketException();
		assertThat(this.resolver.resolveMethod(exception).getName()).isEqualTo("handleSocketException");
	}

	@Test
	void resolveMethodNoMatch() {
		Exception exception = new Exception();
		assertThat(this.resolver.resolveMethod(exception)).as("1st lookup").isNull();
		assertThat(this.resolver.resolveMethod(exception)).as("2nd lookup from cache").isNull();
	}

	@Test
	void resolveMethodInherited() {
		IOException exception = new IOException();
		assertThat(this.resolver.resolveMethod(exception).getName()).isEqualTo("handleIOException");
	}

	@Test
	void resolveMethodAgainstCause() {
		IllegalStateException exception = new IllegalStateException(new IOException());
		assertThat(this.resolver.resolveMethod(exception).getName()).isEqualTo("handleIOException");
	}

	@Test
	void ambiguousExceptionMapping() {
		assertThatIllegalStateException().isThrownBy(() ->
				new AnnotationExceptionHandlerMethodResolver(AmbiguousController.class));
	}

	@Test
	void noExceptionMapping() {
		assertThatIllegalStateException().isThrownBy(() ->
				new AnnotationExceptionHandlerMethodResolver(NoExceptionController.class));
	}


	@Controller
	@SuppressWarnings("unused")
	static class ExceptionController {

		public void handle() {}

		@MessageExceptionHandler(IOException.class)
		public void handleIOException() {
		}

		@MessageExceptionHandler(SocketException.class)
		public void handleSocketException() {
		}

		@MessageExceptionHandler
		public void handleIllegalArgumentException(IllegalArgumentException exception) {
		}

		@MessageExceptionHandler
		public void handleAssertionError(AssertionError exception) {
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

		@MessageExceptionHandler({BindException.class, IllegalArgumentException.class})
		public String handle1(Exception ex) throws IOException {
			return ClassUtils.getShortName(ex.getClass());
		}

		@MessageExceptionHandler
		public String handle2(IllegalArgumentException ex) {
			return ClassUtils.getShortName(ex.getClass());
		}
	}


	@Controller
	static class NoExceptionController {

		@MessageExceptionHandler
		public void handle() {
		}
	}

}
