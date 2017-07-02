/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.result.view;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurationSupport;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.result.method.annotation.AbstractRequestMappingIntegrationTests;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.i18n.FixedLocaleContextResolver;

import static org.junit.Assert.assertEquals;

/**
 * @author Sebastien Deleuze
 */
public class LocaleContextResolverIntegrationTests extends AbstractRequestMappingIntegrationTests {

	private final WebClient webClient = WebClient.create();

	@Test
	public void fixedLocale() {
		Mono<ClientResponse> result = webClient
				.get()
				.uri("http://localhost:" + this.port + "/")
				.exchange();

		StepVerifier
				.create(result)
				.consumeNextWith(response -> {
					assertEquals(HttpStatus.OK, response.statusCode());
					assertEquals(Locale.GERMANY, response.headers().asHttpHeaders().getContentLanguage());
				})
				.verifyComplete();
	}

	@Override
	protected ApplicationContext initApplicationContext() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(WebConfig.class);
		context.refresh();
		return context;
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
