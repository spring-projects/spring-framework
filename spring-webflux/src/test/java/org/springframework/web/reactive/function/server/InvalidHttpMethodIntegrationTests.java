/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.function.server;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class InvalidHttpMethodIntegrationTests extends AbstractRouterFunctionIntegrationTests {

	@Override
	protected RouterFunction<?> routerFunction() {
		return RouterFunctions.route(RequestPredicates.GET("/"),
				request -> ServerResponse.ok().bodyValue("FOO"))
				.andRoute(RequestPredicates.all(), request -> ServerResponse.ok().bodyValue("BAR"));
	}

	@ParameterizedHttpServerTest
	void invalidHttpMethod(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		OkHttpClient client = new OkHttpClient();

		Request request = new Request.Builder()
				.method("BAZ", null)
				.url("http://localhost:" + port + "/")
				.build();

		try (Response response = client.newCall(request).execute()) {
			assertThat(response.body().string()).isEqualTo("BAR");
		}
	}

}
