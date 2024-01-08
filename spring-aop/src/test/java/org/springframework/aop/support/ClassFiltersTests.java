/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.aop.support;

import org.junit.jupiter.api.Test;

import org.springframework.aop.ClassFilter;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.NestedRuntimeException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ClassFilters}.
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @author Sam Brannen
 */
class ClassFiltersTests {

	private final ClassFilter exceptionFilter = new RootClassFilter(Exception.class);

	private final ClassFilter interfaceFilter = new RootClassFilter(ITestBean.class);

	private final ClassFilter hasRootCauseFilter = new RootClassFilter(NestedRuntimeException.class);


	@Test
	void union() {
		assertThat(exceptionFilter.matches(RuntimeException.class)).isTrue();
		assertThat(exceptionFilter.matches(TestBean.class)).isFalse();
		assertThat(interfaceFilter.matches(Exception.class)).isFalse();
		assertThat(interfaceFilter.matches(TestBean.class)).isTrue();
		ClassFilter union = ClassFilters.union(exceptionFilter, interfaceFilter);
		assertThat(union.matches(RuntimeException.class)).isTrue();
		assertThat(union.matches(TestBean.class)).isTrue();
		assertThat(union.toString())
			.matches("^.+UnionClassFilter: \\[.+RootClassFilter: .+Exception, .+RootClassFilter: .+TestBean\\]$");
	}

	@Test
	void intersection() {
		assertThat(exceptionFilter.matches(RuntimeException.class)).isTrue();
		assertThat(hasRootCauseFilter.matches(NestedRuntimeException.class)).isTrue();
		ClassFilter intersection = ClassFilters.intersection(exceptionFilter, hasRootCauseFilter);
		assertThat(intersection.matches(RuntimeException.class)).isFalse();
		assertThat(intersection.matches(TestBean.class)).isFalse();
		assertThat(intersection.matches(NestedRuntimeException.class)).isTrue();
		assertThat(intersection.toString())
			.matches("^.+IntersectionClassFilter: \\[.+RootClassFilter: .+Exception, .+RootClassFilter: .+NestedRuntimeException\\]$");
	}

	@Test
	void negateClassFilter() {
		ClassFilter filter = mock(ClassFilter.class);
		given(filter.matches(String.class)).willReturn(true);
		ClassFilter negate = ClassFilters.negate(filter);
		assertThat(negate.matches(String.class)).isFalse();
		verify(filter).matches(String.class);
	}

	@Test
	void negateTrueClassFilter() {
		ClassFilter negate = ClassFilters.negate(ClassFilter.TRUE);
		assertThat(negate.matches(String.class)).isFalse();
		assertThat(negate.matches(Object.class)).isFalse();
		assertThat(negate.matches(Integer.class)).isFalse();
	}

	@Test
	void negateTrueClassFilterAppliedTwice() {
		ClassFilter negate = ClassFilters.negate(ClassFilters.negate(ClassFilter.TRUE));
		assertThat(negate.matches(String.class)).isTrue();
		assertThat(negate.matches(Object.class)).isTrue();
		assertThat(negate.matches(Integer.class)).isTrue();
	}

	@Test
	void negateIsNotEqualsToOriginalFilter() {
		ClassFilter original = ClassFilter.TRUE;
		ClassFilter negate = ClassFilters.negate(original);
		assertThat(original).isNotEqualTo(negate);
	}

	@Test
	void negateOnSameFilterIsEquals() {
		ClassFilter original = ClassFilter.TRUE;
		ClassFilter first = ClassFilters.negate(original);
		ClassFilter second = ClassFilters.negate(original);
		assertThat(first).isEqualTo(second);
	}

	@Test
	void negateHasNotSameHashCodeAsOriginalFilter() {
		ClassFilter original = ClassFilter.TRUE;
		ClassFilter negate = ClassFilters.negate(original);
		assertThat(original).doesNotHaveSameHashCodeAs(negate);
	}

	@Test
	void negateOnSameFilterHasSameHashCode() {
		ClassFilter original = ClassFilter.TRUE;
		ClassFilter first = ClassFilters.negate(original);
		ClassFilter second = ClassFilters.negate(original);
		assertThat(first).hasSameHashCodeAs(second);
	}

	@Test
	void toStringIncludesRepresentationOfOriginalFilter() {
		ClassFilter original = ClassFilter.TRUE;
		assertThat(ClassFilters.negate(original)).hasToString("Negate " + original);
	}

}
