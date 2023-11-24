/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.context.request;

import jakarta.servlet.ServletRequestEvent;
import org.junit.jupiter.api.Test;

import org.springframework.core.task.MockRunnable;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
public class RequestContextListenerTests {

	@Test
	public void requestContextListenerWithSameThread() {
		RequestContextListener listener = new RequestContextListener();
		MockServletContext context = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(context);
		request.setAttribute("test", "value");

		assertThat(RequestContextHolder.getRequestAttributes()).isNull();
		listener.requestInitialized(new ServletRequestEvent(context, request));
		assertThat(RequestContextHolder.getRequestAttributes()).isNotNull();
		assertThat(RequestContextHolder.getRequestAttributes().getAttribute("test", RequestAttributes.SCOPE_REQUEST)).isEqualTo("value");
		MockRunnable runnable = new MockRunnable();
		RequestContextHolder.getRequestAttributes().registerDestructionCallback(
				"test", runnable, RequestAttributes.SCOPE_REQUEST);

		listener.requestDestroyed(new ServletRequestEvent(context, request));
		assertThat(RequestContextHolder.getRequestAttributes()).isNull();
		assertThat(runnable.wasExecuted()).isTrue();
	}

	@Test
	public void requestContextListenerWithSameThreadAndAttributesGone() {
		RequestContextListener listener = new RequestContextListener();
		MockServletContext context = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(context);
		request.setAttribute("test", "value");

		assertThat(RequestContextHolder.getRequestAttributes()).isNull();
		listener.requestInitialized(new ServletRequestEvent(context, request));
		assertThat(RequestContextHolder.getRequestAttributes()).isNotNull();
		assertThat(RequestContextHolder.getRequestAttributes().getAttribute("test", RequestAttributes.SCOPE_REQUEST)).isEqualTo("value");
		MockRunnable runnable = new MockRunnable();
		RequestContextHolder.getRequestAttributes().registerDestructionCallback(
				"test", runnable, RequestAttributes.SCOPE_REQUEST);

		request.clearAttributes();
		listener.requestDestroyed(new ServletRequestEvent(context, request));
		assertThat(RequestContextHolder.getRequestAttributes()).isNull();
		assertThat(runnable.wasExecuted()).isTrue();
	}

	@Test
	public void requestContextListenerWithDifferentThread() {
		final RequestContextListener listener = new RequestContextListener();
		final MockServletContext context = new MockServletContext();
		final MockHttpServletRequest request = new MockHttpServletRequest(context);
		request.setAttribute("test", "value");

		assertThat(RequestContextHolder.getRequestAttributes()).isNull();
		listener.requestInitialized(new ServletRequestEvent(context, request));
		assertThat(RequestContextHolder.getRequestAttributes()).isNotNull();
		assertThat(RequestContextHolder.getRequestAttributes().getAttribute("test", RequestAttributes.SCOPE_REQUEST)).isEqualTo("value");
		MockRunnable runnable = new MockRunnable();
		RequestContextHolder.getRequestAttributes().registerDestructionCallback(
				"test", runnable, RequestAttributes.SCOPE_REQUEST);

		// Execute requestDestroyed callback in different thread.
		Thread thread = new Thread() {
			@Override
			public void run() {
				listener.requestDestroyed(new ServletRequestEvent(context, request));
			}
		};
		thread.start();
		try {
			thread.join();
		}
		catch (InterruptedException ex) {
		}
		// Still bound to original thread, but at least completed.
		assertThat(RequestContextHolder.getRequestAttributes()).isNotNull();
		assertThat(runnable.wasExecuted()).isTrue();

		// Check that a repeated execution in the same thread works and performs cleanup.
		listener.requestInitialized(new ServletRequestEvent(context, request));
		listener.requestDestroyed(new ServletRequestEvent(context, request));
		assertThat(RequestContextHolder.getRequestAttributes()).isNull();
	}

}
