/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.function;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.servlet.handler.PathPatternsTestUtils;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class RequestPredicateTests {

	private ServerRequest request;

	@BeforeEach
	void createRequest() {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		this.request = new DefaultServerRequest(servletRequest, Collections.emptyList());
	}

	@Test
	void and() {
		RequestPredicate predicate1 = request -> true;
		RequestPredicate predicate2 = request -> true;
		RequestPredicate predicate3 = request -> false;

		assertThat(predicate1.and(predicate2).test(request)).isTrue();
		assertThat(predicate2.and(predicate1).test(request)).isTrue();
		assertThat(predicate1.and(predicate3).test(request)).isFalse();
	}

	@Test
	void negate() {
		RequestPredicate predicate = request -> false;
		RequestPredicate negated = predicate.negate();

		assertThat(negated.test(request)).isTrue();

		predicate = request -> true;
		negated = predicate.negate();

		assertThat(negated.test(request)).isFalse();
	}

	@Test
	void or() {
		RequestPredicate predicate1 = request -> true;
		RequestPredicate predicate2 = request -> false;
		RequestPredicate predicate3 = request -> false;

		assertThat(predicate1.or(predicate2).test(request)).isTrue();
		assertThat(predicate2.or(predicate1).test(request)).isTrue();
		assertThat(predicate2.or(predicate3).test(request)).isFalse();
	}

}
