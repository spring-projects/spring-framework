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

package org.springframework.web.method;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HandlerTypePredicate}.
 *
 * @author Rossen Stoyanchev
 */
class HandlerTypePredicateTests {

	@Test
	void forAnnotation() {

		Predicate<Class<?>> predicate = HandlerTypePredicate.forAnnotation(Controller.class);

		assertThat(predicate.test(HtmlController.class)).isTrue();
		assertThat(predicate.test(ApiController.class)).isTrue();
		assertThat(predicate.test(AnotherApiController.class)).isTrue();
	}

	@Test
	void forAnnotationWithException() {

		Predicate<Class<?>> predicate = HandlerTypePredicate.forAnnotation(Controller.class)
				.and(HandlerTypePredicate.forAssignableType(Special.class));

		assertThat(predicate.test(HtmlController.class)).isFalse();
		assertThat(predicate.test(ApiController.class)).isFalse();
		assertThat(predicate.test(AnotherApiController.class)).isTrue();
	}

	@Test
	void selectorsAreNotChangedByBuilderAfterBuild() {

		HandlerTypePredicate.Builder builder = HandlerTypePredicate.builder().annotation(RestController.class);
		Predicate<Class<?>> predicate = builder.build();

		builder.annotation(Controller.class).basePackage("java.lang").assignableType(Special.class);

		assertThat(predicate.test(ApiController.class)).isTrue();
		assertThat(predicate.test(HtmlController.class)).isFalse();
		assertThat(predicate.test(String.class)).isFalse();
		assertThat(predicate.test(SpecialHandler.class)).isFalse();

		Predicate<Class<?>> newPredicate = builder.build();
		assertThat(newPredicate.test(HtmlController.class)).isTrue();
		assertThat(newPredicate.test(String.class)).isTrue();
		assertThat(newPredicate.test(SpecialHandler.class)).isTrue();
	}


	@Controller
	private static class HtmlController {}

	@RestController
	private static class ApiController {}

	@RestController
	private static class AnotherApiController implements Special {}

	private static class SpecialHandler implements Special {}

	interface Special {}

}
