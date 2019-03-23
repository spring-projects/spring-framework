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
package org.springframework.web.method;

import java.util.function.Predicate;

import org.junit.Test;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link HandlerTypePredicate}.
 * @author Rossen Stoyanchev
 */
public class HandlerTypePredicateTests {

	@Test
	public void forAnnotation() {

		Predicate<Class<?>> predicate = HandlerTypePredicate.forAnnotation(Controller.class);

		assertTrue(predicate.test(HtmlController.class));
		assertTrue(predicate.test(ApiController.class));
		assertTrue(predicate.test(AnotherApiController.class));
	}

	@Test
	public void forAnnotationWithException() {

		Predicate<Class<?>> predicate = HandlerTypePredicate.forAnnotation(Controller.class)
				.and(HandlerTypePredicate.forAssignableType(Special.class));

		assertFalse(predicate.test(HtmlController.class));
		assertFalse(predicate.test(ApiController.class));
		assertTrue(predicate.test(AnotherApiController.class));
	}


	@Controller
	private static class HtmlController {}

	@RestController
	private static class ApiController {}

	@RestController
	private static class AnotherApiController implements Special {}

	interface Special {}

}
