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

/**
 * Tests for {@link Property} name resolution.
 *
 * @author junhyeong9812
 */
class PropertyTests {

	@Test
	void resolveNameForStandardGetter() throws Exception {
		assertThat(readProperty(TestBean.class, "getName").getName()).isEqualTo("name");
	}

	@Test
	void resolveNameForBooleanGetter() throws Exception {
		assertThat(readProperty(TestBean.class, "isEnabled").getName()).isEqualTo("enabled");
	}

	@Test
	void resolveNameForSetter() throws Exception {
		Method setter = TestBean.class.getMethod("setName", String.class);
		assertThat(new Property(TestBean.class, null, setter).getName()).isEqualTo("name");
	}

	@Test  // record component accessor whose name embeds the "get" prefix
	void resolveNameForRecordAccessorEmbeddingGetPrefix() throws Exception {
		assertThat(readProperty(SampleRecord.class, "budget").getName()).isEqualTo("budget");
	}

	@Test  // record component accessor whose name starts with the "is" prefix
	void resolveNameForRecordAccessorStartingWithIsPrefix() throws Exception {
		assertThat(readProperty(SampleRecord.class, "issue").getName()).isEqualTo("issue");
	}

	@Test  // plain record component accessor with no prefix collision (regression guard)
	void resolveNameForPlainRecordAccessor() throws Exception {
		assertThat(readProperty(SampleRecord.class, "name").getName()).isEqualTo("name");
	}

	@Test  // a JavaBeans-style getter declared on a record must still be stripped
	void resolveNameForGetterDeclaredOnRecord() throws Exception {
		assertThat(readProperty(SampleRecord.class, "getWidget").getName()).isEqualTo("widget");
	}

	@Test  // component literally named "get": proves record detection must precede startsWith
	void resolveNameForRecordAccessorNamedGet() throws Exception {
		assertThat(readProperty(EdgeRecord.class, "get").getName()).isEqualTo("get");
	}

	@Test  // component literally named "is": proves record detection must precede startsWith
	void resolveNameForRecordAccessorNamedIs() throws Exception {
		assertThat(readProperty(EdgeRecord.class, "is").getName()).isEqualTo("is");
	}


	private static Property readProperty(Class<?> objectType, String readMethodName) throws Exception {
		Method readMethod = objectType.getMethod(readMethodName);
		return new Property(objectType, readMethod, null);
	}


	@SuppressWarnings("unused")
	static class TestBean {

		public String getName() {
			return null;
		}

		public boolean isEnabled() {
			return false;
		}

		public void setName(String name) {
		}
	}

	record SampleRecord(String name, String budget, String issue) {

		public String getWidget() {
			return null;
		}
	}

	record EdgeRecord(String get, String is) {
	}

}
