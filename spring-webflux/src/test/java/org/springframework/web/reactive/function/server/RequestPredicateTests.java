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

package org.springframework.web.reactive.function.server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class RequestPredicateTests {

	@Test
	public void and() {
		RequestPredicate predicate1 = request -> true;
		RequestPredicate predicate2 = request -> true;
		RequestPredicate predicate3 = request -> false;

		MockServerRequest request = MockServerRequest.builder().build();
		assertThat(predicate1.and(predicate2).test(request)).isTrue();
		assertThat(predicate2.and(predicate1).test(request)).isTrue();
		assertThat(predicate1.and(predicate3).test(request)).isFalse();
	}

	@Test
	public void negate() {
		RequestPredicate predicate = request -> false;
		RequestPredicate negated = predicate.negate();

		MockServerRequest mockRequest = MockServerRequest.builder().build();
		assertThat(negated.test(mockRequest)).isTrue();

		predicate = request -> true;
		negated = predicate.negate();

		assertThat(negated.test(mockRequest)).isFalse();
	}

	@Test
	public void or() {
		RequestPredicate predicate1 = request -> true;
		RequestPredicate predicate2 = request -> false;
		RequestPredicate predicate3 = request -> false;

		MockServerRequest request = MockServerRequest.builder().build();
		assertThat(predicate1.or(predicate2).test(request)).isTrue();
		assertThat(predicate2.or(predicate1).test(request)).isTrue();
		assertThat(predicate2.or(predicate3).test(request)).isFalse();
	}

}
