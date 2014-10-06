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

package org.springframework.web.servlet.handler;

import org.junit.Before;
import org.junit.Test;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test fixture with {@link HandlerExceptionResolverComposite}.
 *
 * @author Karl Bennett
 * @since 4.1.1
 */
public class HandlerExceptionResolverCompositeTest {

	private HttpServletRequest request;
	private HttpServletResponse response;
	private Object handler;
	private Exception exception;
	private RuntimeException thrown;
	private HandlerExceptionResolver exceptionResolver;

    private HandlerExceptionResolverComposite resolver;

    @Before
    public void setUp() {
		request = mock(HttpServletRequest.class);
		response = mock(HttpServletResponse.class);
		handler = mock(Object.class);
		exception = mock(Exception.class);
		thrown = new FirstException("This exception should be caught and handled.");

		exceptionResolver = mock(HandlerExceptionResolver.class);


        resolver = new HandlerExceptionResolverComposite();
    }

    @Test
    public void testResolveExceptionThatThrowsResolvedException() {

        final ModelAndView expectedMav = mock(ModelAndView.class);

		when(exceptionResolver.resolveException(request, response, handler, exception)).thenThrow(thrown);
        when(exceptionResolver.resolveException(request, response, handler, thrown)).thenReturn(expectedMav);

		assertThrownExceptionResolved(expectedMav);
    }

	@Test
    public void testResolveExceptionThatThrowsResolvedWrappedException() {

        final ModelAndView expectedMav = mock(ModelAndView.class);

		when(exceptionResolver.resolveException(request, response, handler, exception))
				.thenThrow(new WrappedException(thrown));
        when(exceptionResolver.resolveException(request, response, handler, thrown)).thenReturn(expectedMav);

		assertThrownExceptionResolved(expectedMav);
    }

	private void assertThrownExceptionResolved(ModelAndView expected) {

		final ModelAndView mav = resolveException();

		assertEquals("An attempt should be made to handle any exceptions thrown by a handler.", expected, mav);
	}

	@Test(expected = SecondException.class)
    public void testResolveExceptionThatThrowsAnUnresolvedException() {

        final RuntimeException thrownSecond = new SecondException("This exception should be thrown out.");

		when(exceptionResolver.resolveException(request, response, handler, exception)).thenThrow(thrown);
        when(exceptionResolver.resolveException(request, response, handler, thrown)).thenThrow(thrownSecond);

		resolveException();
    }

	@Test(expected = WrappedException.class)
    public void testResolveExceptionThatThrowsAnUnresolvedWrappedException() {

        final RuntimeException thrownSecond = new SecondException("This exception should be thrown out.");

		when(exceptionResolver.resolveException(request, response, handler, exception)).thenThrow(thrown);
        when(exceptionResolver.resolveException(request, response, handler, thrown))
				.thenThrow(new WrappedException(thrownSecond));

		resolveException();
    }

	@Test(expected = SecondException.class)
    public void testPossibleInfiniteExceptionLoop() {

        final RuntimeException thrownSecond = new SecondException("This exception should be thrown out.");

		when(exceptionResolver.resolveException(request, response, handler, exception)).thenThrow(thrown);
        when(exceptionResolver.resolveException(request, response, handler, thrown)).thenThrow(thrownSecond);
		// This third resolve call would cause an infinite loop if executed. It should never be called.
        when(exceptionResolver.resolveException(request, response, handler, thrownSecond)).thenThrow(thrown);

		resolveException();
    }

	private ModelAndView resolveException() {

		resolver.setExceptionResolvers(singletonList(exceptionResolver));

		return resolver.resolveException(request, response, handler, exception);
	}

	@SuppressWarnings("serial")
	private static class FirstException extends RuntimeException {
		private FirstException(String message) {
			super(message);
		}
	}
	@SuppressWarnings("serial")
	private static class SecondException extends RuntimeException {
		private SecondException(String message) {
			super(message);
		}
	}
}
