/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.view;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurationSupport;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.result.method.annotation.AbstractRequestMappingIntegrationTests;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.i18n.FixedLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastien Deleuze
 */
class LocaleContextResolverIntegrationTests extends AbstractRequestMappingIntegrationTests {

	private final WebClient webClient = WebClient.create();


	@Override
	protected ApplicationContext initApplicationContext() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(WebConfig.class);
		context.refresh();
		return context;
	}


	@ParameterizedHttpServerTest
	void fixedLocale(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Mono<ResponseEntity<Void>> result = webClient
				.get()
				.uri("http://localhost:" + this.port + "/")
				.retrieve()
				.toBodilessEntity();

		StepVerifier.create(result)
				.consumeNextWith(entity -> {
					assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
					assertThat(entity.getHeaders().getContentLanguage()).isEqualTo(Locale.GERMANY);
				})
				.verifyComplete();
	}


	@Configuration
	@ComponentScan(resourcePattern = "**/LocaleContextResolverIntegrationTests*.class")
	@SuppressWarnings({"unused", "WeakerAccess"})
	static class WebConfig extends WebFluxConfigurationSupport {

		@Override
		protected LocaleContextResolver createLocaleContextResolver() {
			return new FixedLocaleContextResolver(Locale.GERMANY);
		}

		@Override
		protected void configureViewResolvers(ViewResolverRegistry registry) {
			registry.viewResolver((viewName, locale) -> Mono.just(new DummyView(locale)));
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


	@Controller
	@SuppressWarnings("unused")
	static class TestController {

		@GetMapping("/")
		public String foo() {
			return "foo";
		}
	}

}
