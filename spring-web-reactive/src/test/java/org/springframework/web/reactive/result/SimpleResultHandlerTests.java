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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import rx.Observable;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.support.MonoToCompletableFutureConverter;
import org.springframework.core.convert.support.ReactorToRxJava1Converter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.web.reactive.HandlerResult;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link SimpleResultHandler}.
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class SimpleResultHandlerTests {

	private SimpleResultHandler resultHandler;


	@Before
	public void setUp() throws Exception {
		FormattingConversionService service = new DefaultFormattingConversionService();
		service.addConverter(new MonoToCompletableFutureConverter());
		service.addConverter(new ReactorToRxJava1Converter());
		this.resultHandler = new SimpleResultHandler(service);
	}


	@Test
	public void supports() throws NoSuchMethodException {
		testSupports(ResolvableType.forClass(void.class), true);
		testSupports(ResolvableType.forClassWithGenerics(Publisher.class, Void.class), true);
		testSupports(ResolvableType.forClassWithGenerics(Flux.class, Void.class), true);
		testSupports(ResolvableType.forClassWithGenerics(Observable.class, Void.class), true);
		testSupports(ResolvableType.forClassWithGenerics(CompletableFuture.class, Void.class), true);

		testSupports(ResolvableType.forClass(String.class), false);
		testSupports(ResolvableType.forClassWithGenerics(Publisher.class, String.class), false);
	}

	@Test
	public void supportsUsesGenericTypeInformation() throws Exception {
		testSupports(ResolvableType.forClassWithGenerics(List.class, Void.class), false);
	}

	private void testSupports(ResolvableType type, boolean result) {
		MethodParameter param = ResolvableMethod.onClass(TestController.class).returning(type).resolveReturnType();
		HandlerResult handlerResult = new HandlerResult(new TestController(), null, param);
		assertEquals(result, this.resultHandler.supports(handlerResult));
	}


	@SuppressWarnings("unused")
	private static class TestController {

		public void voidReturn() { }

		public Publisher<String> publisherString() { return null; }

		public Flux<Void> flux() { return null; }

		public Observable<Void> observable() { return null; }

		public CompletableFuture<Void> completableFuture() { return null; }

		public String string() { return null; }

		public Publisher<Void> publisher() { return null; }

		public List<Void> list() { return null; }

	}

}
