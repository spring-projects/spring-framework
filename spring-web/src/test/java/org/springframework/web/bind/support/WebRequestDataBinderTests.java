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

package org.springframework.web.bind.support;

import java.beans.PropertyEditorSupport;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.support.StringMultipartFileEditor;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockMultipartFile;
import org.springframework.web.testfixture.servlet.MockMultipartHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.bind.WebDataBinder.DEFAULT_FIELD_DEFAULT_PREFIX;
import static org.springframework.web.bind.WebDataBinder.DEFAULT_FIELD_MARKER_PREFIX;

/**
 * Tests for {@link WebRequestDataBinder}.
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @author Sam Brannen
 */
class WebRequestDataBinderTests {

	@Test
	void bindingWithNestedObjectCreation() {
		TestBean tb = new TestBean();

		WebRequestDataBinder binder = new WebRequestDataBinder(tb);
		binder.registerCustomEditor(ITestBean.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean());
			}
		});

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("spouse", "someValue");
		request.addParameter("spouse.name", "test");
		binder.bind(new ServletWebRequest(request));

		assertThat(tb.getSpouse()).isNotNull();
		assertThat(tb.getSpouse().getName()).isEqualTo("test");
	}

	@Test
	void bindingWithNestedObjectCreationThroughAutoGrow() {
		TestBean tb = new TestBeanWithConcreteSpouse();

		WebRequestDataBinder binder = new WebRequestDataBinder(tb);
		binder.setIgnoreUnknownFields(false);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("concreteSpouse.name", "test");
		binder.bind(new ServletWebRequest(request));

		assertThat(tb.getSpouse()).isNotNull();
		assertThat(tb.getSpouse().getName()).isEqualTo("test");
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void markerPrefixCausesFieldReset(boolean ignoreUnknownFields) {
		TestBean target = new TestBean();

		WebRequestDataBinder binder = new WebRequestDataBinder(target);
		binder.setIgnoreUnknownFields(ignoreUnknownFields);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("_postProcessed", "visible");
		request.addParameter("postProcessed", "on");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.isPostProcessed()).isTrue();

		request.removeParameter("postProcessed");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.isPostProcessed()).isFalse();
	}

	@Test
	void fieldWithArrayIndices() {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("stringArray[0]", "ONE");
		request.addParameter("stringArray[1]", "TWO");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.getStringArray()).containsExactly("ONE", "TWO");
	}

	@Test
	void fieldWithMissingArrayIndex() {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("stringArray", "ONE");
		request.addParameter("stringArray", "TWO");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.getStringArray()).containsExactly("ONE", "TWO");
	}

	@Test  // gh-25836
	void fieldWithEmptyArrayIndex() {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("stringArray[]", "ONE");
		request.addParameter("stringArray[]", "TWO");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.getStringArray()).containsExactly("ONE", "TWO");
	}

	@Test
	void fieldDefault() {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("!postProcessed", "off");
		request.addParameter("postProcessed", "on");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.isPostProcessed()).isTrue();

		request.removeParameter("postProcessed");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.isPostProcessed()).isFalse();
	}

	@Test  // SPR-13502
	void collectionFieldsDefault() {
		TestBean target = new TestBean();
		target.setSomeSet(null);
		target.setSomeList(null);
		target.setSomeMap(null);
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("_someSet", "visible");
		request.addParameter("_someList", "visible");
		request.addParameter("_someMap", "visible");

		binder.bind(new ServletWebRequest(request));
		assertThat(target.getSomeSet()).isInstanceOf(Set.class);
		assertThat(target.getSomeList()).isInstanceOf(List.class);
		assertThat(target.getSomeMap()).isInstanceOf(Map.class);
	}

	@Test
	void fieldDefaultPreemptsFieldMarker() {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("!postProcessed", "on");
		request.addParameter("_postProcessed", "visible");
		request.addParameter("postProcessed", "on");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.isPostProcessed()).isTrue();

		request.removeParameter("postProcessed");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.isPostProcessed()).isTrue();

		request.removeParameter("!postProcessed");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.isPostProcessed()).isFalse();
	}

	@Test
	void fieldDefaultWithNestedProperty() {
		TestBean target = new TestBean();
		target.setSpouse(new TestBean());
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("!spouse.postProcessed", "on");
		request.addParameter("_spouse.postProcessed", "visible");
		request.addParameter("spouse.postProcessed", "on");
		binder.bind(new ServletWebRequest(request));
		assertThat(((TestBean) target.getSpouse()).isPostProcessed()).isTrue();

		request.removeParameter("spouse.postProcessed");
		binder.bind(new ServletWebRequest(request));
		assertThat(((TestBean) target.getSpouse()).isPostProcessed()).isTrue();

		request.removeParameter("!spouse.postProcessed");
		binder.bind(new ServletWebRequest(request));
		assertThat(((TestBean) target.getSpouse()).isPostProcessed()).isFalse();
	}

	@Test
	void fieldDefaultNonBoolean() {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("!name", "anonymous");
		request.addParameter("name", "Scott");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.getName()).isEqualTo("Scott");

		request.removeParameter("name");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.getName()).isEqualTo("anonymous");
	}

	@Test
	void withCommaSeparatedStringArray() {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("stringArray", "bar");
		request.addParameter("stringArray", "abc");
		request.addParameter("stringArray", "123,def");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.getStringArray().length).as("Expected all three items to be bound").isEqualTo(3);

		request.removeParameter("stringArray");
		request.addParameter("stringArray", "123,def");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.getStringArray().length).as("Expected only 1 item to be bound").isEqualTo(1);
	}

	@Test
	void enumBinding() {
		EnumHolder target = new EnumHolder();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("myEnum", "FOO");
		binder.bind(new ServletWebRequest(request));
		assertThat(target.getMyEnum()).isEqualTo(MyEnum.FOO);
	}

	@Test
	void multipartFileAsString() {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);
		binder.registerCustomEditor(String.class, new StringMultipartFileEditor());

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(new MockMultipartFile("name", "Juergen".getBytes()));
		binder.bind(new ServletWebRequest(request));
		assertThat(target.getName()).isEqualTo("Juergen");
	}

	@Test
	void multipartFileAsStringArray() {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);
		binder.registerCustomEditor(String.class, new StringMultipartFileEditor());

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(new MockMultipartFile("stringArray", "Juergen".getBytes()));
		binder.bind(new ServletWebRequest(request));
		assertThat(target.getStringArray()).hasSize(1);
		assertThat(target.getStringArray()[0]).isEqualTo("Juergen");
	}

	@Test
	void multipartFilesAsStringArray() {
		TestBean target = new TestBean();
		WebRequestDataBinder binder = new WebRequestDataBinder(target);
		binder.registerCustomEditor(String.class, new StringMultipartFileEditor());

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(new MockMultipartFile("stringArray", "Juergen".getBytes()));
		request.addFile(new MockMultipartFile("stringArray", "Eva".getBytes()));
		binder.bind(new ServletWebRequest(request));
		assertThat(target.getStringArray()).hasSize(2);
		assertThat(target.getStringArray()[0]).isEqualTo("Juergen");
		assertThat(target.getStringArray()[1]).isEqualTo("Eva");
	}


	@ParameterizedClass  // gh-36625
	@ValueSource(strings = { DEFAULT_FIELD_DEFAULT_PREFIX, DEFAULT_FIELD_MARKER_PREFIX })
	@Nested
	class DefaultAndMarkerPrefixTests {

		@Parameter
		String prefix;

		@Test
		void shouldNotTriggerBindingWhenFieldIsNotAllowed() {
			TestBean tb = new TestBean();

			WebRequestDataBinder binder = new WebRequestDataBinder(tb);
			binder.setAllowedFields("name");

			MockHttpServletRequest request = new MockHttpServletRequest();
			request.addParameter("name", "spring");
			request.addParameter(prefix + "country", "test");
			binder.bind(new ServletWebRequest(request));

			assertThat(tb.getName()).isEqualTo("spring");
			assertThat(tb.getCountry()).isNull();
		}

		@Test
		void shouldNotTriggerBindingWhenFieldIsDisallowed() {
			TestBean tb = new TestBean();

			WebRequestDataBinder binder = new WebRequestDataBinder(tb);
			binder.setDisallowedFields("country");

			MockHttpServletRequest request = new MockHttpServletRequest();
			request.addParameter("name", "spring");
			request.addParameter(prefix + "country", "test");
			binder.bind(new ServletWebRequest(request));

			assertThat(tb.getName()).isEqualTo("spring");
			assertThat(tb.getCountry()).isNull();
		}

		@Test
		void shouldNotTriggerBindingWhenFieldIsNotAllowedWithEmptyArrayIndex() {
			TestBean tb = new TestBean();

			WebRequestDataBinder binder = new WebRequestDataBinder(tb);
			binder.setAllowedFields("name");

			MockHttpServletRequest request = new MockHttpServletRequest();
			request.addParameter("name", "spring");
			request.addParameter(prefix + "stringArray[]", "ONE");
			request.addParameter(prefix + "stringArray[]", "TWO");
			binder.bind(new ServletWebRequest(request));

			assertThat(tb.getName()).isEqualTo("spring");
			assertThat(tb.getStringArray()).isNull();
		}

		@ParameterizedTest
		@ValueSource(strings = { "stringArray*", "stringArray[]" })
		void shouldNotTriggerBindingWhenFieldIsDisallowedWithEmptyArrayIndex(String disallowedField) {
			TestBean tb = new TestBean();

			WebRequestDataBinder binder = new WebRequestDataBinder(tb);
			binder.setDisallowedFields(disallowedField);

			MockHttpServletRequest request = new MockHttpServletRequest();
			request.addParameter("name", "spring");
			request.addParameter(prefix + "stringArray[]", "ONE");
			request.addParameter(prefix + "stringArray[]", "TWO");
			binder.bind(new ServletWebRequest(request));

			assertThat(tb.getName()).isEqualTo("spring");
			assertThat(tb.getStringArray()).isNull();
		}

		@Test
		void shouldNotTriggerAutoGrowWhenFieldIsNotAllowed() {
			TestBean tb = new TestBean();
			tb.setSomeMap(null);

			WebRequestDataBinder binder = new WebRequestDataBinder(tb);
			binder.setAllowedFields("name");

			MockHttpServletRequest request = new MockHttpServletRequest();
			request.addParameter("name", "spring");
			request.addParameter(prefix + "someMap[key1]", "test");
			binder.bind(new ServletWebRequest(request));

			assertThat(tb.getName()).isEqualTo("spring");
			assertThat(tb.getSomeMap()).isNull();
		}

		@ParameterizedTest
		@ValueSource(strings = { "someMap*", "someMap[*]", "someMap[key1]" })
		void shouldNotTriggerAutoGrowWhenFieldIsDisallowed(String disallowedField) {
			TestBean tb = new TestBean();
			tb.setSomeMap(null);

			WebRequestDataBinder binder = new WebRequestDataBinder(tb);
			binder.setDisallowedFields(disallowedField);

			MockHttpServletRequest request = new MockHttpServletRequest();
			request.addParameter("name", "spring");
			request.addParameter(prefix + "someMap[key1]", "test");
			binder.bind(new ServletWebRequest(request));

			assertThat(tb.getName()).isEqualTo("spring");
			assertThat(tb.getSomeMap()).isNull();
		}
	}


	public static class EnumHolder {

		private MyEnum myEnum;

		public MyEnum getMyEnum() {
			return myEnum;
		}

		public void setMyEnum(MyEnum myEnum) {
			this.myEnum = myEnum;
		}
	}

	public enum MyEnum {
		FOO, BAR
	}

	static class TestBeanWithConcreteSpouse extends TestBean {
		public void setConcreteSpouse(TestBean spouse) {
			setSpouse(spouse);
		}

		public TestBean getConcreteSpouse() {
			return (TestBean) getSpouse();
		}
	}

}
