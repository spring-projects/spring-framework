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

package org.springframework.reactive.web.dispatch.method.annotation;

import java.util.Collections;

import org.junit.Test;
import org.reactivestreams.Publisher;

import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.reactive.codec.encoder.StringEncoder;
import org.springframework.reactive.web.dispatch.HandlerResult;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Sebastien Deleuze
 */
public class ResponseBodyResultHandlerTests {


	@Test
	public void supports() throws NoSuchMethodException {
		ResponseBodyResultHandler handler = new ResponseBodyResultHandler(Collections.singletonList(
				new StringEncoder()), new DefaultConversionService());
		TestController controller = new TestController();

		HandlerMethod hm = new HandlerMethod(controller,TestController.class.getMethod("notAnnotated"));
		assertFalse(handler.supports(new HandlerResult(hm, null)));

		hm = new HandlerMethod(controller, TestController.class.getMethod("publisherString"));
		assertTrue(handler.supports(new HandlerResult(hm, null)));

		hm = new HandlerMethod(controller, TestController.class.getMethod("publisherVoid"));
		assertTrue(handler.supports(new HandlerResult(hm, null)));
	}


	@SuppressWarnings("unused")
	private static class TestController {

		public Publisher<String> notAnnotated() {
			return null;
		}

		@ResponseBody
		public Publisher<String> publisherString() {
			return null;
		}

		@ResponseBody
		public Publisher<Void> publisherVoid() {
			return null;
		}
	}

}
