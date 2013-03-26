/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.validation;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.springframework.tests.sample.beans.BeanWithObjectProperty;
import org.springframework.tests.sample.beans.DerivedTestBean;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.IndexedTestBean;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.beans.NullValueInNestedPathException;
import org.springframework.tests.sample.beans.SerializablePerson;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.beans.propertyeditors.CustomCollectionEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.Formatter;
import org.springframework.format.number.NumberFormatter;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.util.StringUtils;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 */
public class DataBinderTests extends TestCase {

	public void testBindingNoErrors() throws Exception {
		TestBean rod = new TestBean();
		DataBinder binder = new DataBinder(rod, "person");
		assertTrue(binder.isIgnoreUnknownFields());
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "Rod");
		pvs.add("age", "032");
		pvs.add("nonExisting", "someValue");

		binder.bind(pvs);
		binder.close();

		assertTrue("changed name correctly", rod.getName().equals("Rod"));
		assertTrue("changed age correctly", rod.getAge() == 32);

		Map<?, ?> map = binder.getBindingResult().getModel();
		assertTrue("There is one element in map", map.size() == 2);
		TestBean tb = (TestBean) map.get("person");
		assertTrue("Same object", tb.equals(rod));

		BindingResult other = new BeanPropertyBindingResult(rod, "person");
		assertEquals(other, binder.getBindingResult());
		assertEquals(binder.getBindingResult(), other);
		BindException ex = new BindException(other);
		assertEquals(ex, other);
		assertEquals(other, ex);
		assertEquals(ex, binder.getBindingResult());
		assertEquals(binder.getBindingResult(), ex);

