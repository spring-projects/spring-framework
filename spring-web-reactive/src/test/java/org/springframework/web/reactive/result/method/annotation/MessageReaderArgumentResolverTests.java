/*
 * Copyright 2002-2016 the original author or authors.
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

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.subscriber.ScriptedSubscriber;
import rx.Observable;
import rx.Single;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.http.HttpMethod;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.ResolvableMethod;
import org.springframework.web.reactive.result.method.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.core.ResolvableType.forClass;
import static org.springframework.core.ResolvableType.forClassWithGenerics;

/**
 * Unit tests for {@link AbstractMessageReaderArgumentResolver}.
 * @author Rossen Stoyanchev
 */
public class MessageReaderArgumentResolverTests {

	private AbstractMessageReaderArgumentResolver resolver = resolver(new Jackson2JsonDecoder());

	private ServerWebExchange exchange;

	private MockServerHttpRequest request;

	private BindingContext bindingContext;

	private ResolvableMethod testMethod = ResolvableMethod.onClass(this.getClass()).name("handle");


	@Before
	public void setUp() throws Exception {
		this.request = new MockServerHttpRequest(HttpMethod.POST, "/path");
		MockServerHttpResponse response = new MockServerHttpResponse();
		this.exchange = new DefaultServerWebExchange(this.request, response, new MockWebSessionManager());

		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setValidator(new TestBeanValidator());
		this.bindingContext = new BindingContext(initializer);
	}


	@Test
	public void missingContentType() throws Exception {
		this.request.setBody("{\"bar\":\"BARBAR\",\"foo\":\"FOOFOO\"}");
		ResolvableType type = forClassWithGenerics(Mono.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		Mono<Object> result = this.resolver.readBody(param, true, this.bindingContext, this.exchange);

		ScriptedSubscriber.create().expectError(UnsupportedMediaTypeStatusException.class).verify(result);
	}

	// More extensive "empty body" tests in RequestBody- and HttpEntityArgumentResolverTests

	@Test @SuppressWarnings("unchecked") // SPR-9942
	public void emptyBody() throws Exception {
		this.request.setHeader("Content-Type", "application/json");
		ResolvableType type = forClassWithGenerics(Mono.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		Mono<TestBean> result = (Mono<TestBean>) this.resolver.readBody(
				param, true, this.bindingContext, this.exchange).block();

		ScriptedSubscriber.create().expectError(ServerWebInputException.class).verify(result);
	}

	@Test
	public void monoTestBean() throws Exception {
		String body = "{\"bar\":\"BARBAR\",\"foo\":\"FOOFOO\"}";
		ResolvableType type = forClassWithGenerics(Mono.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		Mono<Object> mono = resolveValue(param, body);

		assertEquals(new TestBean("FOOFOO", "BARBAR"), mono.block());
	}

	@Test
	public void fluxTestBean() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]";
		ResolvableType type = forClassWithGenerics(Flux.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		Flux<TestBean> flux = resolveValue(param, body);

		assertEquals(Arrays.asList(new TestBean("f1", "b1"), new TestBean("f2", "b2")),
				flux.collectList().block());
	}

	@Test
	public void singleTestBean() throws Exception {
		String body = "{\"bar\":\"b1\",\"foo\":\"f1\"}";
		ResolvableType type = forClassWithGenerics(Single.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		Single<TestBean> single = resolveValue(param, body);

		assertEquals(new TestBean("f1", "b1"), single.toBlocking().value());
	}

	@Test
	public void rxJava2SingleTestBean() throws Exception {
		String body = "{\"bar\":\"b1\",\"foo\":\"f1\"}";
		ResolvableType type = forClassWithGenerics(io.reactivex.Single.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		io.reactivex.Single<TestBean> single = resolveValue(param, body);

		assertEquals(new TestBean("f1", "b1"), single.blockingGet());
	}

	@Test
	public void rxJava2MaybeTestBean() throws Exception {
		String body = "{\"bar\":\"b1\",\"foo\":\"f1\"}";
		ResolvableType type = forClassWithGenerics(Maybe.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		Maybe<TestBean> maybe = resolveValue(param, body);

		assertEquals(new TestBean("f1", "b1"), maybe.blockingGet());
	}

	@Test
	public void observableTestBean() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]";
		ResolvableType type = forClassWithGenerics(Observable.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		Observable<?> observable = resolveValue(param, body);

		assertEquals(Arrays.asList(new TestBean("f1", "b1"), new TestBean("f2", "b2")),
				observable.toList().toBlocking().first());
	}

	@Test
	public void rxJava2ObservableTestBean() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]";
		ResolvableType type = forClassWithGenerics(io.reactivex.Observable.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		io.reactivex.Observable<?> observable = resolveValue(param, body);

		assertEquals(Arrays.asList(new TestBean("f1", "b1"), new TestBean("f2", "b2")),
				observable.toList().blockingGet());
	}

	@Test
	public void flowableTestBean() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]";
		ResolvableType type = forClassWithGenerics(Flowable.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		Flowable<?> flowable = resolveValue(param, body);

		assertEquals(Arrays.asList(new TestBean("f1", "b1"), new TestBean("f2", "b2")),
				flowable.toList().blockingGet());
	}

	@Test
	public void futureTestBean() throws Exception {
		String body = "{\"bar\":\"b1\",\"foo\":\"f1\"}";
		ResolvableType type = forClassWithGenerics(CompletableFuture.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		CompletableFuture<?> future = resolveValue(param, body);

		assertEquals(new TestBean("f1", "b1"), future.get());
	}

	@Test
	public void testBean() throws Exception {
		String body = "{\"bar\":\"b1\",\"foo\":\"f1\"}";
		MethodParameter param = this.testMethod.resolveParam(forClass(TestBean.class));
		TestBean value = resolveValue(param, body);

		assertEquals(new TestBean("f1", "b1"), value);
	}

	@Test
	public void map() throws Exception {
		String body = "{\"bar\":\"b1\",\"foo\":\"f1\"}";
		Map<String, String> map = new HashMap<>();
		map.put("foo", "f1");
		map.put("bar", "b1");
		ResolvableType type = forClassWithGenerics(Map.class, String.class, String.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		Map actual = resolveValue(param, body);

		assertEquals(map, actual);
	}

	@Test
	public void list() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]";
		ResolvableType type = forClassWithGenerics(List.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		List<?> list = resolveValue(param, body);

		assertEquals(Arrays.asList(new TestBean("f1", "b1"), new TestBean("f2", "b2")), list);
	}

	@Test
	public void monoList() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]";
		ResolvableType type = forClassWithGenerics(Mono.class, forClassWithGenerics(List.class, TestBean.class));
		MethodParameter param = this.testMethod.resolveParam(type);
		Mono<?> mono = resolveValue(param, body);

		List<?> list = (List<?>) mono.block(Duration.ofSeconds(5));
		assertEquals(Arrays.asList(new TestBean("f1", "b1"), new TestBean("f2", "b2")), list);
	}

	@Test
	public void array() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]";
		ResolvableType type = forClass(TestBean[].class);
		MethodParameter param = this.testMethod.resolveParam(type);
		TestBean[] value = resolveValue(param, body);

