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

package org.springframework.core.convert;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Property} setter name resolution.
 *
 * @author Junhyeong Kim
 */
class PropertyTests {

	@Test
	void resolveNameForSetter() throws Exception {
		assertThat(writeProperty("setName").getName()).isEqualTo("name");
	}

	@Test  // no "set" token at all: rejected before and after this change
	void rejectNonSetterWriteMethod() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> writeProperty("updateName"))
				.withMessage("Not a setter method");
	}

	@Test  // "set" embedded mid-name: formerly accepted and resolved to "x"
	void rejectWriteMethodEmbeddingSetInName() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> writeProperty("offsetX"))
				.withMessage("Not a setter method");
	}

	@Test  // "set" at the end of the name: formerly accepted and resolved to ""
	void rejectWriteMethodEndingWithSetToken() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> writeProperty("upset"))
				.withMessage("Not a setter method");
	}


	private static Property writeProperty(String writeMethodName) throws Exception {
		Method writeMethod = TestBean.class.getMethod(writeMethodName, String.class);
		return new Property(TestBean.class, null, writeMethod);
	}


	@SuppressWarnings("unused")
	static class TestBean {

		public void setName(String name) {
		}

		public void updateName(String name) {
		}

		public void offsetX(String value) {
		}

		public void upset(String value) {
		}
	}

}
