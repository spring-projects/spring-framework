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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.reactive.result.method.BindingContext;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.SyncInvocableHandlerMethod;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.DefaultWebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;


/**
 * Unit tests for {@link InitBinderBindingContext}.
 * @author Rossen Stoyanchev
 */
public class InitBinderBindingContextTests {

	private final ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();

	private final List<SyncHandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>();

	private final ServerWebExchange exchange = new DefaultServerWebExchange(
			new MockServerHttpRequest(), new MockServerHttpResponse(), new DefaultWebSessionManager());


	@Test
	public void createBinder() throws Exception {
		BindingContext context = createBindingContext("initBinder", WebDataBinder.class);
		WebDataBinder dataBinder = context.createDataBinder(this.exchange, null, null);

		assertNotNull(dataBinder.getDisallowedFields());
		assertEquals("id", dataBinder.getDisallowedFields()[0]);
	}

	@Test
	public void createBinderWithGlobalInitialization() throws Exception {
		ConversionService conversionService = new DefaultFormattingConversionService();
		bindingInitializer.setConversionService(conversionService);

		BindingContext context = createBindingContext("initBinder", WebDataBinder.class);
		WebDataBinder dataBinder = context.createDataBinder(this.exchange, null, null);

		assertSame(conversionService, dataBinder.getConversionService());
	}

	@Test
	public void createBinderWithAttrName() throws Exception {
		BindingContext context = createBindingContext("initBinderWithAttributeName", WebDataBinder.class);
		WebDataBinder dataBinder = context.createDataBinder(this.exchange, null, "foo");

		assertNotNull(dataBinder.getDisallowedFields());
		assertEquals("id", dataBinder.getDisallowedFields()[0]);
	}

	@Test
	public void createBinderWithAttrNameNoMatch() throws Exception {
		BindingContext context = createBindingContext("initBinderWithAttributeName", WebDataBinder.class);
		WebDataBinder dataBinder = context.createDataBinder(this.exchange, null, "invalidName");

		assertNull(dataBinder.getDisallowedFields());
	}

	@Test
	public void createBinderNullAttrName() throws Exception {
		BindingContext context = createBindingContext("initBinderWithAttributeName", WebDataBinder.class);
		WebDataBinder dataBinder = context.createDataBinder(this.exchange, null, null);

		assertNull(dataBinder.getDisallowedFields());
	}

	@Test(expected = IllegalStateException.class)
	public void returnValueNotExpected() throws Exception {
		BindingContext context = createBindingContext("initBinderReturnValue", WebDataBinder.class);
		context.createDataBinder(this.exchange, null, "invalidName");
	}

	@Test
	public void createBinderTypeConversion() throws Exception {
		this.exchange.getRequest().getQueryParams().add("requestParam", "22");
		this.argumentResolvers.add(new RequestParamMethodArgumentResolver(null, false));

		BindingContext context = createBindingContext("initBinderTypeConversion", WebDataBinder.class, int.class);
		WebDataBinder dataBinder = context.createDataBinder(this.exchange, null, "foo");

		assertNotNull(dataBinder.getDisallowedFields());
		assertEquals("requestParam-22", dataBinder.getDisallowedFields()[0]);
	}


	private BindingContext createBindingContext(String methodName, Class<?>... parameterTypes)
			throws Exception {

		Object handler = new InitBinderHandler();
		Method method = handler.getClass().getMethod(methodName, parameterTypes);

		SyncInvocableHandlerMethod handlerMethod = new SyncInvocableHandlerMethod(handler, method);
		handlerMethod.setArgumentResolvers(new ArrayList<>(this.argumentResolvers));
		handlerMethod.setParameterNameDiscoverer(new LocalVariableTableParameterNameDiscoverer());

		return new InitBinderBindingContext(
				this.bindingInitializer, Collections.singletonList(handlerMethod));
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
