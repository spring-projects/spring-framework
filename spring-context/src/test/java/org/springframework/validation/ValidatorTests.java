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

package org.springframework.validation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class ValidatorTests {

	@Test
	void testSupportsForInstanceOf() {
		Validator validator = Validator.forInstanceOf(TestBean.class, (testBean, errors) -> {});
		assertThat(validator.supports(TestBean.class)).isTrue();
		assertThat(validator.supports(TestBeanSubclass.class)).isTrue();
	}

	@Test
	void testSupportsForType() {
		Validator validator = Validator.forType(TestBean.class, (testBean, errors) -> {});
		assertThat(validator.supports(TestBean.class)).isTrue();
		assertThat(validator.supports(TestBeanSubclass.class)).isFalse();
	}


	private static class TestBeanSubclass extends TestBean {
	}

}
