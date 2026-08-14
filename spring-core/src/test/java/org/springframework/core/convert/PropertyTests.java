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
 * Tests for {@link Property} name resolution.
 *
 * @author Junhyeong Kim
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

	@Test  // regression guard for the indexOf -> startsWith fix: "get" embedded mid-name
	void resolveNameForGetterEmbeddingGetInName() throws Exception {
		// with the former indexOf-based resolution this resolved to "" (matched "get" in "isTarget")
		assertThat(readProperty(TestBean.class, "isTarget").getName()).isEqualTo("target");
	}

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

	@Test  // component literally named "get": proves plain accessor detection must precede startsWith
	void resolveNameForRecordAccessorNamedGet() throws Exception {
		assertThat(readProperty(EdgeRecord.class, "get").getName()).isEqualTo("get");
	}

	@Test  // component literally named "is": proves plain accessor detection must precede startsWith
	void resolveNameForRecordAccessorNamedIs() throws Exception {
		assertThat(readProperty(EdgeRecord.class, "is").getName()).isEqualTo("is");
	}

	@Test  // component literally named "getWidget": plain accessor detection must beat prefix stripping
	void resolveNameForRecordAccessorNamedGetWidget() throws Exception {
		assertThat(readProperty(EdgeRecord.class, "getWidget").getName()).isEqualTo("getWidget");
	}

	@Test  // data class accessor whose name embeds the "get" prefix
	void resolveNameForDataClassAccessorEmbeddingGetPrefix() throws Exception {
		assertThat(readProperty(SampleDataClass.class, "budget").getName()).isEqualTo("budget");
	}

	@Test  // data class accessor whose name starts with the "is" prefix
	void resolveNameForDataClassAccessorStartingWithIsPrefix() throws Exception {
		assertThat(readProperty(SampleDataClass.class, "issue").getName()).isEqualTo("issue");
	}

	@Test  // plain data class accessor with no prefix collision (regression guard)
	void resolveNameForPlainDataClassAccessor() throws Exception {
		assertThat(readProperty(SampleDataClass.class, "name").getName()).isEqualTo("name");
	}

	@Test  // a JavaBeans-style getter without a backing field must still be stripped
	void resolveNameForGetterDeclaredOnDataClass() throws Exception {
		assertThat(readProperty(SampleDataClass.class, "getWidget").getName()).isEqualTo("widget");
	}

	@Test  // a boolean getter backed by a field of the exact same name resolves to the field name
	void resolveNameForBooleanGetterBackedByFieldOfSameName() throws Exception {
		assertThat(readProperty(SampleDataClass.class, "isUrgent").getName()).isEqualTo("isUrgent");
	}

	@Test  // a static field of the same name must not make an instance getter a plain accessor
	void resolveNameForGetterWithStaticFieldOfSameName() throws Exception {
		assertThat(readProperty(StaticEdgeBean.class, "getCount").getName()).isEqualTo("count");
	}

	@Test  // a static method must not be treated as a plain accessor
	void resolveNameForStaticGetterWithInstanceFieldOfSameName() throws Exception {
		assertThat(readProperty(StaticEdgeBean.class, "getLabel").getName()).isEqualTo("label");
	}


	private static Property readProperty(Class<?> objectType, String readMethodName) throws Exception {
		Method readMethod = objectType.getMethod(readMethodName);
		return new Property(objectType, readMethod, null);
	}

	private static Property writeProperty(String writeMethodName) throws Exception {
		Method writeMethod = TestBean.class.getMethod(writeMethodName, String.class);
		return new Property(TestBean.class, null, writeMethod);
	}


	@SuppressWarnings("unused")
	static class TestBean {

		public String getName() {
			return null;
		}

		public boolean isEnabled() {
			return false;
		}

		public boolean isTarget() {
			return false;
		}

		public void setName(String name) {
		}

		public void updateName(String name) {
		}

		public void offsetX(String value) {
		}

		public void upset(String value) {
		}
	}

	record SampleRecord(String name, String budget, String issue) {

		public String getWidget() {
			return null;
		}
	}

	record EdgeRecord(String get, String is, String getWidget) {
	}

	@SuppressWarnings("unused")
	static class SampleDataClass {

		private final String name = null;

		private final String budget = null;

		private final String issue = null;

		private final boolean isUrgent = false;

		public String name() {
			return this.name;
		}

		public String budget() {
			return this.budget;
		}

		public String issue() {
			return this.issue;
		}

		public boolean isUrgent() {
			return this.isUrgent;
		}

		public String getWidget() {
			return null;
		}
	}

	@SuppressWarnings("unused")
	static class StaticEdgeBean {

		private static String getCount = null;

		private String getLabel = null;

		public String getCount() {
			return null;
		}

		public static String getLabel() {
			return null;
		}
	}

}
