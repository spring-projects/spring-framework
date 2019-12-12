/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.codec.ByteArrayDecoder;
import org.springframework.core.codec.ByteBufferDecoder;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.ResolvableMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.InvocableHandlerMethod;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.SyncInvocableHandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ControllerMethodResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class ControllerMethodResolverTests {

	private ControllerMethodResolver methodResolver;

	private HandlerMethod handlerMethod;


	@Before
	public void setup() {
		ArgumentResolverConfigurer resolvers = new ArgumentResolverConfigurer();
		resolvers.addCustomResolver(new CustomArgumentResolver());
		resolvers.addCustomResolver(new CustomSyncArgumentResolver());

		ServerCodecConfigurer codecs = ServerCodecConfigurer.create();
		codecs.customCodecs().register(new ByteArrayDecoder());
		codecs.customCodecs().register(new ByteBufferDecoder());

		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.registerBean(TestControllerAdvice.class);
		applicationContext.refresh();

		this.methodResolver = new ControllerMethodResolver(
				resolvers, ReactiveAdapterRegistry.getSharedInstance(), applicationContext, codecs.getReaders());

		Method method = ResolvableMethod.on(TestController.class).mockCall(TestController::handle).method();
		this.handlerMethod = new HandlerMethod(new TestController(), method);
	}


	@Test
	public void requestMappingArgumentResolvers() {
		InvocableHandlerMethod invocable = this.methodResolver.getRequestMappingMethod(this.handlerMethod);
		List<HandlerMethodArgumentResolver> resolvers = invocable.getResolvers();

		AtomicInteger index = new AtomicInteger(-1);
		assertEquals(RequestParamMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(RequestParamMapMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(PathVariableMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(PathVariableMapMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(MatrixVariableMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(MatrixVariableMapMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(RequestBodyArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(RequestPartMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(ModelAttributeMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(RequestHeaderMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(RequestHeaderMapMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(CookieValueMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(ExpressionValueMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(SessionAttributeMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(RequestAttributeMethodArgumentResolver.class, next(resolvers, index).getClass());

		assertEquals(HttpEntityArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(ModelArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(ErrorsMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(ServerWebExchangeArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(PrincipalArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(SessionStatusMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(WebSessionArgumentResolver.class, next(resolvers, index).getClass());

		assertEquals(CustomArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(CustomSyncArgumentResolver.class, next(resolvers, index).getClass());

		assertEquals(RequestParamMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(ModelAttributeMethodArgumentResolver.class, next(resolvers, index).getClass());
	}

	@Test
	public void modelAttributeArgumentResolvers() {
		List<InvocableHandlerMethod> methods = this.methodResolver.getModelAttributeMethods(this.handlerMethod);

		assertEquals("Expected one each from Controller + ControllerAdvice", 2, methods.size());
		InvocableHandlerMethod invocable = methods.get(0);
		List<HandlerMethodArgumentResolver> resolvers = invocable.getResolvers();

		AtomicInteger index = new AtomicInteger(-1);
		assertEquals(RequestParamMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(RequestParamMapMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(PathVariableMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(PathVariableMapMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(MatrixVariableMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(MatrixVariableMapMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(ModelAttributeMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(RequestHeaderMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(RequestHeaderMapMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(CookieValueMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(ExpressionValueMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(SessionAttributeMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(RequestAttributeMethodArgumentResolver.class, next(resolvers, index).getClass());

		assertEquals(ModelArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(ErrorsMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(ServerWebExchangeArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(PrincipalArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(WebSessionArgumentResolver.class, next(resolvers, index).getClass());

		assertEquals(CustomArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(CustomSyncArgumentResolver.class, next(resolvers, index).getClass());

		assertEquals(RequestParamMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(ModelAttributeMethodArgumentResolver.class, next(resolvers, index).getClass());
	}

	@Test
	public void initBinderArgumentResolvers() {
		List<SyncInvocableHandlerMethod> methods =
				this.methodResolver.getInitBinderMethods(this.handlerMethod);

		assertEquals("Expected one each from Controller + ControllerAdvice", 2, methods.size());
		SyncInvocableHandlerMethod invocable = methods.get(0);
		List<SyncHandlerMethodArgumentResolver> resolvers = invocable.getResolvers();

		AtomicInteger index = new AtomicInteger(-1);
		assertEquals(RequestParamMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(RequestParamMapMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(PathVariableMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(PathVariableMapMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(MatrixVariableMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(MatrixVariableMapMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(RequestHeaderMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(RequestHeaderMapMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(CookieValueMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(ExpressionValueMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(RequestAttributeMethodArgumentResolver.class, next(resolvers, index).getClass());

		assertEquals(ModelArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(ServerWebExchangeArgumentResolver.class, next(resolvers, index).getClass());

		assertEquals(CustomSyncArgumentResolver.class, next(resolvers, index).getClass());

		assertEquals(RequestParamMethodArgumentResolver.class, next(resolvers, index).getClass());
	}

	@Test
	public void exceptionHandlerArgumentResolvers() {
		InvocableHandlerMethod invocable = this.methodResolver.getExceptionHandlerMethod(
				new ResponseStatusException(HttpStatus.BAD_REQUEST, "reason"), this.handlerMethod);

		assertNotNull("No match", invocable);
		assertEquals(TestController.class, invocable.getBeanType());
		List<HandlerMethodArgumentResolver> resolvers = invocable.getResolvers();

		AtomicInteger index = new AtomicInteger(-1);
		assertEquals(RequestParamMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(RequestParamMapMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(PathVariableMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(PathVariableMapMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(MatrixVariableMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(MatrixVariableMapMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(RequestHeaderMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(RequestHeaderMapMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(CookieValueMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(ExpressionValueMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(SessionAttributeMethodArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(RequestAttributeMethodArgumentResolver.class, next(resolvers, index).getClass());

		assertEquals(ModelArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(ServerWebExchangeArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(PrincipalArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(WebSessionArgumentResolver.class, next(resolvers, index).getClass());

		assertEquals(CustomArgumentResolver.class, next(resolvers, index).getClass());
		assertEquals(CustomSyncArgumentResolver.class, next(resolvers, index).getClass());

		assertEquals(RequestParamMethodArgumentResolver.class, next(resolvers, index).getClass());
	}

	@Test
	public void exceptionHandlerFromControllerAdvice() {
		InvocableHandlerMethod invocable = this.methodResolver.getExceptionHandlerMethod(
				new IllegalStateException("reason"), this.handlerMethod);

		assertNotNull(invocable);
		assertEquals(TestControllerAdvice.class, invocable.getBeanType());
	}


	private static HandlerMethodArgumentResolver next(
			List<? extends HandlerMethodArgumentResolver> resolvers, AtomicInteger index) {

		return resolvers.get(index.incrementAndGet());
	}


	@Controller
	static class TestController {

		@InitBinder
		void initDataBinder() {}

		@ModelAttribute
		void initModel() {}

		@GetMapping
		void handle() {}

		@ExceptionHandler
		void handleException(ResponseStatusException ex) {}

	}


	@ControllerAdvice
	static class TestControllerAdvice {

		@InitBinder
		void initDataBinder() {}

		@ModelAttribute
		void initModel() {}

		@ExceptionHandler
		void handleException(IllegalStateException ex) {}

	}


	static class CustomArgumentResolver implements HandlerMethodArgumentResolver {

		@Override
		public boolean supportsParameter(MethodParameter p) {
			return false;
		}

		@Override
		public Mono<Object> resolveArgument(MethodParameter p, BindingContext c, ServerWebExchange e) {
			return null;
		}
	}


	static class CustomSyncArgumentResolver extends CustomArgumentResolver
			implements SyncHandlerMethodArgumentResolver {

		@Override
		public Object resolveArgumentValue(MethodParameter p, BindingContext c, ServerWebExchange e) {
			return null;
		}
	}

}
