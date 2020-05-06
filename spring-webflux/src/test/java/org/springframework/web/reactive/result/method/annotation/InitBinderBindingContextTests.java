/*
 * Copyright 2002-2019 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.SyncInvocableHandlerMethod;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link InitBinderBindingContext}.
 *
 * @author Rossen Stoyanchev
 */
public class InitBinderBindingContextTests {

	private final ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();

	private final List<SyncHandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>();


	@Test
	public void createBinder() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		BindingContext context = createBindingContext("initBinder", WebDataBinder.class);
		WebDataBinder dataBinder = context.createDataBinder(exchange, null, null);

		assertThat(dataBinder.getDisallowedFields()).isNotNull();
		assertThat(dataBinder.getDisallowedFields()[0]).isEqualTo("id");
	}

	@Test
	public void createBinderWithGlobalInitialization() throws Exception {
		ConversionService conversionService = new DefaultFormattingConversionService();
		bindingInitializer.setConversionService(conversionService);

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		BindingContext context = createBindingContext("initBinder", WebDataBinder.class);
		WebDataBinder dataBinder = context.createDataBinder(exchange, null, null);

		assertThat(dataBinder.getConversionService()).isSameAs(conversionService);
	}

	@Test
	public void createBinderWithAttrName() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		BindingContext context = createBindingContext("initBinderWithAttributeName", WebDataBinder.class);
		WebDataBinder dataBinder = context.createDataBinder(exchange, null, "foo");

		assertThat(dataBinder.getDisallowedFields()).isNotNull();
		assertThat(dataBinder.getDisallowedFields()[0]).isEqualTo("id");
	}

	@Test
	public void createBinderWithAttrNameNoMatch() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		BindingContext context = createBindingContext("initBinderWithAttributeName", WebDataBinder.class);
		WebDataBinder dataBinder = context.createDataBinder(exchange, null, "invalidName");

		assertThat(dataBinder.getDisallowedFields()).isNull();
	}

	@Test
	public void createBinderNullAttrName() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		BindingContext context = createBindingContext("initBinderWithAttributeName", WebDataBinder.class);
		WebDataBinder dataBinder = context.createDataBinder(exchange, null, null);

		assertThat(dataBinder.getDisallowedFields()).isNull();
	}

	@Test
	public void returnValueNotExpected() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		BindingContext context = createBindingContext("initBinderReturnValue", WebDataBinder.class);
		assertThatIllegalStateException().isThrownBy(() ->
				context.createDataBinder(exchange, null, "invalidName"));
	}

	@Test
	public void createBinderTypeConversion() throws Exception {
		MockServerHttpRequest request = MockServerHttpRequest.get("/path?requestParam=22").build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		ReactiveAdapterRegistry adapterRegistry = ReactiveAdapterRegistry.getSharedInstance();
		this.argumentResolvers.add(new RequestParamMethodArgumentResolver(null, adapterRegistry, false));

		BindingContext context = createBindingContext("initBinderTypeConversion", WebDataBinder.class, int.class);
		WebDataBinder dataBinder = context.createDataBinder(exchange, null, "foo");

		assertThat(dataBinder.getDisallowedFields()).isNotNull();
		assertThat(dataBinder.getDisallowedFields()[0]).isEqualTo("requestParam-22");
	}


	private BindingContext createBindingContext(String methodName, Class<?>... parameterTypes) throws Exception {
		Object handler = new InitBinderHandler();
		Method method = handler.getClass().getMethod(methodName, parameterTypes);

		SyncInvocableHandlerMethod handlerMethod = new SyncInvocableHandlerMethod(handler, method);
		handlerMethod.setArgumentResolvers(new ArrayList<>(this.argumentResolvers));
		handlerMethod.setParameterNameDiscoverer(new LocalVariableTableParameterNameDiscoverer());

		return new InitBinderBindingContext(this.bindingInitializer, Collections.singletonList(handlerMethod));
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

}
