/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.servlet.tags.form;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import org.junit.Test;

import org.springframework.beans.propertyeditors.CustomCollectionEditor;
import org.springframework.format.Formatter;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.tags.TransformTag;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Jeremy Grelle
 * @author Dave Syer
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SelectTagTests extends AbstractFormTagTests {

	private static final Locale LOCALE_AT = new Locale("de", "AT");
	private static final Locale LOCALE_NL = new Locale("nl", "NL");

	private SelectTag tag;

	private TestBeanWithRealCountry bean;


	@Override
	@SuppressWarnings("serial")
	protected void onSetUp() {
		this.tag = new SelectTag() {
			@Override
			protected TagWriter createTagWriter() {
				return new TagWriter(getWriter());
			}
		};
		this.tag.setPageContext(getPageContext());
	}

	@Test
	public void dynamicAttributes() throws JspException {
		String dynamicAttribute1 = "attr1";
		String dynamicAttribute2 = "attr2";

		this.tag.setPath("country");
		this.tag.setItems(Collections.EMPTY_LIST);
		this.tag.setItemValue("isoCode");
		this.tag.setItemLabel("name");
		this.tag.setDynamicAttribute(null, dynamicAttribute1, dynamicAttribute1);
		this.tag.setDynamicAttribute(null, dynamicAttribute2, dynamicAttribute2);

		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();
		assertContainsAttribute(output, dynamicAttribute1, dynamicAttribute1);
		assertContainsAttribute(output, dynamicAttribute2, dynamicAttribute2);
	}

	@Test
	public void emptyItems() throws Exception {
		this.tag.setPath("country");
		this.tag.setItems(Collections.EMPTY_LIST);

		this.tag.setItemValue("isoCode");
		this.tag.setItemLabel("name");
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();
		assertEquals("<select id=\"country\" name=\"country\"></select>", output);
	}

	@Test
	public void nullItems() throws Exception {
		this.tag.setPath("country");
		this.tag.setItems(null);

		this.tag.setItemValue("isoCode");
		this.tag.setItemLabel("name");
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();
		assertEquals("<select id=\"country\" name=\"country\"></select>", output);
	}

	@Test
	public void withList() throws Exception {
		this.tag.setPath("country");
		this.tag.setItems(Country.getCountries());
		assertList(true);
	}

	@Test
	public void withResolvedList() throws Exception {
		this.tag.setPath("country");
		this.tag.setItems(Country.getCountries());
		assertList(true);
	}

	@Test
	public void withOtherValue() throws Exception {
		TestBean tb = getTestBean();
		tb.setCountry("AT");
		this.tag.setPath("country");
		this.tag.setItems(Country.getCountries());
		assertList(false);
	}

	@Test
	public void withNullValue() throws Exception {
		TestBean tb = getTestBean();
		tb.setCountry(null);
		this.tag.setPath("country");
		this.tag.setItems(Country.getCountries());
		assertList(false);
	}

	@Test
	public void withListAndNoLabel() throws Exception {
		this.tag.setPath("country");
		this.tag.setItems(Country.getCountries());
		this.tag.setItemValue("isoCode");
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);
		validateOutput(getOutput(), true);
	}

	@Test
	public void withListAndTransformTag() throws Exception {
		this.tag.setPath("country");
		this.tag.setItems(Country.getCountries());
		assertList(true);

		TransformTag transformTag = new TransformTag();
		transformTag.setValue(Country.getCountries().get(0));
		transformTag.setVar("key");
		transformTag.setParent(this.tag);
		transformTag.setPageContext(getPageContext());
		transformTag.doStartTag();
		assertEquals("Austria(AT)", getPageContext().findAttribute("key"));
	}

	@Test
	public void withListAndTransformTagAndEditor() throws Exception {
		this.tag.setPath("realCountry");
		this.tag.setItems(Country.getCountries());
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(getTestBean(), "testBean");
		bindingResult.getPropertyAccessor().registerCustomEditor(Country.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(Country.getCountryWithIsoCode(text));
			}
			@Override
			public String getAsText() {
				return ((Country) getValue()).getName();
			}
		});
		getPageContext().getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "testBean", bindingResult);
		this.tag.doStartTag();

		TransformTag transformTag = new TransformTag();
		transformTag.setValue(Country.getCountries().get(0));
		transformTag.setVar("key");
		transformTag.setParent(this.tag);
		transformTag.setPageContext(getPageContext());
		transformTag.doStartTag();
		assertEquals("Austria", getPageContext().findAttribute("key"));
	}

	@Test
	public void withListAndEditor() throws Exception {
		this.tag.setPath("realCountry");
		this.tag.setItems(Country.getCountries());
		this.tag.setItemValue("isoCode");
		this.tag.setItemLabel("name");
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(getTestBean(), "testBean");
		bindingResult.getPropertyAccessor().registerCustomEditor(Country.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(Country.getCountryWithIsoCode(text));
			}
			@Override
			public String getAsText() {
				return ((Country) getValue()).getName();
			}
		});
		getPageContext().getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "testBean", bindingResult);
		this.tag.doStartTag();
		String output = getOutput();
		assertTrue(output.startsWith("<select "));
		assertTrue(output.endsWith("</select>"));
		assertTrue(output.contains("option value=\"AT\" selected=\"selected\">Austria"));
	}

	@Test
	public void nestedPathWithListAndEditorAndNullValue() throws Exception {
		this.tag.setPath("bean.realCountry");
		this.tag.setItems(Country.getCountries());
		this.tag.setItemValue("isoCode");
		this.tag.setItemLabel("name");
		this.tag.setMultiple("false");
		TestBeanWrapper testBean = new TestBeanWrapper();
		TestBeanWithRealCountry withCountry = (TestBeanWithRealCountry) getTestBean();
		withCountry.setRealCountry(null);
		testBean.setBean(withCountry);
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(testBean , "testBean");
		bindingResult.getPropertyAccessor().registerCustomEditor(Country.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				if (text==null || text.length()==0) {
					setValue(null);
					return;
				}
				setValue(Country.getCountryWithIsoCode(text));
			}
			@Override
			public String getAsText() {
				Country value = (Country) getValue();
				if (value==null) {
					return null;
				}
				return value.getName();
			}
		});
		getPageContext().getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "testBean", bindingResult);
		this.tag.doStartTag();
		String output = getOutput();
		assertTrue(output.startsWith("<select "));
		assertTrue(output.endsWith("</select>"));
		assertFalse(output.contains("selected=\"selected\""));
		assertFalse(output.contains("multiple=\"multiple\""));
	}

	@Test
	public void nestedPathWithListAndEditor() throws Exception {
		this.tag.setPath("bean.realCountry");
		this.tag.setItems(Country.getCountries());
		this.tag.setItemValue("isoCode");
		this.tag.setItemLabel("name");
		TestBeanWrapper testBean = new TestBeanWrapper();
		testBean.setBean(getTestBean());
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(testBean , "testBean");
		bindingResult.getPropertyAccessor().registerCustomEditor(Country.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(Country.getCountryWithIsoCode(text));
			}
			@Override
			public String getAsText() {
				return ((Country) getValue()).getName();
			}
		});
		getPageContext().getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "testBean", bindingResult);
		this.tag.doStartTag();
		String output = getOutput();
		assertTrue(output.startsWith("<select "));
		assertTrue(output.endsWith("</select>"));
		assertTrue(output.contains("option value=\"AT\" selected=\"selected\">Austria"));
	}

	@Test
	public void withListAndEditorAndNullValue() throws Exception {
		this.tag.setPath("realCountry");
		this.tag.setItems(Country.getCountries());
		this.tag.setItemValue("isoCode");
		this.tag.setItemLabel("name");
		TestBeanWithRealCountry testBean = (TestBeanWithRealCountry) getTestBean();
		testBean.setRealCountry(null);
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(testBean, "testBean");
		bindingResult.getPropertyAccessor().registerCustomEditor(Country.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(Country.getCountryWithIsoCode(text));
			}
			@Override
			public String getAsText() {
				Country value = (Country) getValue();
				if (value==null) {
					return "";
				}
				return value.getName();
			}
		});
		getPageContext().getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + "testBean", bindingResult);
		this.tag.doStartTag();
		String output = getOutput();
		assertTrue(output.startsWith("<select "));
		assertTrue(output.endsWith("</select>"));
		assertFalse(output.contains("selected=\"selected\""));
	}

	@Test
	public void withMap() throws Exception {
		this.tag.setPath("sex");
		this.tag.setItems(getSexes());
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);
	}

	@Test
	public void withInvalidList() throws Exception {
		this.tag.setPath("country");
		this.tag.setItems(new TestBean());
		this.tag.setItemValue("isoCode");
		try {
			this.tag.doStartTag();
			fail("Must not be able to use a non-Collection typed value as the value of 'items'");
		}
		catch (JspException expected) {
			String message = expected.getMessage();
			assertTrue(message.contains("items"));
			assertTrue(message.contains("org.springframework.tests.sample.beans.TestBean"));
		}
	}

	@Test
	public void withNestedOptions() throws Exception {
		this.tag.setPath("country");
		int result = this.tag.doStartTag();
		assertEquals(Tag.EVAL_BODY_INCLUDE, result);

		BindStatus value = (BindStatus) getPageContext().getAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE);
		assertEquals("Selected country not exposed in page context", "UK", value.getValue());

		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);
		this.tag.doFinally();

		String output = getOutput();
		assertTrue(output.startsWith("<select "));
		assertTrue(output.endsWith("</select>"));
		assertContainsAttribute(output, "name", "country");
	}

	@Test
	public void withStringArray() throws Exception {
		this.tag.setPath("name");
		this.tag.setItems(getNames());
		assertStringArray();
	}

	@Test
	public void withResolvedStringArray() throws Exception {
		this.tag.setPath("name");
		this.tag.setItems(getNames());
		assertStringArray();
	}

	@Test
	public void withIntegerArray() throws Exception {
		this.tag.setPath("someIntegerArray");
		Integer[] array = new Integer[50];
		for (int i = 0; i < array.length; i++) {
			array[i] = new Integer(i);
		}
		this.tag.setItems(array);
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertEquals(2, rootElement.elements().size());

		Element selectElement = rootElement.element("select");
		assertEquals("select", selectElement.getName());
		assertEquals("someIntegerArray", selectElement.attribute("name").getValue());

		List children = selectElement.elements();
		assertEquals("Incorrect number of children", array.length, children.size());

		Element e = (Element) selectElement.selectSingleNode("option[text() = '12']");
		assertEquals("'12' node not selected", "selected", e.attribute("selected").getValue());

		e = (Element) selectElement.selectSingleNode("option[text() = '34']");
		assertEquals("'34' node not selected", "selected", e.attribute("selected").getValue());
	}

	@Test
	public void withFloatCustom() throws Exception {
		PropertyEditor propertyEditor = new SimpleFloatEditor();
		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(getTestBean(), COMMAND_NAME);
		errors.getPropertyAccessor().registerCustomEditor(Float.class, propertyEditor);
		exposeBindingResult(errors);

		this.tag.setPath("myFloat");

		Float[] array = new Float[] {
				new Float("12.30"), new Float("12.32"), new Float("12.34"), new Float("12.36"),
				new Float("12.38"), new Float("12.40"), new Float("12.42"), new Float("12.44"),
				new Float("12.46"), new Float("12.48")
		};

		this.tag.setItems(array);
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();
		assertTrue(output.startsWith("<select "));
		assertTrue(output.endsWith("</select>"));

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertEquals("select", rootElement.getName());
		assertEquals("myFloat", rootElement.attribute("name").getValue());
		List children = rootElement.elements();
		assertEquals("Incorrect number of children", array.length, children.size());

		Element e = (Element) rootElement.selectSingleNode("option[text() = '12.34f']");
		assertEquals("'12.34' node not selected", "selected", e.attribute("selected").getValue());

		e = (Element) rootElement.selectSingleNode("option[text() = '12.32f']");
		assertNull("'12.32' node incorrectly selected", e.attribute("selected"));
	}

	@Test
	public void withMultiList() throws Exception {
		List list = new ArrayList();
		list.add(Country.COUNTRY_UK);
		list.add(Country.COUNTRY_AT);
		this.bean.setSomeList(list);

		this.tag.setPath("someList");
		this.tag.setItems(Country.getCountries());
		this.tag.setItemValue("isoCode");
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertEquals(2, rootElement.elements().size());

		Element selectElement = rootElement.element("select");
		assertEquals("select", selectElement.getName());
		assertEquals("someList", selectElement.attribute("name").getValue());

		List children = selectElement.elements();
		assertEquals("Incorrect number of children", 4, children.size());

		Element e = (Element) selectElement.selectSingleNode("option[@value = 'UK']");
		assertEquals("UK node not selected", "selected", e.attribute("selected").getValue());
		assertEquals("United Kingdom(UK)", e.getText());

		e = (Element) selectElement.selectSingleNode("option[@value = 'AT']");
		assertEquals("AT node not selected", "selected", e.attribute("selected").getValue());
		assertEquals("Austria(AT)", e.getText());
	}

	@Test
	public void withElementFormatter() throws Exception {
		this.bean.setRealCountry(Country.COUNTRY_UK);

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(this.bean, COMMAND_NAME);
		FormattingConversionService cs = new FormattingConversionService();
		cs.addFormatterForFieldType(Country.class, new Formatter<Country>() {
			@Override
			public String print(Country object, Locale locale) {
				return object.getName();
			}
			@Override
			public Country parse(String text, Locale locale) throws ParseException {
				return new Country(text, text);
			}
		});
		errors.initConversion(cs);
		exposeBindingResult(errors);

		this.tag.setPath("realCountry");
		this.tag.setItems(Country.getCountries());
		this.tag.setItemValue("isoCode");
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertEquals(1, rootElement.elements().size());

		Element selectElement = rootElement.element("select");
		assertEquals("select", selectElement.getName());
		assertEquals("realCountry", selectElement.attribute("name").getValue());

		List children = selectElement.elements();
		assertEquals("Incorrect number of children", 4, children.size());

		Element e = (Element) selectElement.selectSingleNode("option[@value = 'UK']");
		assertEquals("UK node not selected", "selected", e.attribute("selected").getValue());
		assertEquals("United Kingdom", e.getText());
	}

	@Test
	public void withMultiListAndElementFormatter() throws Exception {
		List list = new ArrayList();
		list.add(Country.COUNTRY_UK);
		list.add(Country.COUNTRY_AT);
		this.bean.setSomeList(list);

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(this.bean, COMMAND_NAME);
		FormattingConversionService cs = new FormattingConversionService();
		cs.addFormatterForFieldType(Country.class, new Formatter<Country>() {
			@Override
			public String print(Country object, Locale locale) {
				return object.getName();
			}
			@Override
			public Country parse(String text, Locale locale) throws ParseException {
				return new Country(text, text);
			}
		});
		errors.initConversion(cs);
		exposeBindingResult(errors);

		this.tag.setPath("someList");
		this.tag.setItems(Country.getCountries());
		this.tag.setItemValue("isoCode");
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertEquals(2, rootElement.elements().size());

		Element selectElement = rootElement.element("select");
		assertEquals("select", selectElement.getName());
		assertEquals("someList", selectElement.attribute("name").getValue());

		List children = selectElement.elements();
		assertEquals("Incorrect number of children", 4, children.size());

		Element e = (Element) selectElement.selectSingleNode("option[@value = 'UK']");
		assertEquals("UK node not selected", "selected", e.attribute("selected").getValue());
		assertEquals("United Kingdom", e.getText());

		e = (Element) selectElement.selectSingleNode("option[@value = 'AT']");
		assertEquals("AT node not selected", "selected", e.attribute("selected").getValue());
		assertEquals("Austria", e.getText());
	}

	@Test
	public void withMultiListAndCustomEditor() throws Exception {
		List list = new ArrayList();
		list.add(Country.COUNTRY_UK);
		list.add(Country.COUNTRY_AT);
		this.bean.setSomeList(list);

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(this.bean, COMMAND_NAME);
		errors.getPropertyAccessor().registerCustomEditor(List.class, new CustomCollectionEditor(LinkedList.class) {
			@Override
			public String getAsText() {
				return getValue().toString();
			}
		});
		exposeBindingResult(errors);

		this.tag.setPath("someList");
		this.tag.setItems(Country.getCountries());
		this.tag.setItemValue("isoCode");
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertEquals(2, rootElement.elements().size());

		Element selectElement = rootElement.element("select");
		assertEquals("select", selectElement.getName());
		assertEquals("someList", selectElement.attribute("name").getValue());

		List children = selectElement.elements();
		assertEquals("Incorrect number of children", 4, children.size());

		Element e = (Element) selectElement.selectSingleNode("option[@value = 'UK']");
		assertEquals("UK node not selected", "selected", e.attribute("selected").getValue());

		e = (Element) selectElement.selectSingleNode("option[@value = 'AT']");
		assertEquals("AT node not selected", "selected", e.attribute("selected").getValue());
	}

	@Test
	public void withMultiMap() throws Exception {
		Map someMap = new HashMap();
		someMap.put("M", "Male");
		someMap.put("F", "Female");
		this.bean.setSomeMap(someMap);

		this.tag.setPath("someMap");
		this.tag.setItems(getSexes());

		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertEquals(2, rootElement.elements().size());

		Element selectElement = rootElement.element("select");
		assertEquals("select", selectElement.getName());
		assertEquals("someMap", selectElement.attribute("name").getValue());

		List children = selectElement.elements();
		assertEquals("Incorrect number of children", 2, children.size());

		Element e = (Element) selectElement.selectSingleNode("option[@value = 'M']");
		assertEquals("M node not selected", "selected", e.attribute("selected").getValue());

		e = (Element) selectElement.selectSingleNode("option[@value = 'F']");
		assertEquals("F node not selected", "selected", e.attribute("selected").getValue());
	}

	/**
	 * Tests new support added as a result of <a
	 * href="http://opensource.atlassian.com/projects/spring/browse/SPR-2660"
	 * target="_blank">SPR-2660</a>.
	 * <p>
	 * Specifically, if the {@code items} attribute is supplied a
	 * {@link Map}, and {@code itemValue} and {@code itemLabel}
	 * are supplied non-null values, then:
	 * </p>
	 * <ul>
	 * <li>{@code itemValue} will be used as the property name of the
	 * map's <em>key</em>, and</li>
	 * <li>{@code itemLabel} will be used as the property name of the
	 * map's <em>value</em>.</li>
	 * </ul>
	 */
	@Test
	public void withMultiMapWithItemValueAndItemLabel() throws Exception {
		// Save original default locale.
		final Locale defaultLocale = Locale.getDefault();
		// Use a locale that doesn't result in the generation of HTML entities
		// (e.g., not German, where \u00e4 becomes &auml;)
		Locale.setDefault(Locale.US);

		try {
			final Country austria = Country.COUNTRY_AT;
			final Country usa = Country.COUNTRY_US;
			final Map someMap = new HashMap();
			someMap.put(austria, LOCALE_AT);
			someMap.put(usa, Locale.US);
			this.bean.setSomeMap(someMap);

			this.tag.setPath("someMap"); // see: TestBean
			this.tag.setItems(getCountryToLocaleMap());
			this.tag.setItemValue("isoCode"); // Map key: Country
			this.tag.setItemLabel("displayLanguage"); // Map value: Locale

			BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(getTestBean(), COMMAND_NAME);
			bindingResult.getPropertyAccessor().registerCustomEditor(Country.class, new PropertyEditorSupport() {
				@Override
				public void setAsText(final String text) throws IllegalArgumentException {
					setValue(Country.getCountryWithIsoCode(text));
				}

				@Override
				public String getAsText() {
					return ((Country) getValue()).getIsoCode();
				}
			});
			exposeBindingResult(bindingResult);

			int result = this.tag.doStartTag();
			assertEquals(Tag.SKIP_BODY, result);

			String output = getOutput();
			output = "<doc>" + output + "</doc>";

			SAXReader reader = new SAXReader();
			Document document = reader.read(new StringReader(output));
			Element rootElement = document.getRootElement();
			assertEquals(2, rootElement.elements().size());

			Element selectElement = rootElement.element("select");
			assertEquals("select", selectElement.getName());
			assertEquals("someMap", selectElement.attribute("name").getValue());

			List children = selectElement.elements();
			assertEquals("Incorrect number of children", 3, children.size());

			Element e;
			e = (Element) selectElement.selectSingleNode("option[@value = '" + austria.getIsoCode() + "']");
			assertNotNull("Option node not found with Country ISO code value [" + austria.getIsoCode() + "].", e);
			assertEquals("AT node not selected.", "selected", e.attribute("selected").getValue());
			assertEquals("AT Locale displayLanguage property not used for option label.",
					LOCALE_AT.getDisplayLanguage(), e.getData());

			e = (Element) selectElement.selectSingleNode("option[@value = '" + usa.getIsoCode() + "']");
			assertNotNull("Option node not found with Country ISO code value [" + usa.getIsoCode() + "].", e);
			assertEquals("US node not selected.", "selected", e.attribute("selected").getValue());
			assertEquals("US Locale displayLanguage property not used for option label.",
					Locale.US.getDisplayLanguage(), e.getData());

		}
		finally {
			// Restore original default locale.
			Locale.setDefault(defaultLocale);
		}
	}

	@Test
	public void multipleForCollection() throws Exception {
		this.bean.setSomeList(new ArrayList());

		this.tag.setPath("someList");
		this.tag.setItems(Country.getCountries());
		this.tag.setItemValue("isoCode");
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertEquals(2, rootElement.elements().size());

		Element selectElement = rootElement.element("select");
		assertEquals("select", selectElement.getName());
		assertEquals("someList", selectElement.attribute("name").getValue());
		assertEquals("multiple", selectElement.attribute("multiple").getValue());

		List children = selectElement.elements();
		assertEquals("Incorrect number of children", 4, children.size());

		Element inputElement = rootElement.element("input");
		assertNotNull(inputElement);
	}

	@Test
	public void multipleWithStringValue() throws Exception {
		this.tag.setPath("name");
		this.tag.setItems(Country.getCountries());
		this.tag.setItemValue("isoCode");
		this.tag.setMultiple("multiple");
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertEquals(2, rootElement.elements().size());

		Element selectElement = rootElement.element("select");
		assertEquals("select", selectElement.getName());
		assertEquals("name", selectElement.attribute("name").getValue());
		assertEquals("multiple", selectElement.attribute("multiple").getValue());

		List children = selectElement.elements();
		assertEquals("Incorrect number of children", 4, children.size());

		Element inputElement = rootElement.element("input");
		assertNotNull(inputElement);
	}

	@Test
	public void multipleExplicitlyTrue() throws Exception {
		this.tag.setPath("name");
		this.tag.setItems(Country.getCountries());
		this.tag.setItemValue("isoCode");
		this.tag.setMultiple("true");
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertEquals(2, rootElement.elements().size());

		Element selectElement = rootElement.element("select");
		assertEquals("select", selectElement.getName());
		assertEquals("name", selectElement.attribute("name").getValue());
		assertEquals("multiple", selectElement.attribute("multiple").getValue());

		List children = selectElement.elements();
		assertEquals("Incorrect number of children", 4, children.size());

		Element inputElement = rootElement.element("input");
		assertNotNull(inputElement);
	}

	@Test
	public void multipleExplicitlyFalse() throws Exception {
		this.tag.setPath("name");
		this.tag.setItems(Country.getCountries());
		this.tag.setItemValue("isoCode");
		this.tag.setMultiple("false");
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertEquals(1, rootElement.elements().size());

		Element selectElement = rootElement.element("select");
		assertEquals("select", selectElement.getName());
		assertEquals("name", selectElement.attribute("name").getValue());
		assertNull(selectElement.attribute("multiple"));

		List children = selectElement.elements();
		assertEquals("Incorrect number of children", 4, children.size());
	}

	@Test
	public void multipleWithBooleanTrue() throws Exception {
		this.tag.setPath("name");
		this.tag.setItems(Country.getCountries());
		this.tag.setItemValue("isoCode");
		this.tag.setMultiple(true);
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertEquals(2, rootElement.elements().size());

		Element selectElement = rootElement.element("select");
		assertEquals("select", selectElement.getName());
		assertEquals("name", selectElement.attribute("name").getValue());
		assertEquals("multiple", selectElement.attribute("multiple").getValue());

		List children = selectElement.elements();
		assertEquals("Incorrect number of children", 4, children.size());

		Element inputElement = rootElement.element("input");
		assertNotNull(inputElement);
	}

	@Test
	public void multipleWithBooleanFalse() throws Exception {
		this.tag.setPath("name");
		this.tag.setItems(Country.getCountries());
		this.tag.setItemValue("isoCode");
		this.tag.setMultiple(false);
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertEquals(1, rootElement.elements().size());

		Element selectElement = rootElement.element("select");
		assertEquals("select", selectElement.getName());
		assertEquals("name", selectElement.attribute("name").getValue());
		assertNull(selectElement.attribute("multiple"));

		List children = selectElement.elements();
		assertEquals("Incorrect number of children", 4, children.size());
	}


	private void assertStringArray() throws JspException, DocumentException {
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();
		assertTrue(output.startsWith("<select "));
		assertTrue(output.endsWith("</select>"));

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertEquals("select", rootElement.getName());
		assertEquals("name", rootElement.attribute("name").getValue());

		List children = rootElement.elements();
		assertEquals("Incorrect number of children", 4, children.size());

		Element e = (Element) rootElement.selectSingleNode("option[text() = 'Rob']");
		assertEquals("Rob node not selected", "selected", e.attribute("selected").getValue());
	}

	private Map getCountryToLocaleMap() {
		Map map = new TreeMap(new Comparator() {
			@Override
			public int compare(Object o1, Object o2) {
				return ((Country)o1).getName().compareTo(((Country)o2).getName());
			}
		});
		map.put(Country.COUNTRY_AT, LOCALE_AT);
		map.put(Country.COUNTRY_NL, LOCALE_NL);
		map.put(Country.COUNTRY_US, Locale.US);
		return map;
	}

	private String[] getNames() {
		return new String[]{"Rod", "Rob", "Juergen", "Adrian"};
	}

	private Map getSexes() {
		Map<String, String> sexes = new HashMap<>();
		sexes.put("F", "Female");
		sexes.put("M", "Male");
		return sexes;
	}

	private void assertList(boolean selected) throws JspException, DocumentException {
		this.tag.setItemValue("isoCode");
		this.tag.setItemLabel("name");
		this.tag.setSize("5");
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();
		validateOutput(output, selected);
		assertContainsAttribute(output, "size", "5");
	}

	private void validateOutput(String output, boolean selected) throws DocumentException {
		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertEquals("select", rootElement.getName());
		assertEquals("country", rootElement.attribute("name").getValue());

		List children = rootElement.elements();
		assertEquals("Incorrect number of children", 4, children.size());

		Element e = (Element) rootElement.selectSingleNode("option[@value = 'UK']");
		Attribute selectedAttr = e.attribute("selected");
		if (selected) {
			assertTrue(selectedAttr != null && "selected".equals(selectedAttr.getValue()));
		}
		else {
			assertNull(selectedAttr);
		}
	}

	@Override
	protected TestBean createTestBean() {
		this.bean = new TestBeanWithRealCountry();
		this.bean.setName("Rob");
		this.bean.setCountry("UK");
		this.bean.setSex("M");
		this.bean.setMyFloat(new Float("12.34"));
		this.bean.setSomeIntegerArray(new Integer[]{new Integer(12), new Integer(34)});
		return this.bean;
	}

	private TestBean getTestBean() {
		return (TestBean) getPageContext().getRequest().getAttribute(COMMAND_NAME);
	}

	public static class TestBeanWrapper {
		private TestBean bean;

		public TestBean getBean() {
			return bean;
		}

		public void setBean(TestBean bean) {
			this.bean = bean;
		}

	}

}
