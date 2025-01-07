/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.Map;

import jakarta.servlet.ServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.ResolvableType;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.bind.support.BindParamNameResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link ExtendedServletRequestDataBinder}.
 *
 * @author Rossen Stoyanchev
 */
class ExtendedServletRequestDataBinderTests {

	private MockHttpServletRequest request;


	@BeforeEach
	void setup() {
		this.request = new MockHttpServletRequest();
	}


	@Test
	void createBinderViaSetters() {
		request.setAttribute(
				HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
				Map.of("name", "John", "age", "25"));

		request.addHeader("Some-Int-Array", "1");
		request.addHeader("Some-Int-Array", "2");

		TestBean target = new TestBean();
		ServletRequestDataBinder binder = new ExtendedServletRequestDataBinder(target, "");
		binder.bind(request);

		assertThat(target.getName()).isEqualTo("John");
		assertThat(target.getAge()).isEqualTo(25);
		assertThat(target.getSomeIntArray()).containsExactly(1, 2);
	}

	@Test
	void createBinderViaConstructor() {
		request.setAttribute(
				HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
				Map.of("name", "John", "age", "25"));

		request.addHeader("Some-Int-Array", "1");
		request.addHeader("Some-Int-Array", "2");

		ServletRequestDataBinder binder = new ExtendedServletRequestDataBinder(null);
		binder.setTargetType(ResolvableType.forClass(DataBean.class));
		binder.setNameResolver(new BindParamNameResolver());
		binder.construct(request);

		DataBean bean = (DataBean) binder.getTarget();

		assertThat(bean.name()).isEqualTo("John");
		assertThat(bean.age()).isEqualTo(25);
		assertThat(bean.someIntArray()).containsExactly(1, 2);
	}

	@Test
	void uriVarsAndHeadersAddedConditionally() {
		request.addParameter("name", "John");
		request.addParameter("age", "25");

		request.addHeader("name", "Johnny");
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("age", "26"));

		TestBean target = new TestBean();
		ServletRequestDataBinder binder = new ExtendedServletRequestDataBinder(target, "");
		binder.bind(request);

		assertThat(target.getName()).isEqualTo("John");
		assertThat(target.getAge()).isEqualTo(25);
	}

	@ParameterizedTest
	@ValueSource(strings = {"Accept", "Authorization", "Connection",
			"Cookie", "From", "Host", "Origin", "Priority", "Range", "Referer", "Upgrade"})
	void filteredHeaders(String headerName) {
		TestBinder binder = new TestBinder();

		MutablePropertyValues mpvs = new MutablePropertyValues();
		request.addHeader(headerName, "u1");
		binder.addBindValues(mpvs, request);

		assertThat(mpvs).isEmpty();
	}

	@Test
	void headerPredicate() {
		TestBinder binder = new TestBinder();
		binder.addHeaderPredicate(name -> !name.equalsIgnoreCase("Another-Int-Array"));

		MutablePropertyValues mpvs = new MutablePropertyValues();
		request.addHeader("Priority", "u1");
		request.addHeader("Some-Int-Array", "1");
		request.addHeader("Another-Int-Array", "1");

		binder.addBindValues(mpvs, request);

		assertThat(mpvs.size()).isEqualTo(1);
		assertThat(mpvs.get("someIntArray")).isEqualTo("1");
	}

	@Test
	void noUriTemplateVars() {
		TestBean target = new TestBean();
		ServletRequestDataBinder binder = new ExtendedServletRequestDataBinder(target, "");
		binder.bind(request);

		assertThat(target.getName()).isNull();
		assertThat(target.getAge()).isEqualTo(0);
	}


	private record DataBean(String name, int age, @BindParam("Some-Int-Array") Integer[] someIntArray) {
	}


	private static class TestBinder extends ExtendedServletRequestDataBinder {

		public TestBinder() {
			super(null);
		}

		@Override
		public void addBindValues(MutablePropertyValues mpvs, ServletRequest request) {
			super.addBindValues(mpvs, request);
		}
	}

}
