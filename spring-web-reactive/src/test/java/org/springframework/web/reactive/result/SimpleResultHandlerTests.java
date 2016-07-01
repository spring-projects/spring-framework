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

package org.springframework.web.reactive.result;

import java.util.concurrent.CompletableFuture;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import rx.Observable;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.convert.support.MonoToCompletableFutureConverter;
import org.springframework.core.convert.support.PublisherToFluxConverter;
import org.springframework.core.convert.support.ReactorToRxJava1Converter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerResult;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link SimpleResultHandler}.
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class SimpleResultHandlerTests {

	@Test
	public void supportsWithConversionService() throws NoSuchMethodException {

		GenericConversionService conversionService = new GenericConversionService();
		conversionService.addConverter(new MonoToCompletableFutureConverter());
		conversionService.addConverter(new PublisherToFluxConverter());
		conversionService.addConverter(new ReactorToRxJava1Converter());

		SimpleResultHandler resultHandler = new SimpleResultHandler(conversionService);
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
		return new HandlerResult(hm, null, type);
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

		public Flux<Void> streamVoid() {
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
