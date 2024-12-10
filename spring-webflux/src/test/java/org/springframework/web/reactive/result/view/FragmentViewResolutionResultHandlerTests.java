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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.accept.HeaderContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.view.script.ScriptTemplateConfigurer;
import org.springframework.web.reactive.result.view.script.ScriptTemplateViewResolver;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.web.testfixture.method.ResolvableMethod.on;

/**
 * Tests for {@link Fragment} rendering through {@link ViewResolutionResultHandler}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class FragmentViewResolutionResultHandlerTests {

	private static final Fragment fragment1 = Fragment.create("fragment1", Map.of("foo", "Foo"));

	private static final Fragment fragment2 = Fragment.create("fragment2", Map.of("bar", "Bar"));


	static Stream<Arguments> arguments() {
		Flux<Fragment> fragmentFlux = Flux.just(fragment1, fragment2).subscribeOn(Schedulers.boundedElastic());
		return Stream.of(
				Arguments.of(FragmentsRendering.fragmentsPublisher(fragmentFlux).build(),
						on(Handler.class).resolveReturnType(FragmentsRendering.class)),
				Arguments.of(FragmentsRendering.fragments(List.of(fragment1, fragment2)).build(),
						on(Handler.class).resolveReturnType(FragmentsRendering.class)),
				Arguments.of(fragmentFlux,
						on(Handler.class).resolveReturnType(Flux.class, Fragment.class)),
				Arguments.of(List.of(fragment1, fragment2),
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

		assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.TEXT_HTML);
		assertThat(body).isEqualTo("""
				<p>
					Hello Foo
				</p>\
				<p>
					Hello Bar
				</p>""");
	}

	@Test
	void renderFragmentStream() {

		testSse(Flux.just(fragment1, fragment2),
				on(Handler.class).resolveReturnType(Flux.class, Fragment.class),
				"""
				event:fragment1
				data:<p>
				data:	Hello Foo
				data:</p>

				event:fragment2
				data:<p>
				data:	Hello Bar
				data:</p>

				""");
	}

	@Test
	void renderServerSentEventFragmentStream() {

		ServerSentEvent<Fragment> event1 = ServerSentEvent.builder(fragment1).id("id1").event("event1").build();
		ServerSentEvent<Fragment> event2 = ServerSentEvent.builder(fragment2).id("id2").event("event2").build();

		MethodParameter returnType = on(Handler.class).resolveReturnType(
				Flux.class, ResolvableType.forClassWithGenerics(ServerSentEvent.class, Fragment.class));

		testSse(Flux.just(event1, event2), returnType,
				"""
				id:id1
				event:event1
				data:<p>
				data:	Hello Foo
				data:</p>

				id:id2
				event:event2
				data:<p>
				data:	Hello Bar
				data:</p>

				""");
	}

	private void testSse(Flux<?> dataFlux, MethodParameter returnType, String output) {
		MockServerHttpRequest request = MockServerHttpRequest.get("/")
				.accept(MediaType.TEXT_EVENT_STREAM)
				.acceptLanguageAsLocales(Locale.ENGLISH)
				.build();

		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		MockServerHttpResponse response = exchange.getResponse();

		HandlerResult result = new HandlerResult(
				new Handler(),
				dataFlux.subscribeOn(Schedulers.boundedElastic()),
				returnType,
				new BindingContext());

		String body = initHandler().handleResult(exchange, result)
				.then(Mono.defer(response::getBodyAsString))
				.block(Duration.ofSeconds(60));

		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_EVENT_STREAM);
		assertThat(body).isEqualTo(output);
	}

	private ViewResolutionResultHandler initHandler() {

		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(ScriptTemplatingConfiguration.class);

		String prefix = "org/springframework/web/reactive/result/view/script/jython/";
		ScriptTemplateViewResolver viewResolver = new ScriptTemplateViewResolver(prefix, ".html");
		viewResolver.setApplicationContext(context);
		viewResolver.setSupportedMediaTypes(List.of(MediaType.TEXT_HTML, MediaType.TEXT_EVENT_STREAM));

		RequestedContentTypeResolver contentTypeResolver = new HeaderContentTypeResolver();
		return new ViewResolutionResultHandler(List.of(viewResolver), contentTypeResolver);
	}


	@SuppressWarnings({"unused", "DataFlowIssue"})
	private static class Handler {

		FragmentsRendering render() { return null; }

		Flux<Fragment> renderFlux() { return null; }

		Flux<ServerSentEvent<Fragment>> renderSseFlux() { return null; }

		List<Fragment> renderList() { return null; }

	}


	@Configuration
	static class ScriptTemplatingConfiguration {

		@Bean
		public ScriptTemplateConfigurer kotlinScriptConfigurer() {
			ScriptTemplateConfigurer configurer = new ScriptTemplateConfigurer();
			configurer.setEngineName("jython");
			configurer.setScripts("org/springframework/web/reactive/result/view/script/jython/render.py");
			configurer.setRenderFunction("render");
			return configurer;
		}
	}

}
