/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.reactive.result.view;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.accept.HeaderContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.view.script.ScriptTemplateConfigurer;
import org.springframework.web.reactive.result.view.script.ScriptTemplateViewResolver;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.web.testfixture.method.ResolvableMethod.on;

/**
 * Tests for {@link Fragment} rendering through {@link ViewResolutionResultHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class FragmentViewResolutionResultHandlerTests {

	static Stream<Arguments> arguments() {
		Fragment f1 = Fragment.create("fragment1", Map.of("foo", "Foo"));
		Fragment f2 = Fragment.create("fragment2", Map.of("bar", "Bar"));
		return Stream.of(
				Arguments.of(
						FragmentsRendering.withPublisher(Flux.just(f1, f2).subscribeOn(Schedulers.boundedElastic()))
								.headers(headers -> headers.setContentType(MediaType.TEXT_HTML))
								.build(),
						on(Handler.class).resolveReturnType(FragmentsRendering.class)),
				Arguments.of(
						FragmentsRendering.withCollection(List.of(f1, f2))
								.headers(headers -> headers.setContentType(MediaType.TEXT_HTML))
								.build(),
						on(Handler.class).resolveReturnType(FragmentsRendering.class)),
				Arguments.of(
						Flux.just(f1, f2).subscribeOn(Schedulers.boundedElastic()),
						on(Handler.class).resolveReturnType(Flux.class, Fragment.class)),
				Arguments.of(
						List.of(f1, f2),
						on(Handler.class).resolveReturnType(List.class, Fragment.class)));
	}


	@ParameterizedTest
	@MethodSource("arguments")
	void render(Object returnValue, MethodParameter parameter) {
		Locale locale = Locale.ENGLISH;
		MockServerHttpRequest request = MockServerHttpRequest.get("/").acceptLanguageAsLocales(locale).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		HandlerResult result = new HandlerResult(new Handler(), returnValue, parameter, new BindingContext());

		String body = initHandler().handleResult(exchange, result)
				.then(Mono.defer(() -> exchange.getResponse().getBodyAsString()))
				.block(Duration.ofSeconds(60));

		assertThat(body).isEqualTo("<p>Hello Foo</p><p>Hello Bar</p>");
	}

	private ViewResolutionResultHandler initHandler() {

		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(ScriptTemplatingConfiguration.class);

		String prefix = "org/springframework/web/reactive/result/view/script/kotlin/";
		ScriptTemplateViewResolver viewResolver = new ScriptTemplateViewResolver(prefix, ".kts");
		viewResolver.setApplicationContext(context);

		RequestedContentTypeResolver contentTypeResolver = new HeaderContentTypeResolver();
		return new ViewResolutionResultHandler(List.of(viewResolver), contentTypeResolver);
	}


	@SuppressWarnings("unused")
	private static class Handler {

		FragmentsRendering rendering() { return null; }

		Flux<Fragment> fragmentFlux() { return null; }

		List<Fragment> fragmentList() { return null; }

	}


	@Configuration
	static class ScriptTemplatingConfiguration {

		@Bean
		public ScriptTemplateConfigurer kotlinScriptConfigurer() {
			ScriptTemplateConfigurer configurer = new ScriptTemplateConfigurer();
			configurer.setEngineName("kotlin");
			configurer.setScripts("org/springframework/web/reactive/result/view/script/kotlin/render.kts");
			configurer.setRenderFunction("render");
			return configurer;
		}

		@Bean
		public ResourceBundleMessageSource messageSource() {
			ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
			messageSource.setBasename("org/springframework/web/reactive/result/view/script/messages");
			return messageSource;
		}
	}

}
