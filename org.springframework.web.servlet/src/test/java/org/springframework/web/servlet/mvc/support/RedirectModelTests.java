/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.mvc.support;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.TestBean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.ui.ModelMap;
import org.springframework.validation.DataBinder;

/**
 * 
 * Test fixture for RedirectModel tests.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class RedirectModelTests {

	private RedirectModel redirectModel;
	
	private FormattingConversionService conversionService;

	private ModelMap implicitModel;
	
	@Before
	public void setup() {
		this.conversionService = new DefaultFormattingConversionService();
		DataBinder dataBinder = new DataBinder(null);
		dataBinder.setConversionService(conversionService);
		
		this.implicitModel = new ModelMap();
		this.redirectModel = new RedirectModel(dataBinder, implicitModel);
	}
	
	@Test
	public void addAttributePrimitiveType() {
		this.redirectModel.addAttribute("speed", 65);
		assertEquals("65", this.redirectModel.get("speed"));
	}

	@Test
	public void addAttributeCustomType() {
		String attrName = "person";
		this.redirectModel.addAttribute(attrName, new TestBean("Fred"));
		
		assertEquals("ConversionService should have invoked toString()", "Fred", this.redirectModel.get(attrName));

		this.conversionService.addConverter(new TestBeanConverter());
		this.redirectModel.addAttribute(attrName, new TestBean("Fred"));
		
		assertEquals("Type converter should have been used", "[Fred]", this.redirectModel.get(attrName));
	}

	@Test
	public void addAttributeToString() {
		String attrName = "person";
		RedirectModel model = new RedirectModel();
		model.addAttribute(attrName, new TestBean("Fred"));

		assertEquals("toString() should have been used", "Fred", model.get(attrName));
	}

	@Test
	public void addAttributeValue() {
		this.redirectModel.addAttribute(new TestBean("Fred"));
		
		assertEquals("Fred", this.redirectModel.get("testBean"));
	}

	@Test
	public void addAllAttributesList() {
		this.redirectModel.addAllAttributes(Arrays.asList(new TestBean("Fred"), new Integer(5)));
		
		assertEquals("Fred", this.redirectModel.get("testBean"));
		assertEquals("5", this.redirectModel.get("integer"));
	}

	@Test
	public void addAttributesMap() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("person", new TestBean("Fred"));
		map.put("age", 33);
		this.redirectModel.addAllAttributes(map);
		
		assertEquals("Fred", this.redirectModel.get("person"));
		assertEquals("33", this.redirectModel.get("age"));
	}

	@Test
	public void mergeAttributes() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("person", new TestBean("Fred"));
		map.put("age", 33);
		
		this.redirectModel.addAttribute("person", new TestBean("Ralph"));
		this.redirectModel.mergeAttributes(map);
		
		assertEquals("Ralph", this.redirectModel.get("person"));
		assertEquals("33", this.redirectModel.get("age"));
	}
	
	@Test
	public void addModelAttributes() {
		this.implicitModel.addAttribute("person", new TestBean("Ralph"));
		this.implicitModel.addAttribute("age", 33);
		
		this.redirectModel.addModelAttributes("person", "age");
		
		assertEquals("Ralph", this.redirectModel.get("person"));
		assertEquals("33", this.redirectModel.get("age"));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void addModelAttributesInvalidName() {
		this.redirectModel.addModelAttributes("person");
	}
	
	public static class TestBeanConverter implements Converter<TestBean, String> {
		
		public String convert(TestBean source) {
			return "[" + source.getName() + "]";
		}
	}	
	
}