		assertArrayEquals(new TestBean[] {new TestBean("f1", "b1"), new TestBean("f2", "b2")}, value);
	}

	@Test @SuppressWarnings("unchecked")
	public void validateMonoTestBean() throws Exception {
		String body = "{\"bar\":\"b1\"}";
		ResolvableType type = forClassWithGenerics(Mono.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		Mono<TestBean> mono = resolveValue(param, body);

		ScriptedSubscriber.create().expectNextCount(0).expectError(ServerWebInputException.class).verify(mono);
	}

	@Test @SuppressWarnings("unchecked")
	public void validateFluxTestBean() throws Exception {
		String body = "[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\"}]";
		ResolvableType type = forClassWithGenerics(Flux.class, TestBean.class);
		MethodParameter param = this.testMethod.resolveParam(type);
		Flux<TestBean> flux = resolveValue(param, body);

		ScriptedSubscriber.<TestBean>create()
				.expectNext(new TestBean("f1", "b1"))
				.expectError(ServerWebInputException.class)
				.verify(flux);
	}

	@Test // SPR-9964
	public void parameterizedMethodArgument() throws Exception {
		Method method = AbstractParameterizedController.class.getMethod("handleDto", Identifiable.class);
		HandlerMethod handlerMethod = new HandlerMethod(new ConcreteParameterizedController(), method);
		MethodParameter methodParam = handlerMethod.getMethodParameters()[0];
		SimpleBean simpleBean = resolveValue(methodParam, "{\"name\" : \"Jad\"}");

		assertEquals("Jad", simpleBean.getName());
	}


	@SuppressWarnings("unchecked")
	private <T> T resolveValue(MethodParameter param, String body) {
		this.request.setHeader("Content-Type", "application/json").setBody(body);
		Mono<Object> result = this.resolver.readBody(param, true, this.bindingContext, this.exchange);
		Object value = result.block(Duration.ofSeconds(5));

		assertNotNull(value);
		assertTrue("Unexpected return value type: " + value,
				param.getParameterType().isAssignableFrom(value.getClass()));

		return (T) value;
	}

	@SuppressWarnings("Convert2MethodRef")
	private AbstractMessageReaderArgumentResolver resolver(Decoder<?>... decoders) {
		List<HttpMessageReader<?>> readers = new ArrayList<>();
		Arrays.asList(decoders).forEach(decoder -> readers.add(new DecoderHttpMessageReader<>(decoder)));
		return new AbstractMessageReaderArgumentResolver(readers) {};
	}


	@SuppressWarnings("unused")
	private void handle(
			@Validated Mono<TestBean> monoTestBean,
			@Validated Flux<TestBean> fluxTestBean,
			Single<TestBean> singleTestBean,
			io.reactivex.Single<TestBean> rxJava2SingleTestBean,
			Maybe<TestBean> rxJava2MaybeTestBean,
			Observable<TestBean> observableTestBean,
			io.reactivex.Observable<TestBean> rxJava2ObservableTestBean,
			Flowable<TestBean> flowableTestBean,
			CompletableFuture<TestBean> futureTestBean,
			TestBean testBean,
			Map<String, String> map,
			List<TestBean> list,
			Mono<List<TestBean>> monoList,
			Set<TestBean> set,
			TestBean[] array) {}


	@XmlRootElement
	private static class TestBean {

		private String foo;

		private String bar;

		@SuppressWarnings("unused")
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
		public void validate(Object target, Errors errors) {
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

	@SuppressWarnings({"serial"})
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
