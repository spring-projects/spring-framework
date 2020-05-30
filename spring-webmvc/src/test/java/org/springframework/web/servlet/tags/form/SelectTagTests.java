/*
 * Copyright 2002-2020 the original author or authors.
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
import org.junit.jupiter.api.Test;

import org.springframework.beans.propertyeditors.CustomCollectionEditor;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.format.Formatter;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.tags.TransformTag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

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
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();
		assertThat(output).isEqualTo("<select id=\"country\" name=\"country\"></select>");
	}

	@Test
	public void nullItems() throws Exception {
		this.tag.setPath("country");
		this.tag.setItems(null);

		this.tag.setItemValue("isoCode");
		this.tag.setItemLabel("name");
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();
		assertThat(output).isEqualTo("<select id=\"country\" name=\"country\"></select>");
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
		assertThat(result).isEqualTo(Tag.SKIP_BODY);
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
		assertThat(getPageContext().findAttribute("key")).isEqualTo("Austria(AT)");
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
		assertThat(getPageContext().findAttribute("key")).isEqualTo("Austria");
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
		assertThat(output.startsWith("<select ")).isTrue();
		assertThat(output.endsWith("</select>")).isTrue();
		assertThat(output.contains("option value=\"AT\" selected=\"selected\">Austria")).isTrue();
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
		assertThat(output.startsWith("<select ")).isTrue();
		assertThat(output.endsWith("</select>")).isTrue();
		assertThat(output.contains("selected=\"selected\"")).isFalse();
		assertThat(output.contains("multiple=\"multiple\"")).isFalse();
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
		assertThat(output.startsWith("<select ")).isTrue();
		assertThat(output.endsWith("</select>")).isTrue();
		assertThat(output.contains("option value=\"AT\" selected=\"selected\">Austria")).isTrue();
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
		assertThat(output.startsWith("<select ")).isTrue();
		assertThat(output.endsWith("</select>")).isTrue();
		assertThat(output.contains("selected=\"selected\"")).isFalse();
	}

	@Test
	public void withMap() throws Exception {
		this.tag.setPath("sex");
		this.tag.setItems(getSexes());
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);
	}

	@Test
	public void withInvalidList() throws Exception {
		this.tag.setPath("country");
		this.tag.setItems(new TestBean());
		this.tag.setItemValue("isoCode");
		assertThatExceptionOfType(JspException.class).as("use a non-Collection typed value as the value of 'items'").isThrownBy(
				this.tag::doStartTag)
			.withMessageContaining("items")
			.withMessageContaining("org.springframework.beans.testfixture.beans.TestBean");
	}

	@Test
	public void withNestedOptions() throws Exception {
		this.tag.setPath("country");
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.EVAL_BODY_INCLUDE);

		BindStatus value = (BindStatus) getPageContext().getAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE);
		assertThat(value.getValue()).as("Selected country not exposed in page context").isEqualTo("UK");

		result = this.tag.doEndTag();
		assertThat(result).isEqualTo(Tag.EVAL_PAGE);
		this.tag.doFinally();

		String output = getOutput();
		assertThat(output.startsWith("<select ")).isTrue();
		assertThat(output.endsWith("</select>")).isTrue();
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
			array[i] = i;
		}
		this.tag.setItems(array);
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertThat(rootElement.elements().size()).isEqualTo(2);

		Element selectElement = rootElement.element("select");
		assertThat(selectElement.getName()).isEqualTo("select");
		assertThat(selectElement.attribute("name").getValue()).isEqualTo("someIntegerArray");

		List children = selectElement.elements();
		assertThat(children.size()).as("Incorrect number of children").isEqualTo(array.length);

		Element e = (Element) selectElement.selectSingleNode("option[text() = '12']");
		assertThat(e.attribute("selected").getValue()).as("'12' node not selected").isEqualTo("selected");

		e = (Element) selectElement.selectSingleNode("option[text() = '34']");
		assertThat(e.attribute("selected").getValue()).as("'34' node not selected").isEqualTo("selected");
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
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();
		assertThat(output.startsWith("<select ")).isTrue();
		assertThat(output.endsWith("</select>")).isTrue();

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertThat(rootElement.getName()).isEqualTo("select");
		assertThat(rootElement.attribute("name").getValue()).isEqualTo("myFloat");
		List children = rootElement.elements();
		assertThat(children.size()).as("Incorrect number of children").isEqualTo(array.length);

		Element e = (Element) rootElement.selectSingleNode("option[text() = '12.34f']");
		assertThat(e.attribute("selected").getValue()).as("'12.34' node not selected").isEqualTo("selected");

		e = (Element) rootElement.selectSingleNode("option[text() = '12.32f']");
		assertThat(e.attribute("selected")).as("'12.32' node incorrectly selected").isNull();
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
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertThat(rootElement.elements().size()).isEqualTo(2);

		Element selectElement = rootElement.element("select");
		assertThat(selectElement.getName()).isEqualTo("select");
		assertThat(selectElement.attribute("name").getValue()).isEqualTo("someList");

		List children = selectElement.elements();
		assertThat(children.size()).as("Incorrect number of children").isEqualTo(4);

		Element e = (Element) selectElement.selectSingleNode("option[@value = 'UK']");
		assertThat(e.attribute("selected").getValue()).as("UK node not selected").isEqualTo("selected");
		assertThat(e.getText()).isEqualTo("United Kingdom(UK)");

		e = (Element) selectElement.selectSingleNode("option[@value = 'AT']");
		assertThat(e.attribute("selected").getValue()).as("AT node not selected").isEqualTo("selected");
		assertThat(e.getText()).isEqualTo("Austria(AT)");
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
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertThat(rootElement.elements().size()).isEqualTo(1);

		Element selectElement = rootElement.element("select");
		assertThat(selectElement.getName()).isEqualTo("select");
		assertThat(selectElement.attribute("name").getValue()).isEqualTo("realCountry");

		List children = selectElement.elements();
		assertThat(children.size()).as("Incorrect number of children").isEqualTo(4);

		Element e = (Element) selectElement.selectSingleNode("option[@value = 'UK']");
		assertThat(e.attribute("selected").getValue()).as("UK node not selected").isEqualTo("selected");
		assertThat(e.getText()).isEqualTo("United Kingdom");
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
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertThat(rootElement.elements().size()).isEqualTo(2);

		Element selectElement = rootElement.element("select");
		assertThat(selectElement.getName()).isEqualTo("select");
		assertThat(selectElement.attribute("name").getValue()).isEqualTo("someList");

		List children = selectElement.elements();
		assertThat(children.size()).as("Incorrect number of children").isEqualTo(4);

		Element e = (Element) selectElement.selectSingleNode("option[@value = 'UK']");
		assertThat(e.attribute("selected").getValue()).as("UK node not selected").isEqualTo("selected");
		assertThat(e.getText()).isEqualTo("United Kingdom");

		e = (Element) selectElement.selectSingleNode("option[@value = 'AT']");
		assertThat(e.attribute("selected").getValue()).as("AT node not selected").isEqualTo("selected");
		assertThat(e.getText()).isEqualTo("Austria");
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
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertThat(rootElement.elements().size()).isEqualTo(2);

		Element selectElement = rootElement.element("select");
		assertThat(selectElement.getName()).isEqualTo("select");
		assertThat(selectElement.attribute("name").getValue()).isEqualTo("someList");

		List children = selectElement.elements();
		assertThat(children.size()).as("Incorrect number of children").isEqualTo(4);

		Element e = (Element) selectElement.selectSingleNode("option[@value = 'UK']");
		assertThat(e.attribute("selected").getValue()).as("UK node not selected").isEqualTo("selected");

		e = (Element) selectElement.selectSingleNode("option[@value = 'AT']");
		assertThat(e.attribute("selected").getValue()).as("AT node not selected").isEqualTo("selected");
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
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertThat(rootElement.elements().size()).isEqualTo(2);

		Element selectElement = rootElement.element("select");
		assertThat(selectElement.getName()).isEqualTo("select");
		assertThat(selectElement.attribute("name").getValue()).isEqualTo("someMap");

		List children = selectElement.elements();
		assertThat(children.size()).as("Incorrect number of children").isEqualTo(2);

		Element e = (Element) selectElement.selectSingleNode("option[@value = 'M']");
		assertThat(e.attribute("selected").getValue()).as("M node not selected").isEqualTo("selected");

		e = (Element) selectElement.selectSingleNode("option[@value = 'F']");
		assertThat(e.attribute("selected").getValue()).as("F node not selected").isEqualTo("selected");
	}

	/**
	 * Tests new support added as a result of <a
	 * href="https://opensource.atlassian.com/projects/spring/browse/SPR-2660"
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
			assertThat(result).isEqualTo(Tag.SKIP_BODY);

			String output = getOutput();
			output = "<doc>" + output + "</doc>";

			SAXReader reader = new SAXReader();
			Document document = reader.read(new StringReader(output));
			Element rootElement = document.getRootElement();
			assertThat(rootElement.elements().size()).isEqualTo(2);

			Element selectElement = rootElement.element("select");
			assertThat(selectElement.getName()).isEqualTo("select");
			assertThat(selectElement.attribute("name").getValue()).isEqualTo("someMap");

			List children = selectElement.elements();
			assertThat(children.size()).as("Incorrect number of children").isEqualTo(3);

			Element e;
			e = (Element) selectElement.selectSingleNode("option[@value = '" + austria.getIsoCode() + "']");
			assertThat(e).as("Option node not found with Country ISO code value [" + austria.getIsoCode() + "].").isNotNull();
			assertThat(e.attribute("selected").getValue()).as("AT node not selected.").isEqualTo("selected");
			assertThat(e.getData()).as("AT Locale displayLanguage property not used for option label.").isEqualTo(LOCALE_AT.getDisplayLanguage());

			e = (Element) selectElement.selectSingleNode("option[@value = '" + usa.getIsoCode() + "']");
			assertThat(e).as("Option node not found with Country ISO code value [" + usa.getIsoCode() + "].").isNotNull();
			assertThat(e.attribute("selected").getValue()).as("US node not selected.").isEqualTo("selected");
			assertThat(e.getData()).as("US Locale displayLanguage property not used for option label.").isEqualTo(Locale.US.getDisplayLanguage());

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
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertThat(rootElement.elements().size()).isEqualTo(2);

		Element selectElement = rootElement.element("select");
		assertThat(selectElement.getName()).isEqualTo("select");
		assertThat(selectElement.attribute("name").getValue()).isEqualTo("someList");
		assertThat(selectElement.attribute("multiple").getValue()).isEqualTo("multiple");

		List children = selectElement.elements();
		assertThat(children.size()).as("Incorrect number of children").isEqualTo(4);

		Element inputElement = rootElement.element("input");
		assertThat(inputElement).isNotNull();
	}

	@Test
	public void multipleWithStringValue() throws Exception {
		this.tag.setPath("name");
		this.tag.setItems(Country.getCountries());
		this.tag.setItemValue("isoCode");
		this.tag.setMultiple("multiple");
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertThat(rootElement.elements().size()).isEqualTo(2);

		Element selectElement = rootElement.element("select");
		assertThat(selectElement.getName()).isEqualTo("select");
		assertThat(selectElement.attribute("name").getValue()).isEqualTo("name");
		assertThat(selectElement.attribute("multiple").getValue()).isEqualTo("multiple");

		List children = selectElement.elements();
		assertThat(children.size()).as("Incorrect number of children").isEqualTo(4);

		Element inputElement = rootElement.element("input");
		assertThat(inputElement).isNotNull();
	}

	@Test
	public void multipleExplicitlyTrue() throws Exception {
		this.tag.setPath("name");
		this.tag.setItems(Country.getCountries());
		this.tag.setItemValue("isoCode");
		this.tag.setMultiple("true");
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertThat(rootElement.elements().size()).isEqualTo(2);

		Element selectElement = rootElement.element("select");
		assertThat(selectElement.getName()).isEqualTo("select");
		assertThat(selectElement.attribute("name").getValue()).isEqualTo("name");
		assertThat(selectElement.attribute("multiple").getValue()).isEqualTo("multiple");

		List children = selectElement.elements();
		assertThat(children.size()).as("Incorrect number of children").isEqualTo(4);

		Element inputElement = rootElement.element("input");
		assertThat(inputElement).isNotNull();
	}

	@Test
	public void multipleExplicitlyFalse() throws Exception {
		this.tag.setPath("name");
		this.tag.setItems(Country.getCountries());
		this.tag.setItemValue("isoCode");
		this.tag.setMultiple("false");
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertThat(rootElement.elements().size()).isEqualTo(1);

		Element selectElement = rootElement.element("select");
		assertThat(selectElement.getName()).isEqualTo("select");
		assertThat(selectElement.attribute("name").getValue()).isEqualTo("name");
		assertThat(selectElement.attribute("multiple")).isNull();

		List children = selectElement.elements();
		assertThat(children.size()).as("Incorrect number of children").isEqualTo(4);
	}

	@Test
	public void multipleWithBooleanTrue() throws Exception {
		this.tag.setPath("name");
		this.tag.setItems(Country.getCountries());
		this.tag.setItemValue("isoCode");
		this.tag.setMultiple(true);
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertThat(rootElement.elements().size()).isEqualTo(2);

		Element selectElement = rootElement.element("select");
		assertThat(selectElement.getName()).isEqualTo("select");
		assertThat(selectElement.attribute("name").getValue()).isEqualTo("name");
		assertThat(selectElement.attribute("multiple").getValue()).isEqualTo("multiple");

		List children = selectElement.elements();
		assertThat(children.size()).as("Incorrect number of children").isEqualTo(4);

		Element inputElement = rootElement.element("input");
		assertThat(inputElement).isNotNull();
	}

	@Test
	public void multipleWithBooleanFalse() throws Exception {
		this.tag.setPath("name");
		this.tag.setItems(Country.getCountries());
		this.tag.setItemValue("isoCode");
		this.tag.setMultiple(false);
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertThat(rootElement.elements().size()).isEqualTo(1);

		Element selectElement = rootElement.element("select");
		assertThat(selectElement.getName()).isEqualTo("select");
		assertThat(selectElement.attribute("name").getValue()).isEqualTo("name");
		assertThat(selectElement.attribute("multiple")).isNull();

		List children = selectElement.elements();
		assertThat(children.size()).as("Incorrect number of children").isEqualTo(4);
	}


	private void assertStringArray() throws JspException, DocumentException {
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();
		assertThat(output.startsWith("<select ")).isTrue();
		assertThat(output.endsWith("</select>")).isTrue();

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertThat(rootElement.getName()).isEqualTo("select");
		assertThat(rootElement.attribute("name").getValue()).isEqualTo("name");

		List children = rootElement.elements();
		assertThat(children.size()).as("Incorrect number of children").isEqualTo(4);

		Element e = (Element) rootElement.selectSingleNode("option[text() = 'Rob']");
		assertThat(e.attribute("selected").getValue()).as("Rob node not selected").isEqualTo("selected");
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
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();
		validateOutput(output, selected);
		assertContainsAttribute(output, "size", "5");
	}

	private void validateOutput(String output, boolean selected) throws DocumentException {
		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertThat(rootElement.getName()).isEqualTo("select");
		assertThat(rootElement.attribute("name").getValue()).isEqualTo("country");

		List children = rootElement.elements();
		assertThat(children.size()).as("Incorrect number of children").isEqualTo(4);

		Element e = (Element) rootElement.selectSingleNode("option[@value = 'UK']");
		Attribute selectedAttr = e.attribute("selected");
		if (selected) {
			assertThat(selectedAttr != null && "selected".equals(selectedAttr.getValue())).isTrue();
		}
		else {
			assertThat(selectedAttr).isNull();
		}
	}

	@Override
	protected TestBean createTestBean() {
		this.bean = new TestBeanWithRealCountry();
		this.bean.setName("Rob");
		this.bean.setCountry("UK");
		this.bean.setSex("M");
		this.bean.setMyFloat(new Float("12.34"));
		this.bean.setSomeIntegerArray(new Integer[]{12, 34});
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
