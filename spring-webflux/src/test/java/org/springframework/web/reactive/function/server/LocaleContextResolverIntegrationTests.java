/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.function.server;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.i18n.FixedLocaleContextResolver;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastien Deleuze
 */
class LocaleContextResolverIntegrationTests extends AbstractRouterFunctionIntegrationTests {

	private final WebClient webClient = WebClient.create();


	@Override
	protected RouterFunction<?> routerFunction() {
		return RouterFunctions.route(RequestPredicates.path("/"), this::render);
	}

	Mono<RenderingResponse> render(ServerRequest request) {
		return RenderingResponse.create("foo").build();
	}

	@Override
	protected HandlerStrategies handlerStrategies() {
		return HandlerStrategies.builder()
				.viewResolver(new DummyViewResolver())
				.localeContextResolver(new FixedLocaleContextResolver(Locale.GERMANY))
				.build();
	}

	@ParameterizedHttpServerTest
	void fixedLocale(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Mono<ResponseEntity<Void>> result = webClient
				.get()
				.uri("http://localhost:" + this.port + "/")
				.retrieve().toBodilessEntity();

		StepVerifier
				.create(result)
				.consumeNextWith(entity -> {
					assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
					assertThat(entity.getHeaders().getContentLanguage()).isEqualTo(Locale.GERMANY);
				})
				.verifyComplete();
	}


	private static class DummyViewResolver implements ViewResolver {

		@Override
		public Mono<View> resolveViewName(String viewName, Locale locale) {
			return Mono.just(new DummyView(locale));
		}
	}


	private static class DummyView implements View {

		private final Locale locale;

		public DummyView(Locale locale) {
			this.locale = locale;
		}

		@Override
		public List<MediaType> getSupportedMediaTypes() {
			return Collections.singletonList(MediaType.TEXT_HTML);
		}

		@Override
		public Mono<Void> render(@Nullable Map<String, ?> model, @Nullable MediaType contentType,
				ServerWebExchange exchange) {
			exchange.getResponse().getHeaders().setContentLanguage(locale);
			return Mono.empty();
		}
	}

}
