/*
 * Copyright 2002-present the original author or authors.
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.SyncInvocableHandlerMethod;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link InitBinderBindingContext}.
 *
 * @author Rossen Stoyanchev
 */
class InitBinderBindingContextTests {

	private final ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();

	private final List<SyncHandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>();


	@Test
	void createBinder() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		BindingContext context = createBindingContext("initBinder", WebDataBinder.class);
		WebDataBinder dataBinder = context.createDataBinder(exchange, null);

		assertThat(dataBinder.getDisallowedFields()).isNotNull();
		assertThat(dataBinder.getDisallowedFields()[0]).isEqualTo("id");
	}

	@Test
	void createBinderWithGlobalInitialization() throws Exception {
		ConversionService conversionService = new DefaultFormattingConversionService();
		bindingInitializer.setConversionService(conversionService);

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		BindingContext context = createBindingContext("initBinder", WebDataBinder.class);
		WebDataBinder dataBinder = context.createDataBinder(exchange, null);

		assertThat(dataBinder.getConversionService()).isSameAs(conversionService);
	}

	@Test
	void createBinderWithAttrName() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		BindingContext context = createBindingContext("initBinderWithAttributeName", WebDataBinder.class);
		WebDataBinder dataBinder = context.createDataBinder(exchange, "foo");

		assertThat(dataBinder.getDisallowedFields()).isNotNull();
		assertThat(dataBinder.getDisallowedFields()[0]).isEqualTo("id");
	}

	@Test
	void createBinderWithAttrNameNoMatch() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		BindingContext context = createBindingContext("initBinderWithAttributeName", WebDataBinder.class);
		WebDataBinder dataBinder = context.createDataBinder(exchange, "invalidName");

		assertThat(dataBinder.getDisallowedFields()).isNull();
	}

	@Test
	void createBinderNullAttrName() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		BindingContext context = createBindingContext("initBinderWithAttributeName", WebDataBinder.class);
		WebDataBinder dataBinder = context.createDataBinder(exchange, null);

		assertThat(dataBinder.getDisallowedFields()).isNull();
	}

	@Test
	void returnValueNotExpected() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		BindingContext context = createBindingContext("initBinderReturnValue", WebDataBinder.class);
		assertThatIllegalStateException().isThrownBy(() -> context.createDataBinder(exchange, "invalidName"));
	}

	@Test
	void createBinderTypeConversion() throws Exception {
		MockServerHttpRequest request = MockServerHttpRequest.get("/path?requestParam=22").build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		ReactiveAdapterRegistry adapterRegistry = ReactiveAdapterRegistry.getSharedInstance();
		this.argumentResolvers.add(new RequestParamMethodArgumentResolver(null, adapterRegistry, false));

		BindingContext context = createBindingContext("initBinderTypeConversion", WebDataBinder.class, int.class);
		WebDataBinder dataBinder = context.createDataBinder(exchange, "foo");

		assertThat(dataBinder.getDisallowedFields()).isNotNull();
		assertThat(dataBinder.getDisallowedFields()[0]).isEqualToIgnoringCase("requestParam-22");
	}

	@Test
	void bindUriVariablesAndHeadersViaSetters() throws Exception {

		MockServerHttpRequest request = MockServerHttpRequest.get("/path")
				.header("Some-Int-Array", "1")
				.header("Some-Int-Array", "2")
				.build();

		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		exchange.getAttributes().put(
				HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
				Map.of("name", "John", "age", "25"));

		TestBean target = new TestBean();

		BindingContext context = createBindingContext("initBinderWithAttributeName", WebDataBinder.class);
		WebExchangeDataBinder binder = context.createDataBinder(exchange, target, "testBean", null);

		binder.bind(exchange).block();

		assertThat(target.getName()).isEqualTo("John");
		assertThat(target.getAge()).isEqualTo(25);
		assertThat(target.getSomeIntArray()).containsExactly(1, 2);
	}

	@Test
	void bindUriVariablesAndHeadersViaConstructor() throws Exception {

		MockServerHttpRequest request = MockServerHttpRequest.get("/path")
				.header("Some-Int-Array", "1")
				.header("Some-Int-Array", "2")
				.build();

		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		exchange.getAttributes().put(
				HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
				Map.of("name", "John", "age", "25"));

		BindingContext context = createBindingContext("initBinderWithAttributeName", WebDataBinder.class);
		WebExchangeDataBinder binder = context.createDataBinder(exchange, null, "dataBean", null);
		binder.setTargetType(ResolvableType.forClass(DataBean.class));
		binder.construct(exchange).block();

		DataBean bean = (DataBean) binder.getTarget();

		assertThat(bean.name()).isEqualTo("John");
		assertThat(bean.age()).isEqualTo(25);
		assertThat(bean.someIntArray()).containsExactly(1, 2);
	}

	@Test
	void bindUriVarsAndHeadersAddedConditionally() throws Exception {

		MockServerHttpRequest request = MockServerHttpRequest.post("/path")
				.header("name", "Johnny")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body("name=John&age=25");

		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		exchange.getAttributes().put(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("age", "26"));

		TestBean target = new TestBean();

		BindingContext context = createBindingContext("initBinderWithAttributeName", WebDataBinder.class);
		WebExchangeDataBinder binder = context.createDataBinder(exchange, target, "testBean", null);

		binder.bind(exchange).block();

		assertThat(target.getName()).isEqualTo("John");
		assertThat(target.getAge()).isEqualTo(25);
	}

	@Test
	void headerPredicate() throws Exception {
		MockServerHttpRequest request = MockServerHttpRequest.get("/path")
				.header("Priority", "u1")
				.header("Some-Int-Array", "1")
				.header("Another-Int-Array", "1")
				.build();

		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		BindingContext context = createBindingContext("initBinderWithAttributeName", WebDataBinder.class);
		ExtendedWebExchangeDataBinder binder = (ExtendedWebExchangeDataBinder) context.createDataBinder(exchange, null, "", null);
		binder.addHeaderPredicate(name -> !name.equalsIgnoreCase("Another-Int-Array"));

		Map<String, Object> map = binder.getValuesToBind(exchange).block();
		assertThat(map).containsExactlyInAnyOrderEntriesOf(Map.of("someIntArray", "1", "Some-Int-Array", "1"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"Accept", "Authorization", "Connection",
			"Cookie", "From", "Host", "Origin", "Priority", "Range", "Referer", "Upgrade", "priority"})
	void filteredHeaders(String headerName) throws Exception {
		MockServerHttpRequest request = MockServerHttpRequest.get("/path")
				.header(headerName, "u1")
				.build();

		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		BindingContext context = createBindingContext("initBinderWithAttributeName", WebDataBinder.class);
		ExtendedWebExchangeDataBinder binder = (ExtendedWebExchangeDataBinder) context.createDataBinder(exchange, null, "", null);

		Map<String, Object> map = binder.getValuesToBind(exchange).block();
		assertThat(map).isEmpty();
	}

	private BindingContext createBindingContext(String methodName, Class<?>... parameterTypes) throws Exception {
		Object handler = new InitBinderHandler();
		Method method = handler.getClass().getMethod(methodName, parameterTypes);

		SyncInvocableHandlerMethod handlerMethod = new SyncInvocableHandlerMethod(handler, method);
		handlerMethod.setArgumentResolvers(new ArrayList<>(this.argumentResolvers));
		handlerMethod.setParameterNameDiscoverer(new DefaultParameterNameDiscoverer());

		return new InitBinderBindingContext(
				this.bindingInitializer, Collections.singletonList(handlerMethod), false,
				ReactiveAdapterRegistry.getSharedInstance());
	}


	private static class InitBinderHandler {

		@InitBinder
		public void initBinder(WebDataBinder dataBinder) {
			dataBinder.setDisallowedFields("id");
		}

		@InitBinder(value="foo")
		public void initBinderWithAttributeName(WebDataBinder dataBinder) {
			dataBinder.setDisallowedFields("id");
		}

		@InitBinder
		public String initBinderReturnValue(WebDataBinder dataBinder) {
			return "invalid";
		}

		@InitBinder
		public void initBinderTypeConversion(WebDataBinder dataBinder, @RequestParam int requestParam) {
			dataBinder.setDisallowedFields("requestParam-" + requestParam);
		}
	}


	private record DataBean(String name, int age, @BindParam("Some-Int-Array") Integer[] someIntArray) {
	}

}
