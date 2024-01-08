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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RootClassFilter}.
 *
 * @author Sam Brannen
 * @since 5.1.10
 */
class RootClassFilterTests {

	private final ClassFilter filter1 = new RootClassFilter(Exception.class);
	private final ClassFilter filter2 = new RootClassFilter(Exception.class);
	private final ClassFilter filter3 = new RootClassFilter(ITestBean.class);


	@Test
	void matches() {
		assertThat(filter1.matches(Exception.class)).isTrue();
		assertThat(filter1.matches(RuntimeException.class)).isTrue();
		assertThat(filter1.matches(Error.class)).isFalse();
	}

	@Test
	void testEquals() {
		assertThat(filter1).isEqualTo(filter2);
		assertThat(filter1).isNotEqualTo(filter3);
	}

	@Test
	void testHashCode() {
		assertThat(filter1.hashCode()).isEqualTo(filter2.hashCode());
		assertThat(filter1.hashCode()).isNotEqualTo(filter3.hashCode());
	}

	@Test
	void testToString() {
		assertThat(filter1.toString()).isEqualTo("org.springframework.aop.support.RootClassFilter: java.lang.Exception");
		assertThat(filter1.toString()).isEqualTo(filter2.toString());
	}

}
