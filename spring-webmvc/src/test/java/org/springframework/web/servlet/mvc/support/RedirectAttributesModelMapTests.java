/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.servlet.mvc.support;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.validation.DataBinder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * Test fixture for {@link RedirectAttributesModelMap} tests.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
class RedirectAttributesModelMapTests {

	private RedirectAttributesModelMap redirectAttributes;

	private FormattingConversionService conversionService;

	@BeforeEach
	public void setup() {
		this.conversionService = new DefaultFormattingConversionService();
		DataBinder dataBinder = new DataBinder(null);
		dataBinder.setConversionService(conversionService);

		this.redirectAttributes = new RedirectAttributesModelMap(dataBinder);
	}

	@Test
	void addAttributePrimitiveType() {
		this.redirectAttributes.addAttribute("speed", 65);
		assertThat(this.redirectAttributes.get("speed")).isEqualTo("65");
	}

	@Test
	void addAttributeCustomType() {
		String attrName = "person";
		this.redirectAttributes.addAttribute(attrName, new TestBean("Fred"));

		assertThat(this.redirectAttributes.get(attrName)).as("ConversionService should have invoked toString()").isEqualTo("Fred");

		this.conversionService.addConverter(new TestBeanConverter());
		this.redirectAttributes.addAttribute(attrName, new TestBean("Fred"));

		assertThat(this.redirectAttributes.get(attrName)).as("Type converter should have been used").isEqualTo("[Fred]");
	}

	@Test
	void addAttributeToString() {
		String attrName = "person";
		RedirectAttributesModelMap model = new RedirectAttributesModelMap();
		model.addAttribute(attrName, new TestBean("Fred"));

		assertThat(model.get(attrName)).as("toString() should have been used").isEqualTo("Fred");
	}

	@Test
	void addAttributeValue() {
		this.redirectAttributes.addAttribute(new TestBean("Fred"));

		assertThat(this.redirectAttributes.get("testBean")).isEqualTo("Fred");
	}

	@Test
	void addAllAttributesList() {
		this.redirectAttributes.addAllAttributes(Arrays.asList(new TestBean("Fred"), 5));

		assertThat(this.redirectAttributes.get("testBean")).isEqualTo("Fred");
		assertThat(this.redirectAttributes.get("integer")).isEqualTo("5");
	}

	@Test
	void addAttributesMap() {
		Map<String, Object> map = new HashMap<>();
		map.put("person", new TestBean("Fred"));
		map.put("age", 33);
		this.redirectAttributes.addAllAttributes(map);

		assertThat(this.redirectAttributes.get("person")).isEqualTo("Fred");
		assertThat(this.redirectAttributes.get("age")).isEqualTo("33");
	}

	@Test
	void mergeAttributes() {
		Map<String, Object> map = new HashMap<>();
		map.put("person", new TestBean("Fred"));
		map.put("age", 33);

		this.redirectAttributes.addAttribute("person", new TestBean("Ralph"));
		this.redirectAttributes.mergeAttributes(map);

		assertThat(this.redirectAttributes.get("person")).isEqualTo("Ralph");
		assertThat(this.redirectAttributes.get("age")).isEqualTo("33");
	}

	@Test
	void put() {
		this.redirectAttributes.put("testBean", new TestBean("Fred"));

		assertThat(this.redirectAttributes.get("testBean")).isEqualTo("Fred");
	}

	@Test
	void putAll() {
		Map<String, Object> map = new HashMap<>();
		map.put("person", new TestBean("Fred"));
		map.put("age", 33);
		this.redirectAttributes.putAll(map);

		assertThat(this.redirectAttributes.get("person")).isEqualTo("Fred");
		assertThat(this.redirectAttributes.get("age")).isEqualTo("33");
	}

	public static class TestBeanConverter implements Converter<TestBean, String> {

		@Override
		public String convert(TestBean source) {
			return "[" + source.getName() + "]";
		}
	}

}
