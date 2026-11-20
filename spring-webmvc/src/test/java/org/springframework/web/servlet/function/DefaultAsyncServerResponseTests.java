/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
