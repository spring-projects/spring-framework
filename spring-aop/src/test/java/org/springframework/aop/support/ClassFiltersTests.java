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
import org.springframework.core.NestedRuntimeException;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class ClassFiltersTests {

	private ClassFilter exceptionFilter = new RootClassFilter(Exception.class);

	private ClassFilter itbFilter = new RootClassFilter(ITestBean.class);

	private ClassFilter hasRootCauseFilter = new RootClassFilter(NestedRuntimeException.class);

	@Test
	public void testUnion() {
		assertThat(exceptionFilter.matches(RuntimeException.class)).isTrue();
		assertThat(exceptionFilter.matches(TestBean.class)).isFalse();
		assertThat(itbFilter.matches(Exception.class)).isFalse();
		assertThat(itbFilter.matches(TestBean.class)).isTrue();
		ClassFilter union = ClassFilters.union(exceptionFilter, itbFilter);
		assertThat(union.matches(RuntimeException.class)).isTrue();
		assertThat(union.matches(TestBean.class)).isTrue();
	}

	@Test
	public void testIntersection() {
		assertThat(exceptionFilter.matches(RuntimeException.class)).isTrue();
		assertThat(hasRootCauseFilter.matches(NestedRuntimeException.class)).isTrue();
		ClassFilter intersection = ClassFilters.intersection(exceptionFilter, hasRootCauseFilter);
		assertThat(intersection.matches(RuntimeException.class)).isFalse();
		assertThat(intersection.matches(TestBean.class)).isFalse();
		assertThat(intersection.matches(NestedRuntimeException.class)).isTrue();
	}

}
