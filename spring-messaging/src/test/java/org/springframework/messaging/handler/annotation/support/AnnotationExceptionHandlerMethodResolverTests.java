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

package org.springframework.messaging.handler.annotation.support;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.BindException;
import java.net.SocketException;

import org.junit.Test;

import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link AnnotationExceptionHandlerMethodResolver} tests.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class AnnotationExceptionHandlerMethodResolverTests {

	private final AnnotationExceptionHandlerMethodResolver resolver =
			new AnnotationExceptionHandlerMethodResolver(ExceptionController.class);


	@Test
	public void resolveMethodFromAnnotation() {
		IOException exception = new IOException();
		assertEquals("handleIOException", this.resolver.resolveMethod(exception).getName());
	}

	@Test
	public void resolveMethodFromArgument() {
		IllegalArgumentException exception = new IllegalArgumentException();
		assertEquals("handleIllegalArgumentException", this.resolver.resolveMethod(exception).getName());
	}

	@Test
	public void resolveMethodFromArgumentWithErrorType() {
		AssertionError exception = new AssertionError();
		assertEquals("handleAssertionError", this.resolver.resolveMethod(new IllegalStateException(exception)).getName());
	}

	@Test
	public void resolveMethodExceptionSubType() {
		IOException ioException = new FileNotFoundException();
		assertEquals("handleIOException", this.resolver.resolveMethod(ioException).getName());
		SocketException bindException = new BindException();
		assertEquals("handleSocketException", this.resolver.resolveMethod(bindException).getName());
	}

	@Test
	public void resolveMethodBestMatch() {
		SocketException exception = new SocketException();
		assertEquals("handleSocketException", this.resolver.resolveMethod(exception).getName());
	}

	@Test
	public void resolveMethodNoMatch() {
		Exception exception = new Exception();
		assertNull("1st lookup", this.resolver.resolveMethod(exception));
		assertNull("2nd lookup from cache", this.resolver.resolveMethod(exception));
	}

	@Test
	public void resolveMethodInherited() {
		IOException exception = new IOException();
		assertEquals("handleIOException", this.resolver.resolveMethod(exception).getName());
	}

	@Test
	public void resolveMethodAgainstCause() {
		IllegalStateException exception = new IllegalStateException(new IOException());
		assertEquals("handleIOException", this.resolver.resolveMethod(exception).getName());
	}

	@Test(expected = IllegalStateException.class)
	public void ambiguousExceptionMapping() {
		new AnnotationExceptionHandlerMethodResolver(AmbiguousController.class);
	}

	@Test(expected = IllegalStateException.class)
	public void noExceptionMapping() {
		new AnnotationExceptionHandlerMethodResolver(NoExceptionController.class);
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
		public void handleIOException()	{
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
