package org.springframework.web.reactive.function.client;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StringArrayDecodeTests {

	@Test
	void decodeNotEmptyList() {
		HttpServer server = HttpServer.create()
				.port(0)
				.route(routes -> routes.get("/not-empty", (req, res) ->
						res.addHeader("Content-Type", "application/json")
								.sendString(Mono.just("[\"hello\",\"world\"]"))
				));

		var disposable = server.bindNow();

		int port = disposable.port();

		WebClient client = WebClient.builder()
				.baseUrl("http://localhost:" + port)
				.build();

		List<String> values = client.get()
				.uri("/not-empty")
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(new ParameterizedTypeReference<@NotNull List<String>>() {})
				.block();

		assertThat(values).containsExactly("hello", "world");

		disposable.disposeNow();
	}

	@Test
	void decodeEmptyList() {
		HttpServer server = HttpServer.create()
				.port(0)
				.route(routes -> routes.get("/empty", (req, res) ->
						res.addHeader("Content-Type", "application/json")
								.sendString(Mono.just("[]"))
				));

		var disposable = server.bindNow();
		int port = disposable.port();

		WebClient client = WebClient.builder()
				.baseUrl("http://localhost:" + port)
				.build();

		List<String> values = client.get()
				.uri("/empty")
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(new ParameterizedTypeReference<@NotNull List<String>>() {})
				.block();

		assertThat(values).isEmpty();

		disposable.disposeNow();
	}

}
