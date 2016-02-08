/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.reactive.handler;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.rx.Stream;
import rx.Observable;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.convert.support.ReactiveStreamsToCompletableFutureConverter;
import org.springframework.core.convert.support.ReactiveStreamsToReactorStreamConverter;
import org.springframework.core.convert.support.ReactiveStreamsToRxJava1Converter;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerResult;

/**
 * @author Sebastien Deleuze
 */
public class SimpleHandlerResultHandlerTests {

	@Test
	public void supports() throws NoSuchMethodException {

		SimpleHandlerResultHandler resultHandler = new SimpleHandlerResultHandler();
		TestController controller = new TestController();

		HandlerMethod hm = new HandlerMethod(controller, TestController.class.getMethod("voidReturnValue"));
		ResolvableType type = ResolvableType.forMethodParameter(hm.getReturnType());
		assertTrue(resultHandler.supports(createHandlerResult(hm, type)));

		hm = new HandlerMethod(controller, TestController.class.getMethod("publisherString"));
		type = ResolvableType.forMethodParameter(hm.getReturnType());
		assertFalse(resultHandler.supports(createHandlerResult(hm, type)));

		hm = new HandlerMethod(controller, TestController.class.getMethod("publisherVoid"));
		type = ResolvableType.forMethodParameter(hm.getReturnType());
		assertTrue(resultHandler.supports(createHandlerResult(hm, type)));

		hm = new HandlerMethod(controller, TestController.class.getMethod("streamVoid"));
		type = ResolvableType.forMethodParameter(hm.getReturnType());
		// Reactor Stream is a Publisher
		assertTrue(resultHandler.supports(createHandlerResult(hm, type)));

		hm = new HandlerMethod(controller, TestController.class.getMethod("observableVoid"));
		type = ResolvableType.forMethodParameter(hm.getReturnType());
		assertFalse(resultHandler.supports(createHandlerResult(hm, type)));

		hm = new HandlerMethod(controller, TestController.class.getMethod("completableFutureVoid"));
		type = ResolvableType.forMethodParameter(hm.getReturnType());
		assertFalse(resultHandler.supports(createHandlerResult(hm, type)));
	}

	@Test
	public void supportsWithConversionService() throws NoSuchMethodException {

		GenericConversionService conversionService = new GenericConversionService();
		conversionService.addConverter(new ReactiveStreamsToCompletableFutureConverter());
		conversionService.addConverter(new ReactiveStreamsToReactorStreamConverter());
		conversionService.addConverter(new ReactiveStreamsToRxJava1Converter());
		SimpleHandlerResultHandler resultHandler = new SimpleHandlerResultHandler(conversionService);
		TestController controller = new TestController();

		HandlerMethod hm = new HandlerMethod(controller, TestController.class.getMethod("voidReturnValue"));
		ResolvableType type = ResolvableType.forMethodParameter(hm.getReturnType());
		assertTrue(resultHandler.supports(createHandlerResult(hm, type)));

		hm = new HandlerMethod(controller, TestController.class.getMethod("publisherString"));
		type = ResolvableType.forMethodParameter(hm.getReturnType());
		assertFalse(resultHandler.supports(createHandlerResult(hm, type)));

		hm = new HandlerMethod(controller, TestController.class.getMethod("publisherVoid"));
		type = ResolvableType.forMethodParameter(hm.getReturnType());
		assertTrue(resultHandler.supports(createHandlerResult(hm, type)));

		hm = new HandlerMethod(controller, TestController.class.getMethod("streamVoid"));
		type = ResolvableType.forMethodParameter(hm.getReturnType());
		assertTrue(resultHandler.supports(createHandlerResult(hm, type)));

		hm = new HandlerMethod(controller, TestController.class.getMethod("observableVoid"));
		type = ResolvableType.forMethodParameter(hm.getReturnType());
		assertTrue(resultHandler.supports(createHandlerResult(hm, type)));

		hm = new HandlerMethod(controller, TestController.class.getMethod("completableFutureVoid"));
		type = ResolvableType.forMethodParameter(hm.getReturnType());
		assertTrue(resultHandler.supports(createHandlerResult(hm, type)));
	}

	private HandlerResult createHandlerResult(HandlerMethod hm, ResolvableType type) {
		return new HandlerResult(hm, null, type, new ExtendedModelMap());
	}


	@SuppressWarnings("unused")
	private static class TestController {

		public void voidReturnValue() {
		}

		public Publisher<String> publisherString() {
			return null;
		}

		public Publisher<Void> publisherVoid() {
			return null;
		}

		public Stream<Void> streamVoid() {
			return null;
		}

		public Observable<Void> observableVoid() {
			return null;
		}

		public CompletableFuture<Void> completableFutureVoid() {
			return null;
		}
	}

}
