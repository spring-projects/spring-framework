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

package org.springframework.aop.support;

import org.junit.jupiter.api.Test;

import org.springframework.aop.ClassFilter;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.NestedRuntimeException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ClassFilters}.
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

}