		other.reject("xxx");
		assertTrue(!other.equals(binder.getBindingResult()));
	}

	public void testedBindingWithDefaultConversionNoErrors() throws Exception {
		TestBean rod = new TestBean();
		DataBinder binder = new DataBinder(rod, "person");
		assertTrue(binder.isIgnoreUnknownFields());
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "Rod");
		pvs.add("jedi", "on");

		binder.bind(pvs);
		binder.close();

		assertEquals("Rod", rod.getName());
		assertTrue(rod.isJedi());
	}

	public void testedNestedBindingWithDefaultConversionNoErrors() throws Exception {
		TestBean rod = new TestBean(new TestBean());
		DataBinder binder = new DataBinder(rod, "person");
		assertTrue(binder.isIgnoreUnknownFields());
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("spouse.name", "Kerry");
		pvs.add("spouse.jedi", "on");

		binder.bind(pvs);
		binder.close();

		assertEquals("Kerry", rod.getSpouse().getName());
		assertTrue(((TestBean) rod.getSpouse()).isJedi());
	}

	public void testBindingNoErrorsNotIgnoreUnknown() throws Exception {
		TestBean rod = new TestBean();
		DataBinder binder = new DataBinder(rod, "person");
		binder.setIgnoreUnknownFields(false);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "Rod");
		pvs.add("age", new Integer(32));
		pvs.add("nonExisting", "someValue");

		try {
			binder.bind(pvs);
			fail("Should have thrown NotWritablePropertyException");
		}
		catch (NotWritablePropertyException ex) {
			// expected
		}
	}

	public void testBindingNoErrorsWithInvalidField() throws Exception {
		TestBean rod = new TestBean();
		DataBinder binder = new DataBinder(rod, "person");
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "Rod");
		pvs.add("spouse.age", new Integer(32));

		try {
			binder.bind(pvs);
			fail("Should have thrown NullValueInNestedPathException");
		}
		catch (NullValueInNestedPathException ex) {
			// expected
		}
	}

	public void testBindingNoErrorsWithIgnoreInvalid() throws Exception {
		TestBean rod = new TestBean();
		DataBinder binder = new DataBinder(rod, "person");
		binder.setIgnoreInvalidFields(true);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "Rod");
		pvs.add("spouse.age", new Integer(32));

		binder.bind(pvs);
	}

	public void testBindingWithErrors() throws Exception {
		TestBean rod = new TestBean();
		DataBinder binder = new DataBinder(rod, "person");
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "Rod");
		pvs.add("age", "32x");
		pvs.add("touchy", "m.y");
		binder.bind(pvs);

		try {
			binder.close();
			fail("Should have thrown BindException");
		}
		catch (BindException ex) {
			assertTrue("changed name correctly", rod.getName().equals("Rod"));
			//assertTrue("changed age correctly", rod.getAge() == 32);

			Map<?, ?> map = binder.getBindingResult().getModel();
			//assertTrue("There are 3 element in map", m.size() == 1);
			TestBean tb = (TestBean) map.get("person");
			assertTrue("Same object", tb.equals(rod));

			BindingResult br = (BindingResult) map.get(BindingResult.MODEL_KEY_PREFIX + "person");
			assertSame(br, BindingResultUtils.getBindingResult(map, "person"));
			assertSame(br, BindingResultUtils.getRequiredBindingResult(map, "person"));

			assertNull(BindingResultUtils.getBindingResult(map, "someOtherName"));
			try {
				BindingResultUtils.getRequiredBindingResult(map, "someOtherName");
				fail("Should have thrown IllegalStateException");
			}
			catch (IllegalStateException expected) {
			}

			assertTrue("Added itself to map", br == binder.getBindingResult());
			assertTrue(br.hasErrors());
			assertTrue("Correct number of errors", br.getErrorCount() == 2);

			assertTrue("Has age errors", br.hasFieldErrors("age"));
			assertTrue("Correct number of age errors", br.getFieldErrorCount("age") == 1);
			assertEquals("32x", binder.getBindingResult().getFieldValue("age"));
			assertEquals("32x", binder.getBindingResult().getFieldError("age").getRejectedValue());
			assertEquals(0, tb.getAge());

			assertTrue("Has touchy errors", br.hasFieldErrors("touchy"));
			assertTrue("Correct number of touchy errors", br.getFieldErrorCount("touchy") == 1);
			assertEquals("m.y", binder.getBindingResult().getFieldValue("touchy"));
			assertEquals("m.y", binder.getBindingResult().getFieldError("touchy").getRejectedValue());
			assertNull(tb.getTouchy());

			rod = new TestBean();
			binder = new DataBinder(rod, "person");
			pvs = new MutablePropertyValues();
			pvs.add("name", "Rod");
			pvs.add("age", "32x");
			pvs.add("touchy", "m.y");
			binder.bind(pvs);
			assertEquals(binder.getBindingResult(), ex.getBindingResult());
		}
	}

	public void testBindingWithSystemFieldError() throws Exception {
		TestBean rod = new TestBean();
		DataBinder binder = new DataBinder(rod, "person");
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("class.classLoader.URLs[0]", "http://myserver");
		binder.setIgnoreUnknownFields(false);

		try {
			binder.bind(pvs);
			fail("Should have thrown NotWritablePropertyException");
		}
		catch (NotWritablePropertyException ex) {
			assertTrue(ex.getMessage().contains("classLoader"));
		}
	}

	public void testBindingWithErrorsAndCustomEditors() throws Exception {
		TestBean rod = new TestBean();
		DataBinder binder = new DataBinder(rod, "person");
		binder.registerCustomEditor(String.class, "touchy", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("prefix_" + text);
			}
			@Override
			public String getAsText() {
				return getValue().toString().substring(7);
			}
		});
		binder.registerCustomEditor(TestBean.class, "spouse", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean(text, 0));
			}
			@Override
			public String getAsText() {
				return ((TestBean) getValue()).getName();
			}
		});
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "Rod");
		pvs.add("age", "32x");
		pvs.add("touchy", "m.y");
		pvs.add("spouse", "Kerry");
		binder.bind(pvs);

		try {
			binder.close();
			fail("Should have thrown BindException");
		}
		catch (BindException ex) {
			assertTrue("changed name correctly", rod.getName().equals("Rod"));
			//assertTrue("changed age correctly", rod.getAge() == 32);

			Map<?, ?> model = binder.getBindingResult().getModel();
			//assertTrue("There are 3 element in map", m.size() == 1);
			TestBean tb = (TestBean) model.get("person");
			assertTrue("Same object", tb.equals(rod));

			BindingResult br = (BindingResult) model.get(BindingResult.MODEL_KEY_PREFIX + "person");
			assertTrue("Added itself to map", br == binder.getBindingResult());
			assertTrue(br.hasErrors());
			assertTrue("Correct number of errors", br.getErrorCount() == 2);

			assertTrue("Has age errors", br.hasFieldErrors("age"));
			assertTrue("Correct number of age errors", br.getFieldErrorCount("age") == 1);
			assertEquals("32x", binder.getBindingResult().getFieldValue("age"));
			assertEquals("32x", binder.getBindingResult().getFieldError("age").getRejectedValue());
			assertEquals(0, tb.getAge());

			assertTrue("Has touchy errors", br.hasFieldErrors("touchy"));
			assertTrue("Correct number of touchy errors", br.getFieldErrorCount("touchy") == 1);
			assertEquals("m.y", binder.getBindingResult().getFieldValue("touchy"));
			assertEquals("m.y", binder.getBindingResult().getFieldError("touchy").getRejectedValue());
			assertNull(tb.getTouchy());

			assertTrue("Does not have spouse errors", !br.hasFieldErrors("spouse"));
			assertEquals("Kerry", binder.getBindingResult().getFieldValue("spouse"));
			assertNotNull(tb.getSpouse());
		}
	}

	public void testBindingWithCustomEditorOnObjectField() {
		BeanWithObjectProperty tb = new BeanWithObjectProperty();
		DataBinder binder = new DataBinder(tb);
		binder.registerCustomEditor(Integer.class, "object", new CustomNumberEditor(Integer.class, true));
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("object", "1");
		binder.bind(pvs);
		assertEquals(new Integer(1), tb.getObject());
	}

	public void testBindingWithFormatter() {
		TestBean tb = new TestBean();
		DataBinder binder = new DataBinder(tb);
		FormattingConversionService conversionService = new FormattingConversionService();
		DefaultConversionService.addDefaultConverters(conversionService);
		conversionService.addFormatterForFieldType(Float.class, new NumberFormatter());
		binder.setConversionService(conversionService);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("myFloat", "1,2");

		LocaleContextHolder.setLocale(Locale.GERMAN);
		try {
			binder.bind(pvs);
			assertEquals(new Float(1.2), tb.getMyFloat());
			assertEquals("1,2", binder.getBindingResult().getFieldValue("myFloat"));

			PropertyEditor editor = binder.getBindingResult().findEditor("myFloat", Float.class);
			assertNotNull(editor);
			editor.setValue(new Float(1.4));
			assertEquals("1,4", editor.getAsText());

			editor = binder.getBindingResult().findEditor("myFloat", null);
			assertNotNull(editor);
			editor.setAsText("1,6");
			assertEquals(new Float(1.6), editor.getValue());
		}
		finally {
			LocaleContextHolder.resetLocaleContext();
		}
	}

	public void testBindingErrorWithFormatter() {
		TestBean tb = new TestBean();
		DataBinder binder = new DataBinder(tb);
		FormattingConversionService conversionService = new FormattingConversionService();
		DefaultConversionService.addDefaultConverters(conversionService);
		conversionService.addFormatterForFieldType(Float.class, new NumberFormatter());
		binder.setConversionService(conversionService);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("myFloat", "1x2");

		LocaleContextHolder.setLocale(Locale.GERMAN);
		try {
			binder.bind(pvs);
			assertEquals(new Float(0.0), tb.getMyFloat());
			assertEquals("1x2", binder.getBindingResult().getFieldValue("myFloat"));
			assertTrue(binder.getBindingResult().hasFieldErrors("myFloat"));
		}
		finally {
			LocaleContextHolder.resetLocaleContext();
		}
	}

	public void testBindingErrorWithStringFormatter() {
		TestBean tb = new TestBean();
		DataBinder binder = new DataBinder(tb);
		FormattingConversionService conversionService = new FormattingConversionService();
		DefaultConversionService.addDefaultConverters(conversionService);
		conversionService.addFormatterForFieldType(String.class, new Formatter<String>() {
			@Override
			public String parse(String text, Locale locale) throws ParseException {
				throw new ParseException(text, 0);
			}
			@Override
			public String print(String object, Locale locale) {
				return object;
			}
		});
		binder.setConversionService(conversionService);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "test");

		binder.bind(pvs);
		assertTrue(binder.getBindingResult().hasFieldErrors("name"));
		assertEquals("test", binder.getBindingResult().getFieldValue("name"));
	}

	public void testBindingWithFormatterAgainstList() {
		BeanWithIntegerList tb = new BeanWithIntegerList();
		DataBinder binder = new DataBinder(tb);
		FormattingConversionService conversionService = new FormattingConversionService();
		DefaultConversionService.addDefaultConverters(conversionService);
		conversionService.addFormatterForFieldType(Float.class, new NumberFormatter());
		binder.setConversionService(conversionService);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("integerList[0]", "1");

		LocaleContextHolder.setLocale(Locale.GERMAN);
		try {
			binder.bind(pvs);
			assertEquals(new Integer(1), tb.getIntegerList().get(0));
			assertEquals("1", binder.getBindingResult().getFieldValue("integerList[0]"));
		}
		finally {
			LocaleContextHolder.resetLocaleContext();
		}
	}

	public void testBindingErrorWithFormatterAgainstList() {
		BeanWithIntegerList tb = new BeanWithIntegerList();
		DataBinder binder = new DataBinder(tb);
		FormattingConversionService conversionService = new FormattingConversionService();
		DefaultConversionService.addDefaultConverters(conversionService);
		conversionService.addFormatterForFieldType(Float.class, new NumberFormatter());
		binder.setConversionService(conversionService);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("integerList[0]", "1x2");

		LocaleContextHolder.setLocale(Locale.GERMAN);
		try {
			binder.bind(pvs);
			assertTrue(tb.getIntegerList().isEmpty());
			assertEquals("1x2", binder.getBindingResult().getFieldValue("integerList[0]"));
			assertTrue(binder.getBindingResult().hasFieldErrors("integerList[0]"));
		}
		finally {
			LocaleContextHolder.resetLocaleContext();
		}
	}

	public void testBindingWithFormatterAgainstFields() {
		TestBean tb = new TestBean();
		DataBinder binder = new DataBinder(tb);
		FormattingConversionService conversionService = new FormattingConversionService();
		DefaultConversionService.addDefaultConverters(conversionService);
		conversionService.addFormatterForFieldType(Float.class, new NumberFormatter());
		binder.setConversionService(conversionService);
		binder.initDirectFieldAccess();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("myFloat", "1,2");

		LocaleContextHolder.setLocale(Locale.GERMAN);
		try {
			binder.bind(pvs);
			assertEquals(new Float(1.2), tb.getMyFloat());
			assertEquals("1,2", binder.getBindingResult().getFieldValue("myFloat"));

			PropertyEditor editor = binder.getBindingResult().findEditor("myFloat", Float.class);
			assertNotNull(editor);
			editor.setValue(new Float(1.4));
			assertEquals("1,4", editor.getAsText());

			editor = binder.getBindingResult().findEditor("myFloat", null);
			assertNotNull(editor);
			editor.setAsText("1,6");
			assertEquals(new Float(1.6), editor.getValue());
		}
		finally {
			LocaleContextHolder.resetLocaleContext();
		}
	}

	public void testBindingErrorWithFormatterAgainstFields() {
		TestBean tb = new TestBean();
		DataBinder binder = new DataBinder(tb);
		binder.initDirectFieldAccess();
		FormattingConversionService conversionService = new FormattingConversionService();
		DefaultConversionService.addDefaultConverters(conversionService);
		conversionService.addFormatterForFieldType(Float.class, new NumberFormatter());
		binder.setConversionService(conversionService);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("myFloat", "1x2");

		LocaleContextHolder.setLocale(Locale.GERMAN);
		try {
			binder.bind(pvs);
			assertEquals(new Float(0.0), tb.getMyFloat());
			assertEquals("1x2", binder.getBindingResult().getFieldValue("myFloat"));
			assertTrue(binder.getBindingResult().hasFieldErrors("myFloat"));
		}
		finally {
			LocaleContextHolder.resetLocaleContext();
		}
	}

	public void testBindingWithAllowedFields() throws Exception {
		TestBean rod = new TestBean();
		DataBinder binder = new DataBinder(rod);
		binder.setAllowedFields(new String[] {"name", "myparam"});
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "Rod");
		pvs.add("age", "32x");

		binder.bind(pvs);
		binder.close();
		assertTrue("changed name correctly", rod.getName().equals("Rod"));
		assertTrue("did not change age", rod.getAge() == 0);
	}

	public void testBindingWithDisallowedFields() throws Exception {
		TestBean rod = new TestBean();
		DataBinder binder = new DataBinder(rod);
		binder.setDisallowedFields(new String[] {"age"});
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "Rod");
		pvs.add("age", "32x");

		binder.bind(pvs);
		binder.close();
		assertTrue("changed name correctly", rod.getName().equals("Rod"));
		assertTrue("did not change age", rod.getAge() == 0);
		String[] disallowedFields = binder.getBindingResult().getSuppressedFields();
		assertEquals(1, disallowedFields.length);
		assertEquals("age", disallowedFields[0]);
	}

	public void testBindingWithAllowedAndDisallowedFields() throws Exception {
		TestBean rod = new TestBean();
		DataBinder binder = new DataBinder(rod);
		binder.setAllowedFields(new String[] {"name", "myparam"});
		binder.setDisallowedFields(new String[] {"age"});
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "Rod");
		pvs.add("age", "32x");

		binder.bind(pvs);
		binder.close();
		assertTrue("changed name correctly", rod.getName().equals("Rod"));
		assertTrue("did not change age", rod.getAge() == 0);
		String[] disallowedFields = binder.getBindingResult().getSuppressedFields();
		assertEquals(1, disallowedFields.length);
		assertEquals("age", disallowedFields[0]);
	}

	public void testBindingWithOverlappingAllowedAndDisallowedFields() throws Exception {
		TestBean rod = new TestBean();
		DataBinder binder = new DataBinder(rod);
		binder.setAllowedFields(new String[] {"name", "age"});
		binder.setDisallowedFields(new String[] {"age"});
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "Rod");
		pvs.add("age", "32x");

		binder.bind(pvs);
		binder.close();
		assertTrue("changed name correctly", rod.getName().equals("Rod"));
		assertTrue("did not change age", rod.getAge() == 0);
		String[] disallowedFields = binder.getBindingResult().getSuppressedFields();
		assertEquals(1, disallowedFields.length);
		assertEquals("age", disallowedFields[0]);
	}

	public void testBindingWithAllowedFieldsUsingAsterisks() throws Exception {
		TestBean rod = new TestBean();
		DataBinder binder = new DataBinder(rod, "person");
		binder.setAllowedFields(new String[] {"nam*", "*ouchy"});

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "Rod");
		pvs.add("touchy", "Rod");
		pvs.add("age", "32x");

		binder.bind(pvs);
		binder.close();

		assertTrue("changed name correctly", "Rod".equals(rod.getName()));
		assertTrue("changed touchy correctly", "Rod".equals(rod.getTouchy()));
		assertTrue("did not change age", rod.getAge() == 0);
		String[] disallowedFields = binder.getBindingResult().getSuppressedFields();
		assertEquals(1, disallowedFields.length);
		assertEquals("age", disallowedFields[0]);

		Map<?,?> m = binder.getBindingResult().getModel();
		assertTrue("There is one element in map", m.size() == 2);
		TestBean tb = (TestBean) m.get("person");
		assertTrue("Same object", tb.equals(rod));
	}

	public void testBindingWithAllowedAndDisallowedMapFields() throws Exception {
		TestBean rod = new TestBean();
		DataBinder binder = new DataBinder(rod);
		binder.setAllowedFields(new String[] {"someMap[key1]", "someMap[key2]"});
		binder.setDisallowedFields(new String[] {"someMap['key3']", "someMap[key4]"});

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("someMap[key1]", "value1");
		pvs.add("someMap['key2']", "value2");
		pvs.add("someMap[key3]", "value3");
		pvs.add("someMap['key4']", "value4");

		binder.bind(pvs);
		binder.close();
		assertEquals("value1", rod.getSomeMap().get("key1"));
		assertEquals("value2", rod.getSomeMap().get("key2"));
		assertNull(rod.getSomeMap().get("key3"));
		assertNull(rod.getSomeMap().get("key4"));
		String[] disallowedFields = binder.getBindingResult().getSuppressedFields();
		assertEquals(2, disallowedFields.length);
		assertEquals("someMap[key3]", disallowedFields[0]);
		assertEquals("someMap[key4]", disallowedFields[1]);
	}

	/**
	 * Tests for required field, both null, non-existing and empty strings.
	 */
	public void testBindingWithRequiredFields() throws Exception {
		TestBean tb = new TestBean();
		tb.setSpouse(new TestBean());

		DataBinder binder = new DataBinder(tb, "person");
		binder.setRequiredFields(new String[] {"touchy", "name", "age", "date", "spouse.name"});

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("touchy", "");
		pvs.add("name", null);
		pvs.add("age", null);
		pvs.add("spouse.name", "     ");

		binder.bind(pvs);

		BindingResult br = binder.getBindingResult();
		assertEquals("Wrong number of errors", 5, br.getErrorCount());

		assertEquals("required", br.getFieldError("touchy").getCode());
		assertEquals("", br.getFieldValue("touchy"));
		assertEquals("required", br.getFieldError("name").getCode());
		assertEquals("", br.getFieldValue("name"));
		assertEquals("required", br.getFieldError("age").getCode());
		assertEquals("", br.getFieldValue("age"));
		assertEquals("required", br.getFieldError("date").getCode());
		assertEquals("", br.getFieldValue("date"));
		assertEquals("required", br.getFieldError("spouse.name").getCode());
		assertEquals("", br.getFieldValue("spouse.name"));
	}

	public void testBindingWithRequiredMapFields() throws Exception {
		TestBean tb = new TestBean();
		tb.setSpouse(new TestBean());

		DataBinder binder = new DataBinder(tb, "person");
		binder.setRequiredFields(new String[] {"someMap[key1]", "someMap[key2]", "someMap['key3']", "someMap[key4]"});

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("someMap[key1]", "value1");
		pvs.add("someMap['key2']", "value2");
		pvs.add("someMap[key3]", "value3");

		binder.bind(pvs);

		BindingResult br = binder.getBindingResult();
		assertEquals("Wrong number of errors", 1, br.getErrorCount());
		assertEquals("required", br.getFieldError("someMap[key4]").getCode());
	}

	public void testBindingWithNestedObjectCreation() throws Exception {
		TestBean tb = new TestBean();

		DataBinder binder = new DataBinder(tb, "person");
		binder.registerCustomEditor(ITestBean.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean());
			}
		});

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("spouse", "someValue");
		pvs.add("spouse.name", "test");
		binder.bind(pvs);

		assertNotNull(tb.getSpouse());
		assertEquals("test", tb.getSpouse().getName());
	}

	public void testCustomEditorForSingleProperty() {
		TestBean tb = new TestBean();
		tb.setSpouse(new TestBean());
		DataBinder binder = new DataBinder(tb, "tb");

		binder.registerCustomEditor(String.class, "name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("prefix" + text);
			}
			@Override
			public String getAsText() {
				return ((String) getValue()).substring(6);
			}
		});

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "value");
		pvs.add("touchy", "value");
		pvs.add("spouse.name", "sue");
		binder.bind(pvs);

		binder.getBindingResult().rejectValue("name", "someCode", "someMessage");
		binder.getBindingResult().rejectValue("touchy", "someCode", "someMessage");
		binder.getBindingResult().rejectValue("spouse.name", "someCode", "someMessage");

		assertEquals("", binder.getBindingResult().getNestedPath());
		assertEquals("value", binder.getBindingResult().getFieldValue("name"));
		assertEquals("prefixvalue", binder.getBindingResult().getFieldError("name").getRejectedValue());
		assertEquals("prefixvalue", tb.getName());
		assertEquals("value", binder.getBindingResult().getFieldValue("touchy"));
		assertEquals("value", binder.getBindingResult().getFieldError("touchy").getRejectedValue());
		assertEquals("value", tb.getTouchy());

		assertTrue(binder.getBindingResult().hasFieldErrors("spouse.*"));
		assertEquals(1, binder.getBindingResult().getFieldErrorCount("spouse.*"));
		assertEquals("spouse.name", binder.getBindingResult().getFieldError("spouse.*").getField());
	}

	public void testCustomEditorForPrimitiveProperty() {
		TestBean tb = new TestBean();
		DataBinder binder = new DataBinder(tb, "tb");

		binder.registerCustomEditor(int.class, "age", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new Integer(99));
			}
			@Override
			public String getAsText() {
				return "argh";
			}
		});

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("age", "");
		binder.bind(pvs);

		assertEquals("argh", binder.getBindingResult().getFieldValue("age"));
		assertEquals(99, tb.getAge());
	}

	public void testCustomEditorForAllStringProperties() {
		TestBean tb = new TestBean();
		DataBinder binder = new DataBinder(tb, "tb");

		binder.registerCustomEditor(String.class, null, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("prefix" + text);
			}
			@Override
			public String getAsText() {
				return ((String) getValue()).substring(6);
			}
		});

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "value");
		pvs.add("touchy", "value");
		binder.bind(pvs);

		binder.getBindingResult().rejectValue("name", "someCode", "someMessage");
		binder.getBindingResult().rejectValue("touchy", "someCode", "someMessage");

		assertEquals("value", binder.getBindingResult().getFieldValue("name"));
		assertEquals("prefixvalue", binder.getBindingResult().getFieldError("name").getRejectedValue());
		assertEquals("prefixvalue", tb.getName());
		assertEquals("value", binder.getBindingResult().getFieldValue("touchy"));
		assertEquals("prefixvalue", binder.getBindingResult().getFieldError("touchy").getRejectedValue());
		assertEquals("prefixvalue", tb.getTouchy());
	}

	public void testCustomEditorWithOldValueAccess() {
		TestBean tb = new TestBean();
		DataBinder binder = new DataBinder(tb, "tb");

		binder.registerCustomEditor(String.class, null, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				if (getValue() == null || !text.equalsIgnoreCase(getValue().toString())) {
					setValue(text);
				}
			}
		});

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "value");
		binder.bind(pvs);
		assertEquals("value", tb.getName());

		pvs = new MutablePropertyValues();
		pvs.add("name", "vaLue");
		binder.bind(pvs);
		assertEquals("value", tb.getName());
	}

	public void testJavaBeanPropertyConventions() {
		Book book = new Book();
		DataBinder binder = new DataBinder(book);

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("title", "my book");
		pvs.add("ISBN", "1234");
		pvs.add("NInStock", "5");
		binder.bind(pvs);
		assertEquals("my book", book.getTitle());
		assertEquals("1234", book.getISBN());
		assertEquals(5, book.getNInStock());

		pvs = new MutablePropertyValues();
		pvs.add("Title", "my other book");
		pvs.add("iSBN", "6789");
		pvs.add("nInStock", "0");
		binder.bind(pvs);
		assertEquals("my other book", book.getTitle());
		assertEquals("6789", book.getISBN());
		assertEquals(0, book.getNInStock());
	}

	public void testValidatorNoErrors() {
		TestBean tb = new TestBean();
		tb.setAge(33);
		tb.setName("Rod");
		try {
			tb.setTouchy("Rod");
		}
		catch (Exception e) {
			fail("Should not throw any Exception");
		}
		TestBean tb2 = new TestBean();
		tb2.setAge(34);
		tb.setSpouse(tb2);
		DataBinder db = new DataBinder(tb, "tb");
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("spouse.age", "argh");
		db.bind(pvs);
		Errors errors = db.getBindingResult();
		Validator testValidator = new TestBeanValidator();
		testValidator.validate(tb, errors);

		errors.setNestedPath("spouse");
		assertEquals("spouse.", errors.getNestedPath());
		assertEquals("argh", errors.getFieldValue("age"));
		Validator spouseValidator = new SpouseValidator();
		spouseValidator.validate(tb.getSpouse(), errors);

		errors.setNestedPath("");
		assertEquals("", errors.getNestedPath());
		errors.pushNestedPath("spouse");
		assertEquals("spouse.", errors.getNestedPath());
		errors.pushNestedPath("spouse");
		assertEquals("spouse.spouse.", errors.getNestedPath());
		errors.popNestedPath();
		assertEquals("spouse.", errors.getNestedPath());
		errors.popNestedPath();
		assertEquals("", errors.getNestedPath());
		try {
			errors.popNestedPath();
		}
		catch (IllegalStateException ex) {
			// expected, because stack was empty
		}
		errors.pushNestedPath("spouse");
		assertEquals("spouse.", errors.getNestedPath());
		errors.setNestedPath("");
		assertEquals("", errors.getNestedPath());
		try {
			errors.popNestedPath();
		}
		catch (IllegalStateException ex) {
			// expected, because stack was reset by setNestedPath
		}

		errors.pushNestedPath("spouse");
		assertEquals("spouse.", errors.getNestedPath());

		assertEquals(1, errors.getErrorCount());
		assertTrue(!errors.hasGlobalErrors());
		assertEquals(1, errors.getFieldErrorCount("age"));
		assertTrue(!errors.hasFieldErrors("name"));
	}

	public void testValidatorWithErrors() {
		TestBean tb = new TestBean();
		tb.setSpouse(new TestBean());

		Errors errors = new BeanPropertyBindingResult(tb, "tb");

		Validator testValidator = new TestBeanValidator();
		testValidator.validate(tb, errors);

		errors.setNestedPath("spouse.");
		assertEquals("spouse.", errors.getNestedPath());
		Validator spouseValidator = new SpouseValidator();
		spouseValidator.validate(tb.getSpouse(), errors);

		errors.setNestedPath("");
		assertTrue(errors.hasErrors());
		assertEquals(6, errors.getErrorCount());

		assertEquals(2, errors.getGlobalErrorCount());
		assertEquals("NAME_TOUCHY_MISMATCH", errors.getGlobalError().getCode());
		assertEquals("NAME_TOUCHY_MISMATCH", (errors.getGlobalErrors().get(0)).getCode());
		assertEquals("NAME_TOUCHY_MISMATCH.tb", (errors.getGlobalErrors().get(0)).getCodes()[0]);
		assertEquals("NAME_TOUCHY_MISMATCH", (errors.getGlobalErrors().get(0)).getCodes()[1]);
		assertEquals("tb", (errors.getGlobalErrors().get(0)).getObjectName());
		assertEquals("GENERAL_ERROR", (errors.getGlobalErrors().get(1)).getCode());
		assertEquals("GENERAL_ERROR.tb", (errors.getGlobalErrors().get(1)).getCodes()[0]);
		assertEquals("GENERAL_ERROR", (errors.getGlobalErrors().get(1)).getCodes()[1]);
		assertEquals("msg", (errors.getGlobalErrors().get(1)).getDefaultMessage());
		assertEquals("arg", (errors.getGlobalErrors().get(1)).getArguments()[0]);

		assertTrue(errors.hasFieldErrors());
		assertEquals(4, errors.getFieldErrorCount());
		assertEquals("TOO_YOUNG", errors.getFieldError().getCode());
		assertEquals("TOO_YOUNG", (errors.getFieldErrors().get(0)).getCode());
		assertEquals("age", (errors.getFieldErrors().get(0)).getField());
		assertEquals("AGE_NOT_ODD", (errors.getFieldErrors().get(1)).getCode());
		assertEquals("age", (errors.getFieldErrors().get(1)).getField());
		assertEquals("NOT_ROD", (errors.getFieldErrors().get(2)).getCode());
		assertEquals("name", (errors.getFieldErrors().get(2)).getField());
		assertEquals("TOO_YOUNG", (errors.getFieldErrors().get(3)).getCode());
		assertEquals("spouse.age", (errors.getFieldErrors().get(3)).getField());

		assertTrue(errors.hasFieldErrors("age"));
		assertEquals(2, errors.getFieldErrorCount("age"));
		assertEquals("TOO_YOUNG", errors.getFieldError("age").getCode());
		assertEquals("TOO_YOUNG", (errors.getFieldErrors("age").get(0)).getCode());
		assertEquals("tb", (errors.getFieldErrors("age").get(0)).getObjectName());
		assertEquals("age", (errors.getFieldErrors("age").get(0)).getField());
		assertEquals(new Integer(0), (errors.getFieldErrors("age").get(0)).getRejectedValue());
		assertEquals("AGE_NOT_ODD", (errors.getFieldErrors("age").get(1)).getCode());

		assertTrue(errors.hasFieldErrors("name"));
		assertEquals(1, errors.getFieldErrorCount("name"));
		assertEquals("NOT_ROD", errors.getFieldError("name").getCode());
		assertEquals("NOT_ROD.tb.name", errors.getFieldError("name").getCodes()[0]);
		assertEquals("NOT_ROD.name", errors.getFieldError("name").getCodes()[1]);
		assertEquals("NOT_ROD.java.lang.String", errors.getFieldError("name").getCodes()[2]);
		assertEquals("NOT_ROD", errors.getFieldError("name").getCodes()[3]);
		assertEquals("name", (errors.getFieldErrors("name").get(0)).getField());
		assertEquals(null, (errors.getFieldErrors("name").get(0)).getRejectedValue());

		assertTrue(errors.hasFieldErrors("spouse.age"));
		assertEquals(1, errors.getFieldErrorCount("spouse.age"));
		assertEquals("TOO_YOUNG", errors.getFieldError("spouse.age").getCode());
		assertEquals("tb", (errors.getFieldErrors("spouse.age").get(0)).getObjectName());
		assertEquals(new Integer(0), (errors.getFieldErrors("spouse.age").get(0)).getRejectedValue());
	}

	public void testValidatorWithErrorsAndCodesPrefix() {
		TestBean tb = new TestBean();
		tb.setSpouse(new TestBean());

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(tb, "tb");
		DefaultMessageCodesResolver codesResolver = new DefaultMessageCodesResolver();
		codesResolver.setPrefix("validation.");
		errors.setMessageCodesResolver(codesResolver);

		Validator testValidator = new TestBeanValidator();
		testValidator.validate(tb, errors);

		errors.setNestedPath("spouse.");
		assertEquals("spouse.", errors.getNestedPath());
		Validator spouseValidator = new SpouseValidator();
		spouseValidator.validate(tb.getSpouse(), errors);

		errors.setNestedPath("");
		assertTrue(errors.hasErrors());
		assertEquals(6, errors.getErrorCount());

		assertEquals(2, errors.getGlobalErrorCount());
		assertEquals("validation.NAME_TOUCHY_MISMATCH", errors.getGlobalError().getCode());
		assertEquals("validation.NAME_TOUCHY_MISMATCH", (errors.getGlobalErrors().get(0)).getCode());
		assertEquals("validation.NAME_TOUCHY_MISMATCH.tb", (errors.getGlobalErrors().get(0)).getCodes()[0]);
		assertEquals("validation.NAME_TOUCHY_MISMATCH", (errors.getGlobalErrors().get(0)).getCodes()[1]);
		assertEquals("tb", (errors.getGlobalErrors().get(0)).getObjectName());
		assertEquals("validation.GENERAL_ERROR", (errors.getGlobalErrors().get(1)).getCode());
		assertEquals("validation.GENERAL_ERROR.tb", (errors.getGlobalErrors().get(1)).getCodes()[0]);
		assertEquals("validation.GENERAL_ERROR", (errors.getGlobalErrors().get(1)).getCodes()[1]);
		assertEquals("msg", (errors.getGlobalErrors().get(1)).getDefaultMessage());
		assertEquals("arg", (errors.getGlobalErrors().get(1)).getArguments()[0]);

		assertTrue(errors.hasFieldErrors());
		assertEquals(4, errors.getFieldErrorCount());
		assertEquals("validation.TOO_YOUNG", errors.getFieldError().getCode());
		assertEquals("validation.TOO_YOUNG", (errors.getFieldErrors().get(0)).getCode());
		assertEquals("age", (errors.getFieldErrors().get(0)).getField());
		assertEquals("validation.AGE_NOT_ODD", (errors.getFieldErrors().get(1)).getCode());
		assertEquals("age", (errors.getFieldErrors().get(1)).getField());
		assertEquals("validation.NOT_ROD", (errors.getFieldErrors().get(2)).getCode());
		assertEquals("name", (errors.getFieldErrors().get(2)).getField());
		assertEquals("validation.TOO_YOUNG", (errors.getFieldErrors().get(3)).getCode());
		assertEquals("spouse.age", (errors.getFieldErrors().get(3)).getField());

		assertTrue(errors.hasFieldErrors("age"));
		assertEquals(2, errors.getFieldErrorCount("age"));
		assertEquals("validation.TOO_YOUNG", errors.getFieldError("age").getCode());
		assertEquals("validation.TOO_YOUNG", (errors.getFieldErrors("age").get(0)).getCode());
		assertEquals("tb", (errors.getFieldErrors("age").get(0)).getObjectName());
		assertEquals("age", (errors.getFieldErrors("age").get(0)).getField());
		assertEquals(new Integer(0), (errors.getFieldErrors("age").get(0)).getRejectedValue());
		assertEquals("validation.AGE_NOT_ODD", (errors.getFieldErrors("age").get(1)).getCode());

		assertTrue(errors.hasFieldErrors("name"));
		assertEquals(1, errors.getFieldErrorCount("name"));
		assertEquals("validation.NOT_ROD", errors.getFieldError("name").getCode());
		assertEquals("validation.NOT_ROD.tb.name", errors.getFieldError("name").getCodes()[0]);
		assertEquals("validation.NOT_ROD.name", errors.getFieldError("name").getCodes()[1]);
		assertEquals("validation.NOT_ROD.java.lang.String", errors.getFieldError("name").getCodes()[2]);
		assertEquals("validation.NOT_ROD", errors.getFieldError("name").getCodes()[3]);
		assertEquals("name", (errors.getFieldErrors("name").get(0)).getField());
		assertEquals(null, (errors.getFieldErrors("name").get(0)).getRejectedValue());

		assertTrue(errors.hasFieldErrors("spouse.age"));
		assertEquals(1, errors.getFieldErrorCount("spouse.age"));
		assertEquals("validation.TOO_YOUNG", errors.getFieldError("spouse.age").getCode());
		assertEquals("tb", (errors.getFieldErrors("spouse.age").get(0)).getObjectName());
		assertEquals(new Integer(0), (errors.getFieldErrors("spouse.age").get(0)).getRejectedValue());
	}

	public void testValidatorWithNestedObjectNull() {
		TestBean tb = new TestBean();
		Errors errors = new BeanPropertyBindingResult(tb, "tb");
		Validator testValidator = new TestBeanValidator();
		testValidator.validate(tb, errors);
		errors.setNestedPath("spouse.");
		assertEquals("spouse.", errors.getNestedPath());
		Validator spouseValidator = new SpouseValidator();
		spouseValidator.validate(tb.getSpouse(), errors);
		errors.setNestedPath("");

		assertTrue(errors.hasFieldErrors("spouse"));
		assertEquals(1, errors.getFieldErrorCount("spouse"));
		assertEquals("SPOUSE_NOT_AVAILABLE", errors.getFieldError("spouse").getCode());
		assertEquals("tb", (errors.getFieldErrors("spouse").get(0)).getObjectName());
		assertEquals(null, (errors.getFieldErrors("spouse").get(0)).getRejectedValue());
	}

	public void testNestedValidatorWithoutNestedPath() {
		TestBean tb = new TestBean();
		tb.setName("XXX");
		Errors errors = new BeanPropertyBindingResult(tb, "tb");
		Validator spouseValidator = new SpouseValidator();
		spouseValidator.validate(tb, errors);

		assertTrue(errors.hasGlobalErrors());
		assertEquals(1, errors.getGlobalErrorCount());
		assertEquals("SPOUSE_NOT_AVAILABLE", errors.getGlobalError().getCode());
		assertEquals("tb", (errors.getGlobalErrors().get(0)).getObjectName());
	}

	public void testBindingStringArrayToIntegerSet() {
		IndexedTestBean tb = new IndexedTestBean();
		DataBinder binder = new DataBinder(tb, "tb");
		binder.registerCustomEditor(Set.class, new CustomCollectionEditor(TreeSet.class) {
			@Override
			protected Object convertElement(Object element) {
				return new Integer(element.toString());
			}
		});
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("set", new String[] {"10", "20", "30"});
		binder.bind(pvs);

		assertEquals(tb.getSet(), binder.getBindingResult().getFieldValue("set"));
		assertTrue(tb.getSet() instanceof TreeSet);
		assertEquals(3, tb.getSet().size());
		assertTrue(tb.getSet().contains(new Integer(10)));
		assertTrue(tb.getSet().contains(new Integer(20)));
		assertTrue(tb.getSet().contains(new Integer(30)));

		pvs = new MutablePropertyValues();
		pvs.add("set", null);
		binder.bind(pvs);

		assertNull(tb.getSet());
	}

	public void testBindingNullToEmptyCollection() {
		IndexedTestBean tb = new IndexedTestBean();
		DataBinder binder = new DataBinder(tb, "tb");
		binder.registerCustomEditor(Set.class, new CustomCollectionEditor(TreeSet.class, true));
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("set", null);
		binder.bind(pvs);

		assertTrue(tb.getSet() instanceof TreeSet);
		assertTrue(tb.getSet().isEmpty());
	}

	public void testBindingToIndexedField() {
		IndexedTestBean tb = new IndexedTestBean();
		DataBinder binder = new DataBinder(tb, "tb");
		binder.registerCustomEditor(String.class, "array.name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("array" + text);
			}
		});
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0]", "a");
		binder.bind(pvs);
		Errors errors = binder.getBindingResult();
		errors.rejectValue("array[0].name", "NOT_ROD", "are you sure you're not Rod?");
		errors.rejectValue("map[key1].name", "NOT_ROD", "are you sure you're not Rod?");

		assertEquals(1, errors.getFieldErrorCount("array[0].name"));
		assertEquals("NOT_ROD", errors.getFieldError("array[0].name").getCode());
		assertEquals("NOT_ROD.tb.array[0].name", errors.getFieldError("array[0].name").getCodes()[0]);
		assertEquals("NOT_ROD.tb.array.name", errors.getFieldError("array[0].name").getCodes()[1]);
		assertEquals("NOT_ROD.array[0].name", errors.getFieldError("array[0].name").getCodes()[2]);
		assertEquals("NOT_ROD.array.name", errors.getFieldError("array[0].name").getCodes()[3]);
		assertEquals("NOT_ROD.name", errors.getFieldError("array[0].name").getCodes()[4]);
		assertEquals("NOT_ROD.java.lang.String", errors.getFieldError("array[0].name").getCodes()[5]);
		assertEquals("NOT_ROD", errors.getFieldError("array[0].name").getCodes()[6]);
		assertEquals(1, errors.getFieldErrorCount("map[key1].name"));
		assertEquals(1, errors.getFieldErrorCount("map['key1'].name"));
		assertEquals(1, errors.getFieldErrorCount("map[\"key1\"].name"));
		assertEquals("NOT_ROD", errors.getFieldError("map[key1].name").getCode());
		assertEquals("NOT_ROD.tb.map[key1].name", errors.getFieldError("map[key1].name").getCodes()[0]);
		assertEquals("NOT_ROD.tb.map.name", errors.getFieldError("map[key1].name").getCodes()[1]);
		assertEquals("NOT_ROD.map[key1].name", errors.getFieldError("map[key1].name").getCodes()[2]);
		assertEquals("NOT_ROD.map.name", errors.getFieldError("map[key1].name").getCodes()[3]);
		assertEquals("NOT_ROD.name", errors.getFieldError("map[key1].name").getCodes()[4]);
		assertEquals("NOT_ROD.java.lang.String", errors.getFieldError("map[key1].name").getCodes()[5]);
		assertEquals("NOT_ROD", errors.getFieldError("map[key1].name").getCodes()[6]);
	}

	public void testBindingToNestedIndexedField() {
		IndexedTestBean tb = new IndexedTestBean();
		tb.getArray()[0].setNestedIndexedBean(new IndexedTestBean());
		tb.getArray()[1].setNestedIndexedBean(new IndexedTestBean());
		DataBinder binder = new DataBinder(tb, "tb");
		binder.registerCustomEditor(String.class, "array.nestedIndexedBean.list.name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("list" + text);
			}
		});
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0].nestedIndexedBean.list[0].name", "a");
		binder.bind(pvs);
		Errors errors = binder.getBindingResult();
		errors.rejectValue("array[0].nestedIndexedBean.list[0].name", "NOT_ROD", "are you sure you're not Rod?");

		assertEquals(1, errors.getFieldErrorCount("array[0].nestedIndexedBean.list[0].name"));
		assertEquals("NOT_ROD", errors.getFieldError("array[0].nestedIndexedBean.list[0].name").getCode());
		assertEquals("NOT_ROD.tb.array[0].nestedIndexedBean.list[0].name",
				errors.getFieldError("array[0].nestedIndexedBean.list[0].name").getCodes()[0]);
		assertEquals("NOT_ROD.tb.array[0].nestedIndexedBean.list.name",
				errors.getFieldError("array[0].nestedIndexedBean.list[0].name").getCodes()[1]);
		assertEquals("NOT_ROD.tb.array.nestedIndexedBean.list.name",
				errors.getFieldError("array[0].nestedIndexedBean.list[0].name").getCodes()[2]);
		assertEquals("NOT_ROD.array[0].nestedIndexedBean.list[0].name",
				errors.getFieldError("array[0].nestedIndexedBean.list[0].name").getCodes()[3]);
		assertEquals("NOT_ROD.array[0].nestedIndexedBean.list.name",
				errors.getFieldError("array[0].nestedIndexedBean.list[0].name").getCodes()[4]);
		assertEquals("NOT_ROD.array.nestedIndexedBean.list.name",
				errors.getFieldError("array[0].nestedIndexedBean.list[0].name").getCodes()[5]);
		assertEquals("NOT_ROD.name", errors.getFieldError("array[0].nestedIndexedBean.list[0].name").getCodes()[6]);
		assertEquals("NOT_ROD.java.lang.String",
				errors.getFieldError("array[0].nestedIndexedBean.list[0].name").getCodes()[7]);
		assertEquals("NOT_ROD", errors.getFieldError("array[0].nestedIndexedBean.list[0].name").getCodes()[8]);
	}

	public void testEditorForNestedIndexedField() {
		IndexedTestBean tb = new IndexedTestBean();
		tb.getArray()[0].setNestedIndexedBean(new IndexedTestBean());
		tb.getArray()[1].setNestedIndexedBean(new IndexedTestBean());
		DataBinder binder = new DataBinder(tb, "tb");
		binder.registerCustomEditor(String.class, "array.nestedIndexedBean.list.name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("list" + text);
			}
			@Override
			public String getAsText() {
				return ((String) getValue()).substring(4);
			}
		});
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0].nestedIndexedBean.list[0].name", "test1");
		pvs.add("array[1].nestedIndexedBean.list[1].name", "test2");
		binder.bind(pvs);
		assertEquals("listtest1", ((TestBean)tb.getArray()[0].getNestedIndexedBean().getList().get(0)).getName());
		assertEquals("listtest2", ((TestBean)tb.getArray()[1].getNestedIndexedBean().getList().get(1)).getName());
		assertEquals("test1", binder.getBindingResult().getFieldValue("array[0].nestedIndexedBean.list[0].name"));
		assertEquals("test2", binder.getBindingResult().getFieldValue("array[1].nestedIndexedBean.list[1].name"));
	}

	public void testSpecificEditorForNestedIndexedField() {
		IndexedTestBean tb = new IndexedTestBean();
		tb.getArray()[0].setNestedIndexedBean(new IndexedTestBean());
		tb.getArray()[1].setNestedIndexedBean(new IndexedTestBean());
		DataBinder binder = new DataBinder(tb, "tb");
		binder.registerCustomEditor(String.class, "array[0].nestedIndexedBean.list.name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("list" + text);
			}
			@Override
			public String getAsText() {
				return ((String) getValue()).substring(4);
			}
		});
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0].nestedIndexedBean.list[0].name", "test1");
		pvs.add("array[1].nestedIndexedBean.list[1].name", "test2");
		binder.bind(pvs);
		assertEquals("listtest1", ((TestBean)tb.getArray()[0].getNestedIndexedBean().getList().get(0)).getName());
		assertEquals("test2", ((TestBean)tb.getArray()[1].getNestedIndexedBean().getList().get(1)).getName());
		assertEquals("test1", binder.getBindingResult().getFieldValue("array[0].nestedIndexedBean.list[0].name"));
		assertEquals("test2", binder.getBindingResult().getFieldValue("array[1].nestedIndexedBean.list[1].name"));
	}

	public void testInnerSpecificEditorForNestedIndexedField() {
		IndexedTestBean tb = new IndexedTestBean();
		tb.getArray()[0].setNestedIndexedBean(new IndexedTestBean());
		tb.getArray()[1].setNestedIndexedBean(new IndexedTestBean());
		DataBinder binder = new DataBinder(tb, "tb");
		binder.registerCustomEditor(String.class, "array.nestedIndexedBean.list[0].name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("list" + text);
			}
			@Override
			public String getAsText() {
				return ((String) getValue()).substring(4);
			}
		});
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0].nestedIndexedBean.list[0].name", "test1");
		pvs.add("array[1].nestedIndexedBean.list[1].name", "test2");
		binder.bind(pvs);
		assertEquals("listtest1", ((TestBean)tb.getArray()[0].getNestedIndexedBean().getList().get(0)).getName());
		assertEquals("test2", ((TestBean)tb.getArray()[1].getNestedIndexedBean().getList().get(1)).getName());
		assertEquals("test1", binder.getBindingResult().getFieldValue("array[0].nestedIndexedBean.list[0].name"));
		assertEquals("test2", binder.getBindingResult().getFieldValue("array[1].nestedIndexedBean.list[1].name"));
	}

	public void testDirectBindingToIndexedField() {
		IndexedTestBean tb = new IndexedTestBean();
		DataBinder binder = new DataBinder(tb, "tb");
		binder.registerCustomEditor(TestBean.class, "array", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				DerivedTestBean tb = new DerivedTestBean();
				tb.setName("array" + text);
				setValue(tb);
			}
			@Override
			public String getAsText() {
				return ((TestBean) getValue()).getName();
			}
		});
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0]", "a");
		binder.bind(pvs);
		Errors errors = binder.getBindingResult();
		errors.rejectValue("array[0]", "NOT_ROD", "are you sure you're not Rod?");
		errors.rejectValue("map[key1]", "NOT_ROD", "are you sure you're not Rod?");
		errors.rejectValue("map[key0]", "NOT_NULL", "should not be null");

		assertEquals("arraya", errors.getFieldValue("array[0]"));
		assertEquals(1, errors.getFieldErrorCount("array[0]"));
		assertEquals("NOT_ROD", errors.getFieldError("array[0]").getCode());
		assertEquals("NOT_ROD.tb.array[0]", errors.getFieldError("array[0]").getCodes()[0]);
		assertEquals("NOT_ROD.tb.array", errors.getFieldError("array[0]").getCodes()[1]);
		assertEquals("NOT_ROD.array[0]", errors.getFieldError("array[0]").getCodes()[2]);
		assertEquals("NOT_ROD.array", errors.getFieldError("array[0]").getCodes()[3]);
		assertEquals("NOT_ROD.org.springframework.tests.sample.beans.DerivedTestBean", errors.getFieldError("array[0]").getCodes()[4]);
		assertEquals("NOT_ROD", errors.getFieldError("array[0]").getCodes()[5]);
		assertEquals("arraya", errors.getFieldValue("array[0]"));

		assertEquals(1, errors.getFieldErrorCount("map[key1]"));
		assertEquals("NOT_ROD", errors.getFieldError("map[key1]").getCode());
		assertEquals("NOT_ROD.tb.map[key1]", errors.getFieldError("map[key1]").getCodes()[0]);
		assertEquals("NOT_ROD.tb.map", errors.getFieldError("map[key1]").getCodes()[1]);
		assertEquals("NOT_ROD.map[key1]", errors.getFieldError("map[key1]").getCodes()[2]);
		assertEquals("NOT_ROD.map", errors.getFieldError("map[key1]").getCodes()[3]);
		assertEquals("NOT_ROD.org.springframework.tests.sample.beans.TestBean", errors.getFieldError("map[key1]").getCodes()[4]);
		assertEquals("NOT_ROD", errors.getFieldError("map[key1]").getCodes()[5]);

		assertEquals(1, errors.getFieldErrorCount("map[key0]"));
		assertEquals("NOT_NULL", errors.getFieldError("map[key0]").getCode());
		assertEquals("NOT_NULL.tb.map[key0]", errors.getFieldError("map[key0]").getCodes()[0]);
		assertEquals("NOT_NULL.tb.map", errors.getFieldError("map[key0]").getCodes()[1]);
		assertEquals("NOT_NULL.map[key0]", errors.getFieldError("map[key0]").getCodes()[2]);
		assertEquals("NOT_NULL.map", errors.getFieldError("map[key0]").getCodes()[3]);
		assertEquals("NOT_NULL", errors.getFieldError("map[key0]").getCodes()[4]);
	}

	public void testDirectBindingToEmptyIndexedFieldWithRegisteredSpecificEditor() {
		IndexedTestBean tb = new IndexedTestBean();
		DataBinder binder = new DataBinder(tb, "tb");
		binder.registerCustomEditor(TestBean.class, "map[key0]", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				DerivedTestBean tb = new DerivedTestBean();
				tb.setName("array" + text);
				setValue(tb);
			}
			@Override
			public String getAsText() {
				return ((TestBean) getValue()).getName();
			}
		});
		Errors errors = binder.getBindingResult();
		errors.rejectValue("map[key0]", "NOT_NULL", "should not be null");

		assertEquals(1, errors.getFieldErrorCount("map[key0]"));
		assertEquals("NOT_NULL", errors.getFieldError("map[key0]").getCode());
		assertEquals("NOT_NULL.tb.map[key0]", errors.getFieldError("map[key0]").getCodes()[0]);
		assertEquals("NOT_NULL.tb.map", errors.getFieldError("map[key0]").getCodes()[1]);
		assertEquals("NOT_NULL.map[key0]", errors.getFieldError("map[key0]").getCodes()[2]);
		assertEquals("NOT_NULL.map", errors.getFieldError("map[key0]").getCodes()[3]);
		// This next code is only generated because of the registered editor, using the
		// registered type of the editor as guess for the content type of the collection.
		assertEquals("NOT_NULL.org.springframework.tests.sample.beans.TestBean", errors.getFieldError("map[key0]").getCodes()[4]);
		assertEquals("NOT_NULL", errors.getFieldError("map[key0]").getCodes()[5]);
	}

	public void testDirectBindingToEmptyIndexedFieldWithRegisteredGenericEditor() {
		IndexedTestBean tb = new IndexedTestBean();
		DataBinder binder = new DataBinder(tb, "tb");
		binder.registerCustomEditor(TestBean.class, "map", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				DerivedTestBean tb = new DerivedTestBean();
				tb.setName("array" + text);
				setValue(tb);
			}
			@Override
			public String getAsText() {
				return ((TestBean) getValue()).getName();
			}
		});
		Errors errors = binder.getBindingResult();
		errors.rejectValue("map[key0]", "NOT_NULL", "should not be null");

		assertEquals(1, errors.getFieldErrorCount("map[key0]"));
		assertEquals("NOT_NULL", errors.getFieldError("map[key0]").getCode());
		assertEquals("NOT_NULL.tb.map[key0]", errors.getFieldError("map[key0]").getCodes()[0]);
		assertEquals("NOT_NULL.tb.map", errors.getFieldError("map[key0]").getCodes()[1]);
		assertEquals("NOT_NULL.map[key0]", errors.getFieldError("map[key0]").getCodes()[2]);
		assertEquals("NOT_NULL.map", errors.getFieldError("map[key0]").getCodes()[3]);
		// This next code is only generated because of the registered editor, using the
		// registered type of the editor as guess for the content type of the collection.
		assertEquals("NOT_NULL.org.springframework.tests.sample.beans.TestBean", errors.getFieldError("map[key0]").getCodes()[4]);
		assertEquals("NOT_NULL", errors.getFieldError("map[key0]").getCodes()[5]);
	}

	public void testCustomEditorWithSubclass() {
		IndexedTestBean tb = new IndexedTestBean();
		DataBinder binder = new DataBinder(tb, "tb");
		binder.registerCustomEditor(TestBean.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				DerivedTestBean tb = new DerivedTestBean();
				tb.setName("array" + text);
				setValue(tb);
			}
			@Override
			public String getAsText() {
				return ((TestBean) getValue()).getName();
			}
		});
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0]", "a");
		binder.bind(pvs);
		Errors errors = binder.getBindingResult();
		errors.rejectValue("array[0]", "NOT_ROD", "are you sure you're not Rod?");

		assertEquals("arraya", errors.getFieldValue("array[0]"));
		assertEquals(1, errors.getFieldErrorCount("array[0]"));
		assertEquals("NOT_ROD", errors.getFieldError("array[0]").getCode());
		assertEquals("NOT_ROD.tb.array[0]", errors.getFieldError("array[0]").getCodes()[0]);
		assertEquals("NOT_ROD.tb.array", errors.getFieldError("array[0]").getCodes()[1]);
		assertEquals("NOT_ROD.array[0]", errors.getFieldError("array[0]").getCodes()[2]);
		assertEquals("NOT_ROD.array", errors.getFieldError("array[0]").getCodes()[3]);
		assertEquals("NOT_ROD.org.springframework.tests.sample.beans.DerivedTestBean", errors.getFieldError("array[0]").getCodes()[4]);
		assertEquals("NOT_ROD", errors.getFieldError("array[0]").getCodes()[5]);
		assertEquals("arraya", errors.getFieldValue("array[0]"));
	}

	public void testBindToStringArrayWithArrayEditor() {
		TestBean tb = new TestBean();
		DataBinder binder = new DataBinder(tb, "tb");
		binder.registerCustomEditor(String[].class, "stringArray", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(StringUtils.delimitedListToStringArray(text, "-"));
			}
		});
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("stringArray", "a1-b2");
		binder.bind(pvs);
		assertTrue(!binder.getBindingResult().hasErrors());
		assertEquals(2, tb.getStringArray().length);
		assertEquals("a1", tb.getStringArray()[0]);
		assertEquals("b2", tb.getStringArray()[1]);
	}

	public void testBindToStringArrayWithComponentEditor() {
		TestBean tb = new TestBean();
		DataBinder binder = new DataBinder(tb, "tb");
		binder.registerCustomEditor(String.class, "stringArray", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("X" + text);
			}
		});
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("stringArray", new String[] {"a1", "b2"});
		binder.bind(pvs);
		assertTrue(!binder.getBindingResult().hasErrors());
		assertEquals(2, tb.getStringArray().length);
		assertEquals("Xa1", tb.getStringArray()[0]);
		assertEquals("Xb2", tb.getStringArray()[1]);
	}

	public void testBindingErrors() {
		TestBean rod = new TestBean();
		DataBinder binder = new DataBinder(rod, "person");
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("age", "32x");
		binder.bind(pvs);
		Errors errors = binder.getBindingResult();
		FieldError ageError = errors.getFieldError("age");
		assertEquals("typeMismatch", ageError.getCode());

		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename("org.springframework.validation.messages1");
		String msg = messageSource.getMessage(ageError, Locale.getDefault());
		assertEquals("Field age did not have correct type", msg);

		messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename("org.springframework.validation.messages2");
		msg = messageSource.getMessage(ageError, Locale.getDefault());
		assertEquals("Field Age did not have correct type", msg);

		messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename("org.springframework.validation.messages3");
		msg = messageSource.getMessage(ageError, Locale.getDefault());
		assertEquals("Field Person Age did not have correct type", msg);
	}

	public void testAddAllErrors() {
		TestBean rod = new TestBean();
		DataBinder binder = new DataBinder(rod, "person");
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("age", "32x");
		binder.bind(pvs);
		Errors errors = binder.getBindingResult();

		BeanPropertyBindingResult errors2 = new BeanPropertyBindingResult(rod, "person");
		errors.rejectValue("name", "badName");
		errors.addAllErrors(errors2);

		FieldError ageError = errors.getFieldError("age");
		assertEquals("typeMismatch", ageError.getCode());
		FieldError nameError = errors.getFieldError("name");
		assertEquals("badName", nameError.getCode());
	}

	public void testBindingWithResortedList() {
		IndexedTestBean tb = new IndexedTestBean();
		DataBinder binder = new DataBinder(tb, "tb");
		MutablePropertyValues pvs = new MutablePropertyValues();
		TestBean tb1 = new TestBean("tb1", 99);
		TestBean tb2 = new TestBean("tb2", 99);
		pvs.add("list[0]", tb1);
		pvs.add("list[1]", tb2);
		binder.bind(pvs);
		assertEquals(tb1.getName(), binder.getBindingResult().getFieldValue("list[0].name"));
		assertEquals(tb2.getName(), binder.getBindingResult().getFieldValue("list[1].name"));
		tb.getList().set(0, tb2);
		tb.getList().set(1, tb1);
		assertEquals(tb2.getName(), binder.getBindingResult().getFieldValue("list[0].name"));
		assertEquals(tb1.getName(), binder.getBindingResult().getFieldValue("list[1].name"));
	}

	public void testRejectWithoutDefaultMessage() throws Exception {
		TestBean tb = new TestBean();
		tb.setName("myName");
		tb.setAge(99);

		BeanPropertyBindingResult ex = new BeanPropertyBindingResult(tb, "tb");
		ex.reject("invalid");
		ex.rejectValue("age", "invalidField");

		StaticMessageSource ms = new StaticMessageSource();
		ms.addMessage("invalid", Locale.US, "general error");
		ms.addMessage("invalidField", Locale.US, "invalid field");

		assertEquals("general error", ms.getMessage(ex.getGlobalError(), Locale.US));
		assertEquals("invalid field", ms.getMessage(ex.getFieldError("age"), Locale.US));
	}

	public void testBindExceptionSerializable() throws Exception {
		SerializablePerson tb = new SerializablePerson();
		tb.setName("myName");
		tb.setAge(99);

		BindException ex = new BindException(tb, "tb");
		ex.reject("invalid", "someMessage");
		ex.rejectValue("age", "invalidField", "someMessage");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(ex);
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);

		BindException ex2 = (BindException) ois.readObject();
		assertTrue(ex2.hasGlobalErrors());
		assertEquals("invalid", ex2.getGlobalError().getCode());
		assertTrue(ex2.hasFieldErrors("age"));
		assertEquals("invalidField", ex2.getFieldError("age").getCode());
		assertEquals(new Integer(99), ex2.getFieldValue("age"));

		ex2.rejectValue("name", "invalidField", "someMessage");
		assertTrue(ex2.hasFieldErrors("name"));
		assertEquals("invalidField", ex2.getFieldError("name").getCode());
		assertEquals("myName", ex2.getFieldValue("name"));
	}

	public void testTrackDisallowedFields() throws Exception {
		TestBean testBean = new TestBean();
		DataBinder binder = new DataBinder(testBean, "testBean");
		binder.setAllowedFields(new String[] {"name", "age"});

		String name = "Rob Harrop";
		String beanName = "foobar";

		MutablePropertyValues mpvs = new MutablePropertyValues();
		mpvs.add("name", name);
		mpvs.add("beanName", beanName);
		binder.bind(mpvs);

		assertEquals(name, testBean.getName());
		String[] disallowedFields = binder.getBindingResult().getSuppressedFields();
		assertEquals(1, disallowedFields.length);
		assertEquals("beanName", disallowedFields[0]);
	}

	public void testAutoGrowWithinDefaultLimit() throws Exception {
		TestBean testBean = new TestBean();
		DataBinder binder = new DataBinder(testBean, "testBean");

		MutablePropertyValues mpvs = new MutablePropertyValues();
		mpvs.add("friends[4]", "");
		binder.bind(mpvs);

		assertEquals(5, testBean.getFriends().size());
	}

	public void testAutoGrowBeyondDefaultLimit() throws Exception {
		TestBean testBean = new TestBean();
		DataBinder binder = new DataBinder(testBean, "testBean");

		MutablePropertyValues mpvs = new MutablePropertyValues();
		mpvs.add("friends[256]", "");
		try {
			binder.bind(mpvs);
			fail("Should have thrown InvalidPropertyException");
		}
		catch (InvalidPropertyException ex) {
			// expected
			assertTrue(ex.getRootCause() instanceof IndexOutOfBoundsException);
		}
	}

	public void testAutoGrowWithinCustomLimit() throws Exception {
		TestBean testBean = new TestBean();
		DataBinder binder = new DataBinder(testBean, "testBean");
		binder.setAutoGrowCollectionLimit(10);

		MutablePropertyValues mpvs = new MutablePropertyValues();
		mpvs.add("friends[4]", "");
		binder.bind(mpvs);

		assertEquals(5, testBean.getFriends().size());
	}

	public void testAutoGrowBeyondCustomLimit() throws Exception {
		TestBean testBean = new TestBean();
		DataBinder binder = new DataBinder(testBean, "testBean");
		binder.setAutoGrowCollectionLimit(10);

		MutablePropertyValues mpvs = new MutablePropertyValues();
		mpvs.add("friends[16]", "");
		try {
			binder.bind(mpvs);
			fail("Should have thrown InvalidPropertyException");
		}
		catch (InvalidPropertyException ex) {
			// expected
			assertTrue(ex.getRootCause() instanceof IndexOutOfBoundsException);
		}
	}

	public void testNestedGrowingList() {
		Form form = new Form();
		DataBinder binder = new DataBinder(form, "form");
		MutablePropertyValues mpv = new MutablePropertyValues();
		mpv.add("f[list][0]", "firstValue");
		mpv.add("f[list][1]", "secondValue");
		binder.bind(mpv);
		assertFalse(binder.getBindingResult().hasErrors());
		@SuppressWarnings("unchecked")
		List<Object> list = (List<Object>) form.getF().get("list");
		assertEquals("firstValue", list.get(0));
		assertEquals("secondValue", list.get(1));
		assertEquals(2, list.size());
	  }


	@SuppressWarnings("unused")
	private static class BeanWithIntegerList {

		private List<Integer> integerList;

		public List<Integer> getIntegerList() {
			return integerList;
		}

		public void setIntegerList(List<Integer> integerList) {
			this.integerList = integerList;
		}
	}


	@SuppressWarnings("unused")
	private static class Book {

		private String Title;

		private String ISBN;

		private int nInStock;

		public String getTitle() {
			return Title;
		}

		public void setTitle(String title) {
			Title = title;
		}

		public String getISBN() {
			return ISBN;
		}

		public void setISBN(String ISBN) {
			this.ISBN = ISBN;
		}

		public int getNInStock() {
			return nInStock;
		}

		public void setNInStock(int nInStock) {
			this.nInStock = nInStock;
		}
	}


	private static class TestBeanValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return TestBean.class.isAssignableFrom(clazz);
		}

		@Override
		public void validate(Object obj, Errors errors) {
			TestBean tb = (TestBean) obj;
			if (tb.getAge() < 32) {
				errors.rejectValue("age", "TOO_YOUNG", "simply too young");
			}
			if (tb.getAge() % 2 == 0) {
				errors.rejectValue("age", "AGE_NOT_ODD", "your age isn't odd");
			}
			if (tb.getName() == null || !tb.getName().equals("Rod")) {
				errors.rejectValue("name", "NOT_ROD", "are you sure you're not Rod?");
			}
			if (tb.getTouchy() == null || !tb.getTouchy().equals(tb.getName())) {
				errors.reject("NAME_TOUCHY_MISMATCH", "name and touchy do not match");
			}
			if (tb.getAge() == 0) {
				errors.reject("GENERAL_ERROR", new String[] {"arg"}, "msg");
			}
		}
	}


	private static class SpouseValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return TestBean.class.isAssignableFrom(clazz);
		}

		@Override
		public void validate(Object obj, Errors errors) {
			TestBean tb = (TestBean) obj;
			if (tb == null || "XXX".equals(tb.getName())) {
				errors.rejectValue("", "SPOUSE_NOT_AVAILABLE");
				return;
			}
			if (tb.getAge() < 32) {
				errors.rejectValue("age", "TOO_YOUNG", "simply too young");
			}
		}
	}


	@SuppressWarnings("unused")
	private static class GrowingList<E> extends AbstractList<E> {

		private List<E> list;

		public GrowingList() {
			this.list = new ArrayList<E>();
		}

		public List<E> getWrappedList() {
			return list;
		}

		@Override
		public E get(int index) {
			if (index >= list.size()) {
				for (int i = list.size(); i < index; i++) {
					list.add(null);
				}
				list.add(null);
				return null;
			}
			else {
				return list.get(index);
			}
		}

		@Override
		public int size() {
			return list.size();
		}

		@Override
		public boolean add(E o) {
			return list.add(o);
		}

		@Override
		public void add(int index, E element) {
			list.add(index, element);
		}

		@Override
		public boolean addAll(int index, Collection<? extends E> c) {
			return list.addAll(index, c);
		}

		@Override
		public void clear() {
			list.clear();
		}

		@Override
		public int indexOf(Object o) {
			return list.indexOf(o);
		}

		@Override
		public Iterator<E> iterator() {
			return list.iterator();
		}

		@Override
		public int lastIndexOf(Object o) {
			return list.lastIndexOf(o);
		}

		@Override
		public ListIterator<E> listIterator() {
			return list.listIterator();
		}

		@Override
		public ListIterator<E> listIterator(int index) {
			return list.listIterator(index);
		}

		@Override
		public E remove(int index) {
			return list.remove(index);
		}

		@Override
		public E set(int index, E element) {
			return list.set(index, element);
		}
	}


	private static class Form {

		private final Map<String, Object> f;

		public Form() {
			f = new HashMap<String, Object>();
			f.put("list", new GrowingList<Object>());
		}

		public Map<String, Object> getF() {
			return f;
		}
	}

}
