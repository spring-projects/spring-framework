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

package org.springframework.aot.generate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link MethodName}.
 *
 * @author Phillip Webb
 */
class MethodNameTests {

	@Test
	void ofWhenPartsIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> MethodName.of((String[]) null))
				.withMessage("'parts' must not be null");
	}

	@Test
	void ofReturnsMethodName() {
		assertThat(MethodName.of("get", "bean", "factory")).hasToString("getBeanFactory");
		assertThat(MethodName.of("get", null, "factory")).hasToString("getFactory");
		assertThat(MethodName.of(null, null)).hasToString("$$aot");
		assertThat(MethodName.of("", null)).hasToString("$$aot");
		assertThat(MethodName.of("get", "InputStream")).hasToString("getInputStream");
		assertThat(MethodName.of("register", "myBean123", "bean")).hasToString("registerMyBeanBean");
		assertThat(MethodName.of("register", "org.springframework.example.bean"))
				.hasToString("registerOrgSpringframeworkExampleBean");
	}

	@Test
	void andPartsWhenPartsIsNullThrowsException() {
		MethodName name = MethodName.of("myBean");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> name.and(((String[]) null)))
				.withMessage("'parts' must not be null");
	}

	@Test
	void andPartsReturnsMethodName() {
		MethodName name = MethodName.of("myBean");
		assertThat(name.and("test")).hasToString("myBeanTest");
		assertThat(name.and("test", null)).hasToString("myBeanTest");
		assertThat(name.and("getName")).hasToString("getMyBeanName");
		assertThat(name.and("setName")).hasToString("setMyBeanName");
		assertThat(name.and("isDoingOk")).hasToString("isMyBeanDoingOk");
		assertThat(name.and("this", "that", "the", "other")).hasToString("myBeanThisThatTheOther");
	}

	@Test
	void andNameWhenPartsIsNullThrowsException() {
		MethodName name = MethodName.of("myBean");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> name.and(((MethodName) null)))
				.withMessage("'name' must not be null");
	}

	@Test
	void andNameReturnsMethodName() {
		MethodName name = MethodName.of("myBean");
		assertThat(name.and(MethodName.of("test"))).hasToString("myBeanTest");
	}

	@Test
	void hashCodeAndEquals() {
		MethodName name1 = MethodName.of("myBean");
		MethodName name2 = MethodName.of("my", "bean");
		MethodName name3 = MethodName.of("myOtherBean");
		assertThat(name1.hashCode()).isEqualTo(name2.hashCode());
		assertThat(name1).isEqualTo(name1).isEqualTo(name2).isNotEqualTo(name3);
	}

}
