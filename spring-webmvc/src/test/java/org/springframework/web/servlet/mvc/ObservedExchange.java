/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.servlet.mvc;

import java.util.concurrent.atomic.AtomicReference;

import io.micrometer.observation.tck.TestObservationRegistry;
import org.jspecify.annotations.Nullable;

import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.web.filter.ServerHttpObservationFilter;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

/**
 * Test support that runs an action inside a {@link ServerHttpObservationFilter}
 * so that errors recorded by the action are reported the way the filter does
 * it when an exchange completes.
 *
 * @param <T> the type of the action result
 * @param result the result of the action
 * @param error the error reported to the observation, if any
 * @author Brian Clozel
 */
public record ObservedExchange<T>(@Nullable T result, @Nullable Throwable error) {

	/**
	 * Run the given action within a {@link ServerHttpObservationFilter}.
	 */
	public static <T> ObservedExchange<T> observe(MockHttpServletRequest request,
			MockHttpServletResponse response, Action<T> action) throws Exception {

		TestObservationRegistry registry = TestObservationRegistry.create();
		AtomicReference<@Nullable T> result = new AtomicReference<>();
		AtomicReference<@Nullable Exception> failure = new AtomicReference<>();
		new ServerHttpObservationFilter(registry).doFilter(request, response, (req, res) -> {
			try {
				result.set(action.run());
			}
			catch (Exception ex) {
				failure.set(ex);
			}
		});
		Exception exception = failure.get();
		if (exception != null) {
			throw exception;
		}
		ServerRequestObservationContext context =
				ServerHttpObservationFilter.findObservationContext(request).orElseThrow();
		return new ObservedExchange<>(result.get(), context.getError());
	}


	@FunctionalInterface
	public interface Action<T> {

		@Nullable T run() throws Exception;

	}

}
