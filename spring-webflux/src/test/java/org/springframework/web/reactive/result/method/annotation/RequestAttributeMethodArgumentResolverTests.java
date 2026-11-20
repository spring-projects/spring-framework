/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.method.annotation;

import java.time.Duration;
import java.util.Optional;

import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.testfixture.method.MvcAnnotationPredicates.requestAttribute;

/**
 * Tests for {@link RequestAttributeMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
class RequestAttributeMethodArgumentResolverTests {

	private RequestAttributeMethodArgumentResolver resolver;

	private final MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

	private final ResolvableMethod testMethod = ResolvableMethod.on(getClass())
			.named("handleWithRequestAttribute").build();


	@BeforeEach
	void setup() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.refresh();
		ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();
		this.resolver = new RequestAttributeMethodArgumentResolver(context.getBeanFactory(), registry);
	}


	@Test
	void supportsParameter() {
		assertThat(this.resolver.supportsParameter(
				this.testMethod.annot(requestAttribute().noName()).arg(Foo.class))).isTrue();

		// SPR-16158
		assertThat(this.resolver.supportsParameter(
				this.testMethod.annotPresent(RequestAttribute.class).arg(Mono.class, Foo.class))).isTrue();

		assertThat(this.resolver.supportsParameter(
				this.testMethod.annotNotPresent(RequestAttribute.class).arg())).isFalse();
	}

	@Test
	void resolve() {
		MethodParameter param = this.testMethod.annot(requestAttribute().noName()).arg(Foo.class);
		Mono<Object> mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		StepVerifier.create(mono)
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();

		Foo foo = new Foo();
		this.exchange.getAttributes().put("foo", foo);
		mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertThat(mono.block()).isSameAs(foo);
	}

	@Test
	void resolveWithName() {
		MethodParameter param = this.testMethod.annot(requestAttribute().name("specialFoo")).arg();
		Foo foo = new Foo();
		this.exchange.getAttributes().put("specialFoo", foo);
		Mono<Object> mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertThat(mono.block()).isSameAs(foo);
	}

	@Test
	void resolveNotRequired() {
		MethodParameter param = this.testMethod.annot(requestAttribute().name("foo").notRequired()).arg();
		Mono<Object> mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertThat(mono.block()).isNull();

		Foo foo = new Foo();
		this.exchange.getAttributes().put("foo", foo);
		mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertThat(mono.block()).isSameAs(foo);
	}

	@Test
	void resolveOptional() {
		MethodParameter param = this.testMethod.annot(requestAttribute().name("foo")).arg(Optional.class, Foo.class);
		Mono<Object> mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);

		assertThat(mono.block()).isNotNull();
		assertThat(mono.block().getClass()).isEqualTo(Optional.class);
		assertThat((Optional<?>) mono.block()).isNotPresent();

		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultFormattingConversionService());
		BindingContext bindingContext = new BindingContext(initializer);

		Foo foo = new Foo();
		this.exchange.getAttributes().put("foo", foo);
		mono = this.resolver.resolveArgument(param, bindingContext, this.exchange);

		assertThat(mono.block()).isNotNull();
		assertThat(mono.block().getClass()).isEqualTo(Optional.class);
		Optional<?> optional = (Optional<?>) mono.block();
		assertThat(optional).isPresent();
		assertThat(optional.get()).isSameAs(foo);
	}

	@Test  // SPR-16158
	void resolveMonoParameter() {
		MethodParameter param = this.testMethod.annot(requestAttribute().noName()).arg(Mono.class, Foo.class);

		// Mono attribute
		Foo foo = new Foo();
		Mono<Foo> fooMono = Mono.just(foo);
		this.exchange.getAttributes().put("fooMono", fooMono);
		Mono<Object> mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertThat(mono.block(Duration.ZERO)).isSameAs(fooMono);

		// RxJava Single attribute
		Single<Foo> singleMono = Single.just(foo);
		this.exchange.getAttributes().clear();
		this.exchange.getAttributes().put("fooMono", singleMono);
		mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		Object value = mono.block(Duration.ZERO);
		assertThat(value).isInstanceOf(Mono.class);
		assertThat(((Mono<?>) value).block(Duration.ZERO)).isSameAs(foo);

		// No attribute --> Mono.empty
		this.exchange.getAttributes().clear();
		mono = this.resolver.resolveArgument(param, new BindingContext(), this.exchange);
		assertThat(mono.block(Duration.ZERO)).isSameAs(Mono.empty());
	}


	@SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
	private void handleWithRequestAttribute(
			@RequestAttribute Foo foo,
			@RequestAttribute("specialFoo") Foo namedFoo,
			@RequestAttribute(name="foo", required = false) Foo notRequiredFoo,
			@RequestAttribute(name="foo") Optional<Foo> optionalFoo,
			@RequestAttribute Mono<Foo> fooMono,
			String notSupported) {
	}


	private static class Foo {
	}

}
