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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.reactivestreams.Publisher;

import org.springframework.reactive.web.dispatch.HandlerResult;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;

/**
 * @author Sebastien Deleuze
 */
public class ResponseBodyResultHandlerTests {

	@Test
	public void supports() throws NoSuchMethodException {
		ResponseBodyResultHandler resultHandler = new ResponseBodyResultHandler(Collections.emptyList());
		TestController controller = new TestController();

		HandlerMethod notAnnotatedMethod = new HandlerMethod(controller, TestController.class.getMethod("notAnnotated"));
		assertFalse(resultHandler.supports(new HandlerResult(notAnnotatedMethod, null)));

		HandlerMethod publisherStringMethod = new HandlerMethod(controller, TestController.class.getMethod("publisherString"));
		assertTrue(resultHandler.supports(new HandlerResult(publisherStringMethod, null)));

		HandlerMethod publisherVoidMethod = new HandlerMethod(controller, TestController.class.getMethod("publisherVoid"));
		assertTrue(resultHandler.supports(new HandlerResult(publisherVoidMethod, null)));
	}


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
