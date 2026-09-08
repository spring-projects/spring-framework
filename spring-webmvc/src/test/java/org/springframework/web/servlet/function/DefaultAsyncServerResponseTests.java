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

package org.springframework.web.servlet.function;

import java.util.concurrent.CompletableFuture;

import jakarta.servlet.AsyncContext;
import org.junit.jupiter.api.Test;

import org.springframework.web.context.request.async.AsyncWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultAsyncServerResponse}.
 * @author Arjen Poutsma
 */
class DefaultAsyncServerResponseTests {

	@Test
	void writeAsyncReusesExistingAsyncWebRequestTimeout() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.setAsyncSupported(true);
		MockHttpServletResponse response = new MockHttpServletResponse();

		AsyncWebRequest existingAsyncWebRequest = WebAsyncUtils.createAsyncWebRequest(request, response);
		existingAsyncWebRequest.setTimeout(1000L);
		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		asyncManager.setAsyncWebRequest(existingAsyncWebRequest);

		DeferredResult<Object> deferredResult = new DeferredResult<>();
		DefaultAsyncServerResponse.writeAsync(request, response, deferredResult);

		assertThat(asyncManager.getAsyncWebRequest()).isSameAs(existingAsyncWebRequest);
		AsyncContext asyncContext = request.getAsyncContext();
		assertThat(asyncContext.getTimeout()).isEqualTo(1000L);
	}

	@Test
	void blockCompleted() {
		ServerResponse wrappee = ServerResponse.ok().build();
		CompletableFuture<ServerResponse> future = CompletableFuture.completedFuture(wrappee);
		AsyncServerResponse response = AsyncServerResponse.create(future);

		assertThat(response.block()).isSameAs(wrappee);
	}

	@Test
	void blockNotCompleted() {
		ServerResponse wrappee = ServerResponse.ok().build();
		CompletableFuture<ServerResponse> future = CompletableFuture.supplyAsync(() -> {
			try {
				Thread.sleep(500);
				return wrappee;
			}
			catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
		});
		AsyncServerResponse response = AsyncServerResponse.create(future);

		assertThat(response.block()).isSameAs(wrappee);
	}

}
