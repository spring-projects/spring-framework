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

package org.springframework.web.reactive.function.server;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.bootstrap.HttpServer;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.reactive.function.server.HandlerFilterFunction.ofResponseProcessor;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * @author Arjen Poutsma
 * @since 5.0
 */
class RenderingResponseIntegrationTests extends AbstractRouterFunctionIntegrationTests {

	private final RestTemplate restTemplate = new RestTemplate();


	@Override
	protected RouterFunction<?> routerFunction() {
		RenderingResponseHandler handler = new RenderingResponseHandler();
		RouterFunction<RenderingResponse> normalRoute = route(GET("/normal"), handler::render);
		RouterFunction<RenderingResponse> filteredRoute = route(GET("/filter"), handler::render)
				.filter(ofResponseProcessor(
						response -> RenderingResponse.from(response)
								.modelAttribute("qux", "quux")
								.build()));

		return normalRoute.and(filteredRoute);
	}

	@Override
	protected HandlerStrategies handlerStrategies() {
		return HandlerStrategies.builder()
				.viewResolver(new DummyViewResolver())
				.build();
	}


	@ParameterizedHttpServerTest
	void normal(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<String> result =
				restTemplate.getForEntity("http://localhost:" + port + "/normal", String.class);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		Map<String, String> body = parseBody(result.getBody());
		assertThat(body.size()).isEqualTo(2);
		assertThat(body.get("name")).isEqualTo("foo");
		assertThat(body.get("bar")).isEqualTo("baz");
	}

	@ParameterizedHttpServerTest
	void filter(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<String> result =
				restTemplate.getForEntity("http://localhost:" + port + "/filter", String.class);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		Map<String, String> body = parseBody(result.getBody());
		assertThat(body.size()).isEqualTo(3);
		assertThat(body.get("name")).isEqualTo("foo");
		assertThat(body.get("bar")).isEqualTo("baz");
		assertThat(body.get("qux")).isEqualTo("quux");
	}

	private Map<String, String> parseBody(String body) {
		String[] lines = body.split("\\n");
		Map<String, String> result = new LinkedHashMap<>(lines.length);
		for (String line : lines) {
			int idx = line.indexOf('=');
			String key = line.substring(0, idx);
			String value = line.substring(idx + 1);
			result.put(key, value);
		}
		return result;
	}


	private static class RenderingResponseHandler {

		public Mono<RenderingResponse> render(ServerRequest request) {
			return RenderingResponse.create("foo")
					.modelAttribute("bar", "baz")
					.build();
		}
	}

	private static class DummyViewResolver implements ViewResolver {

		@Override
		public Mono<View> resolveViewName(String viewName, Locale locale) {
			return Mono.just(new DummyView(viewName));
		}
	}


	private static class DummyView implements View {

		private final String name;

		public DummyView(String name) {
			this.name = name;
		}

		@Override
		public List<MediaType> getSupportedMediaTypes() {
			return Collections.singletonList(MediaType.TEXT_PLAIN);
		}

		@Override
		public Mono<Void> render(@Nullable Map<String, ?> model, @Nullable MediaType contentType,
				ServerWebExchange exchange) {
			StringBuilder builder = new StringBuilder();
			builder.append("name=").append(this.name).append('\n');
			for (Map.Entry<String, ?> entry : model.entrySet()) {
				builder.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
			}
			builder.setLength(builder.length() - 1);
			byte[] bytes = builder.toString().getBytes(StandardCharsets.UTF_8);

			ServerHttpResponse response = exchange.getResponse();
			DataBuffer buffer = response.bufferFactory().wrap(bytes);
			response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
			return response.writeWith(Mono.just(buffer));
		}
	}

}
