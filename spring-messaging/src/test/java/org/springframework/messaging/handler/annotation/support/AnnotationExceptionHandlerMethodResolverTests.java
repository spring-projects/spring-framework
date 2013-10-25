/*
 * Copyright 2002-2013 the original author or authors.
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
 */
public class AnnotationExceptionHandlerMethodResolverTests {

	@Test
	public void resolveMethodFromAnnotation() {
		AnnotationExceptionHandlerMethodResolver resolver = new AnnotationExceptionHandlerMethodResolver(ExceptionController.class);
		IOException exception = new IOException();
		assertEquals("handleIOException", resolver.resolveMethod(exception).getName());
	}

	@Test
	public void resolveMethodFromArgument() {
		AnnotationExceptionHandlerMethodResolver resolver = new AnnotationExceptionHandlerMethodResolver(ExceptionController.class);
		IllegalArgumentException exception = new IllegalArgumentException();
		assertEquals("handleIllegalArgumentException", resolver.resolveMethod(exception).getName());
	}

	@Test
	public void resolveMethodExceptionSubType() {
		AnnotationExceptionHandlerMethodResolver resolver = new AnnotationExceptionHandlerMethodResolver(ExceptionController.class);
		IOException ioException = new FileNotFoundException();
		assertEquals("handleIOException", resolver.resolveMethod(ioException).getName());
		SocketException bindException = new BindException();
		assertEquals("handleSocketException", resolver.resolveMethod(bindException).getName());
	}

	@Test
	public void resolveMethodBestMatch() {
		AnnotationExceptionHandlerMethodResolver resolver = new AnnotationExceptionHandlerMethodResolver(ExceptionController.class);
		SocketException exception = new SocketException();
		assertEquals("handleSocketException", resolver.resolveMethod(exception).getName());
	}

	@Test
	public void resolveMethodNoMatch() {
		AnnotationExceptionHandlerMethodResolver resolver = new AnnotationExceptionHandlerMethodResolver(ExceptionController.class);
		Exception exception = new Exception();
		assertNull("1st lookup", resolver.resolveMethod(exception));
		assertNull("2nd lookup from cache", resolver.resolveMethod(exception));
	}

	@Test
	public void resolveMethodInherited() {
		AnnotationExceptionHandlerMethodResolver resolver = new AnnotationExceptionHandlerMethodResolver(InheritedController.class);
		IOException exception = new IOException();
		assertEquals("handleIOException", resolver.resolveMethod(exception).getName());
	}

	@Test(expected = IllegalStateException.class)
	public void ambiguousExceptionMapping() {
		new AnnotationExceptionHandlerMethodResolver(AmbiguousController.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void noExceptionMapping() {
		new AnnotationExceptionHandlerMethodResolver(NoExceptionController.class);
	}

	@Controller
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
