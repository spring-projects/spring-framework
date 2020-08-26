/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.xml.bind.annotation.XmlRootElement;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.http.MediaType;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.lang.Nullable;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.ResolvableType.forClassWithGenerics;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.post;

/**
 * Unit tests for {@link AbstractMessageReaderArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class MessageReaderArgumentResolverTests {

	private AbstractMessageReaderArgumentResolver resolver = resolver(new Jackson2JsonDecoder());

	private BindingContext bindingContext;

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@BeforeEach
	public void setup() throws Exception {
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setValidator(new TestBeanValidator());
		this.bindingContext = new BindingContext(initializer);
	}


	@SuppressWarnings("unchecked")
	@Test
	public void missingContentType() throws Exception {
		MockServerHttpRequest request = post("/path").body("{\"bar\":\"BARBAR\",\"foo\":\"FOOFOO\"}");
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		ResolvableType type = forClassWithGenerics(Mono.class, TestBean.class);
		MethodParameter param = this.testMethod.arg(type);
		Mono<Object> result = this.resolver.readBody(param, true, this.bindingContext, exchange);
		Mono<TestBean> value = (Mono<TestBean>) result.block(Duration.ofSeconds(1));

		StepVerifier.create(value).expectError(UnsupportedMediaTypeStatusException.class).verify();
	}

	// More extensive "empty body" tests in RequestBody- and HttpEntityArgumentResolverTests

	@Test @SuppressWarnings("unchecked") // SPR-9942
	public void emptyBody() throws Exception {
		MockServerHttpRequest request = post("/path").contentType(MediaType.APPLICATION_JSON).build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		ResolvableType type = forClassWithGenerics(Mono.class, TestBean.class);
		MethodParameter param = this.testMethod.arg(type);
		Mono<TestBean> result = (Mono<TestBean>) this.resolver.readBody(
				param, true, this.bindingContext, exchange).block();

		StepVerifier.create(result).expectError(ServerWebInputException.class).verify();
	}

	@Test
	public void monoTestBean() throws Exception {
		String body = "{\"bar\":\"BARBAR\",\"foo\":\"FOOFOO\"}";
		ResolvableType type = forClassWithGenerics(Mono.class, TestBean.class);
		MethodParameter param = this.testMethod.arg(type);
		Mono<Object> mono = resolveValue(param, body);

		assertThat(mono.block()).isEqualTo(new TestBean("FOOFOO", "BARBAR"));
	}

	@Test
	public void fluxTestBean() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]";
		ResolvableType type = forClassWithGenerics(Flux.class, TestBean.class);
		MethodParameter param = this.testMethod.arg(type);
		Flux<TestBean> flux = resolveValue(param, body);

		assertThat(flux.collectList().block()).isEqualTo(Arrays.asList(new TestBean("f1", "b1"), new TestBean("f2", "b2")));
	}

	@Test
	public void singleTestBean() throws Exception {
		String body = "{\"bar\":\"b1\",\"foo\":\"f1\"}";
		ResolvableType type = forClassWithGenerics(Single.class, TestBean.class);
		MethodParameter param = this.testMethod.arg(type);
		Single<TestBean> single = resolveValue(param, body);

		assertThat(single.blockingGet()).isEqualTo(new TestBean("f1", "b1"));
	}

	@Test
	public void maybeTestBean() throws Exception {
		String body = "{\"bar\":\"b1\",\"foo\":\"f1\"}";
		ResolvableType type = forClassWithGenerics(Maybe.class, TestBean.class);
		MethodParameter param = this.testMethod.arg(type);
		Maybe<TestBean> maybe = resolveValue(param, body);

		assertThat(maybe.blockingGet()).isEqualTo(new TestBean("f1", "b1"));
	}

	@Test
	public void observableTestBean() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]";
		ResolvableType type = forClassWithGenerics(Observable.class, TestBean.class);
		MethodParameter param = this.testMethod.arg(type);
		Observable<?> observable = resolveValue(param, body);

		assertThat(observable.toList().blockingGet()).isEqualTo(Arrays.asList(new TestBean("f1", "b1"), new TestBean("f2", "b2")));
	}

	@Test
	public void flowableTestBean() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]";
		ResolvableType type = forClassWithGenerics(Flowable.class, TestBean.class);
		MethodParameter param = this.testMethod.arg(type);
		Flowable<?> flowable = resolveValue(param, body);

		assertThat(flowable.toList().blockingGet()).isEqualTo(Arrays.asList(new TestBean("f1", "b1"), new TestBean("f2", "b2")));
	}

	@Test
	public void futureTestBean() throws Exception {
		String body = "{\"bar\":\"b1\",\"foo\":\"f1\"}";
		ResolvableType type = forClassWithGenerics(CompletableFuture.class, TestBean.class);
		MethodParameter param = this.testMethod.arg(type);
		CompletableFuture<?> future = resolveValue(param, body);

		assertThat(future.get()).isEqualTo(new TestBean("f1", "b1"));
	}

	@Test
	public void testBean() throws Exception {
		String body = "{\"bar\":\"b1\",\"foo\":\"f1\"}";
		MethodParameter param = this.testMethod.arg(TestBean.class);
		TestBean value = resolveValue(param, body);

		assertThat(value).isEqualTo(new TestBean("f1", "b1"));
	}

	@Test
	public void map() throws Exception {
		String body = "{\"bar\":\"b1\",\"foo\":\"f1\"}";
		Map<String, String> map = new HashMap<>();
		map.put("foo", "f1");
		map.put("bar", "b1");
		ResolvableType type = forClassWithGenerics(Map.class, String.class, String.class);
		MethodParameter param = this.testMethod.arg(type);
		Map<String, String> actual = resolveValue(param, body);

		assertThat(actual).isEqualTo(map);
	}

	@Test
	public void list() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]";
		ResolvableType type = forClassWithGenerics(List.class, TestBean.class);
		MethodParameter param = this.testMethod.arg(type);
		List<?> list = resolveValue(param, body);

		assertThat(list).isEqualTo(Arrays.asList(new TestBean("f1", "b1"), new TestBean("f2", "b2")));
	}

	@Test
	public void monoList() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]";
		ResolvableType type = forClassWithGenerics(Mono.class, forClassWithGenerics(List.class, TestBean.class));
		MethodParameter param = this.testMethod.arg(type);
		Mono<?> mono = resolveValue(param, body);

		List<?> list = (List<?>) mono.block(Duration.ofSeconds(5));
		assertThat(list).isEqualTo(Arrays.asList(new TestBean("f1", "b1"), new TestBean("f2", "b2")));
	}

	@Test
	public void array() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]";
		MethodParameter param = this.testMethod.arg(TestBean[].class);
		TestBean[] value = resolveValue(param, body);

		assertThat(value).isEqualTo(new TestBean[] {new TestBean("f1", "b1"), new TestBean("f2", "b2")});
	}

	@Test
	public void validateMonoTestBean() throws Exception {
		String body = "{\"bar\":\"b1\"}";
		ResolvableType type = forClassWithGenerics(Mono.class, TestBean.class);
		MethodParameter param = this.testMethod.arg(type);
		Mono<TestBean> mono = resolveValue(param, body);

		StepVerifier.create(mono).expectNextCount(0).expectError(ServerWebInputException.class).verify();
	}

	@Test
	public void validateFluxTestBean() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\"}]";
		ResolvableType type = forClassWithGenerics(Flux.class, TestBean.class);
		MethodParameter param = this.testMethod.arg(type);
		Flux<TestBean> flux = resolveValue(param, body);

		StepVerifier.create(flux)
				.expectNext(new TestBean("f1", "b1"))
				.expectError(ServerWebInputException.class)
				.verify();
	}

	@Test  // SPR-9964
	public void parameterizedMethodArgument() throws Exception {
		Method method = AbstractParameterizedController.class.getMethod("handleDto", Identifiable.class);
		HandlerMethod handlerMethod = new HandlerMethod(new ConcreteParameterizedController(), method);
		MethodParameter methodParam = handlerMethod.getMethodParameters()[0];
		SimpleBean simpleBean = resolveValue(methodParam, "{\"name\" : \"Jad\"}");

		assertThat(simpleBean.getName()).isEqualTo("Jad");
	}


	@SuppressWarnings("unchecked")
	private <T> T resolveValue(MethodParameter param, String body) {
		MockServerHttpRequest request = post("/path").contentType(MediaType.APPLICATION_JSON).body(body);
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		Mono<Object> result = this.resolver.readBody(param, true, this.bindingContext, exchange);
		Object value = result.block(Duration.ofSeconds(5));

		assertThat(value).isNotNull();
		assertThat(param.getParameterType().isAssignableFrom(value.getClass())).as("Unexpected return value type: " + value).isTrue();

		return (T) value;
	}

	@SuppressWarnings("Convert2MethodRef")
	private AbstractMessageReaderArgumentResolver resolver(Decoder<?>... decoders) {
		List<HttpMessageReader<?>> readers = new ArrayList<>();
		Arrays.asList(decoders).forEach(decoder -> readers.add(new DecoderHttpMessageReader<>(decoder)));
		return new AbstractMessageReaderArgumentResolver(readers) {
			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return false;
			}
			@Override
			public Mono<Object> resolveArgument(MethodParameter p, BindingContext bc, ServerWebExchange e) {
				return null;
			}
		};
	}


	@SuppressWarnings("unused")
	private void handle(
			@Validated Mono<TestBean> monoTestBean,
			@Validated Flux<TestBean> fluxTestBean,
			Single<TestBean> singleTestBean,
			Maybe<TestBean> maybeTestBean,
			Observable<TestBean> observableTestBean,
			Flowable<TestBean> flowableTestBean,
			CompletableFuture<TestBean> futureTestBean,
			TestBean testBean,
			Map<String, String> map,
			List<TestBean> list,
			Mono<List<TestBean>> monoList,
			Set<TestBean> set,
			TestBean[] array) {
	}


	@XmlRootElement
	@SuppressWarnings("unused")
	private static class TestBean {

		private String foo;

		private String bar;

		public TestBean() {
		}

		TestBean(String foo, String bar) {
			this.foo = foo;
			this.bar = bar;
		}

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

		public String getBar() {
			return this.bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o instanceof TestBean) {
				TestBean other = (TestBean) o;
				return this.foo.equals(other.foo) && this.bar.equals(other.bar);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return 31 * foo.hashCode() + bar.hashCode();
		}

		@Override
		public String toString() {
			return "TestBean[foo='" + this.foo + "\'" + ", bar='" + this.bar + "\']";
		}
	}


	private static class TestBeanValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return clazz.equals(TestBean.class);
		}

		@Override
		public void validate(@Nullable Object target, Errors errors) {
			TestBean testBean = (TestBean) target;
			if (testBean.getFoo() == null) {
				errors.rejectValue("foo", "nullValue");
			}
		}
	}


	private static abstract class AbstractParameterizedController<DTO extends Identifiable> {

		@SuppressWarnings("unused")
		public void handleDto(DTO dto) {}
	}


	private static class ConcreteParameterizedController extends AbstractParameterizedController<SimpleBean> {
	}


	private interface Identifiable extends Serializable {

		Long getId();

		void setId(Long id);
	}


	@SuppressWarnings({"serial", "unused"})
	private static class SimpleBean implements Identifiable {

		private Long id;

		private String name;

		@Override
		public Long getId() {
			return id;
		}

		@Override
		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
