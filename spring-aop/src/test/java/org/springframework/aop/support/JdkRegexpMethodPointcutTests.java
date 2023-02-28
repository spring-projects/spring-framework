/*
 * Copyright 2002-2022 the original author or authors.
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

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.testfixture.io.SerializationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Dmitriy Kopylenko
 * @author Chris Beams
 * @author Dmitriy Kopylenko
 */
class JdkRegexpMethodPointcutTests {

	private AbstractRegexpMethodPointcut rpc = new JdkRegexpMethodPointcut();


	@Test
	void noPatternSupplied() throws Exception {
		noPatternSuppliedTests(rpc);
	}

	@Test
	void serializationWithNoPatternSupplied() throws Exception {
		rpc = SerializationTestUtils.serializeAndDeserialize(rpc);
		noPatternSuppliedTests(rpc);
	}

	private void noPatternSuppliedTests(AbstractRegexpMethodPointcut rpc) throws Exception {
		assertThat(rpc.matches(Object.class.getMethod("hashCode"), String.class)).isFalse();
		assertThat(rpc.matches(Object.class.getMethod("wait"), Object.class)).isFalse();
		assertThat(rpc.getPatterns()).isEmpty();
	}

	@Test
	void exactMatch() throws Exception {
		rpc.setPattern("java.lang.Object.hashCode");
		exactMatchTests(rpc);
		rpc = SerializationTestUtils.serializeAndDeserialize(rpc);
		exactMatchTests(rpc);
	}

	private void exactMatchTests(AbstractRegexpMethodPointcut rpc) throws Exception {
		// assumes rpc.setPattern("java.lang.Object.hashCode");
		assertThat(rpc.matches(Object.class.getMethod("hashCode"), String.class)).isTrue();
		assertThat(rpc.matches(Object.class.getMethod("hashCode"), Object.class)).isTrue();
		assertThat(rpc.matches(Object.class.getMethod("wait"), Object.class)).isFalse();
	}

	@Test
	void specificMatch() throws Exception {
		rpc.setPattern("java.lang.String.hashCode");
		assertThat(rpc.matches(Object.class.getMethod("hashCode"), String.class)).isTrue();
		assertThat(rpc.matches(Object.class.getMethod("hashCode"), Object.class)).isFalse();
	}

	@Test
	void wildcard() throws Exception {
		rpc.setPattern(".*Object.hashCode");
		assertThat(rpc.matches(Object.class.getMethod("hashCode"), Object.class)).isTrue();
		assertThat(rpc.matches(Object.class.getMethod("wait"), Object.class)).isFalse();
	}

	@Test
	void wildcardForOneClass() throws Exception {
		rpc.setPattern("java.lang.Object.*");
		assertThat(rpc.matches(Object.class.getMethod("hashCode"), String.class)).isTrue();
		assertThat(rpc.matches(Object.class.getMethod("wait"), String.class)).isTrue();
	}

	@Test
	void matchesObjectClass() throws Exception {
		rpc.setPattern("java.lang.Object.*");
		assertThat(rpc.matches(Exception.class.getMethod("hashCode"), IOException.class)).isTrue();
		// Doesn't match a method from Throwable
		assertThat(rpc.matches(Exception.class.getMethod("getMessage"), Exception.class)).isFalse();
	}

	@Test
	void withExclusion() throws Exception {
		this.rpc.setPattern(".*get.*");
		this.rpc.setExcludedPattern(".*Age.*");
		assertThat(this.rpc.matches(TestBean.class.getMethod("getName"), TestBean.class)).isTrue();
		assertThat(this.rpc.matches(TestBean.class.getMethod("getAge"), TestBean.class)).isFalse();
	}

}
