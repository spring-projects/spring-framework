/*
 * Copyright 2002-2024 the original author or authors.
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

import java.beans.PropertyEditorSupport;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.jsp.tagext.Tag;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.junit.jupiter.api.Test;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.beans.testfixture.beans.Colour;
import org.springframework.beans.testfixture.beans.Pet;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.format.Formatter;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Thomas Risberg
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Benjamin Hoffmann
 * @author Jeremy Grelle
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CheckboxesTagTests extends AbstractFormTagTests {

	private CheckboxesTag tag;

	private TestBean bean;

	@Override
	protected void onSetUp() {
		this.tag = new CheckboxesTag() {
			@Override
			protected TagWriter createTagWriter() {
				return new TagWriter(getWriter());
			}
		};
		this.tag.setPageContext(getPageContext());
	}

	@Test
	void withMultiValueArray() throws Exception {
		this.tag.setPath("stringArray");
		this.tag.setItems(new Object[] {"foo", "bar", "baz"});
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";
		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = document.getRootElement().elements().get(0);
		Element checkboxElement1 = spanElement1.elements().get(0);
		assertThat(checkboxElement1.getName()).isEqualTo("input");
		assertThat(checkboxElement1.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement1.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement1.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement1.attribute("value").getValue()).isEqualTo("foo");
		assertThat(spanElement1.getStringValue()).isEqualTo("foo");
		Element spanElement2 = document.getRootElement().elements().get(1);
		Element checkboxElement2 = spanElement2.elements().get(0);
		assertThat(checkboxElement2.getName()).isEqualTo("input");
		assertThat(checkboxElement2.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement2.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement2.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement2.attribute("value").getValue()).isEqualTo("bar");
		assertThat(spanElement2.getStringValue()).isEqualTo("bar");
		Element spanElement3 = document.getRootElement().elements().get(2);
		Element checkboxElement3 = spanElement3.elements().get(0);
		assertThat(checkboxElement3.getName()).isEqualTo("input");
		assertThat(checkboxElement3.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement3.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement3.attribute("checked")).as("not checked").isNull();
		assertThat(checkboxElement3.attribute("value").getValue()).isEqualTo("baz");
		assertThat(spanElement3.getStringValue()).isEqualTo("baz");
	}

	@Test
	void withMultiValueArrayAndDynamicAttributes() throws Exception {
		String dynamicAttribute1 = "attr1";
		String dynamicAttribute2 = "attr2";

		this.tag.setPath("stringArray");
		this.tag.setItems(new Object[] {"foo", "bar", "baz"});
		this.tag.setDynamicAttribute(null, dynamicAttribute1, dynamicAttribute1);
		this.tag.setDynamicAttribute(null, dynamicAttribute2, dynamicAttribute2);

		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";
		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = document.getRootElement().elements().get(0);
		Element checkboxElement1 = spanElement1.elements().get(0);
		assertThat(checkboxElement1.getName()).isEqualTo("input");
		assertThat(checkboxElement1.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement1.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement1.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement1.attribute("value").getValue()).isEqualTo("foo");
		assertThat(spanElement1.getStringValue()).isEqualTo("foo");
		assertThat(checkboxElement1.attribute(dynamicAttribute1).getValue()).isEqualTo(dynamicAttribute1);
		assertThat(checkboxElement1.attribute(dynamicAttribute2).getValue()).isEqualTo(dynamicAttribute2);

		Element spanElement2 = document.getRootElement().elements().get(1);
		Element checkboxElement2 = spanElement2.elements().get(0);
		assertThat(checkboxElement2.getName()).isEqualTo("input");
		assertThat(checkboxElement2.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement2.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement2.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement2.attribute("value").getValue()).isEqualTo("bar");
		assertThat(spanElement2.getStringValue()).isEqualTo("bar");
		assertThat(checkboxElement2.attribute(dynamicAttribute1).getValue()).isEqualTo(dynamicAttribute1);
		assertThat(checkboxElement2.attribute(dynamicAttribute2).getValue()).isEqualTo(dynamicAttribute2);

		Element spanElement3 = document.getRootElement().elements().get(2);
		Element checkboxElement3 = spanElement3.elements().get(0);
		assertThat(checkboxElement3.getName()).isEqualTo("input");
		assertThat(checkboxElement3.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement3.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement3.attribute("checked")).as("not checked").isNull();
		assertThat(checkboxElement3.attribute("value").getValue()).isEqualTo("baz");
		assertThat(spanElement3.getStringValue()).isEqualTo("baz");
		assertThat(checkboxElement3.attribute(dynamicAttribute1).getValue()).isEqualTo(dynamicAttribute1);
		assertThat(checkboxElement3.attribute(dynamicAttribute2).getValue()).isEqualTo(dynamicAttribute2);
	}

	@Test
	void withMultiValueArrayWithDelimiter() throws Exception {
		this.tag.setDelimiter("<br/>");
		this.tag.setPath("stringArray");
		this.tag.setItems(new Object[] {"foo", "bar", "baz"});
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";
		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = document.getRootElement().elements().get(0);
		Element delimiterElement1 = spanElement1.element("br");
		assertThat(delimiterElement1).isNull();
		Element checkboxElement1 = spanElement1.elements().get(0);
		assertThat(checkboxElement1.getName()).isEqualTo("input");
		assertThat(checkboxElement1.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement1.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement1.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement1.attribute("value").getValue()).isEqualTo("foo");
		assertThat(spanElement1.getStringValue()).isEqualTo("foo");
		Element spanElement2 = document.getRootElement().elements().get(1);
		Element delimiterElement2 = spanElement2.elements().get(0);
		assertThat(delimiterElement2.getName()).isEqualTo("br");
		Element checkboxElement2 = spanElement2.elements().get(1);
		assertThat(checkboxElement2.getName()).isEqualTo("input");
		assertThat(checkboxElement2.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement2.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement2.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement2.attribute("value").getValue()).isEqualTo("bar");
		assertThat(spanElement2.getStringValue()).isEqualTo("bar");
		Element spanElement3 = document.getRootElement().elements().get(2);
		Element delimiterElement3 = spanElement3.elements().get(0);
		assertThat(delimiterElement3.getName()).isEqualTo("br");
		Element checkboxElement3 = spanElement3.elements().get(1);
		assertThat(checkboxElement3.getName()).isEqualTo("input");
		assertThat(checkboxElement3.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement3.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement3.attribute("checked")).as("not checked").isNull();
		assertThat(checkboxElement3.attribute("value").getValue()).isEqualTo("baz");
		assertThat(spanElement3.getStringValue()).isEqualTo("baz");
	}

	@Test
	void withMultiValueMap() throws Exception {
		this.tag.setPath("stringArray");
		Map m = new LinkedHashMap();
		m.put("foo", "FOO");
		m.put("bar", "BAR");
		m.put("baz", "BAZ");
		this.tag.setItems(m);
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = document.getRootElement().elements().get(0);
		Element checkboxElement1 = spanElement1.elements().get(0);
		assertThat(checkboxElement1.getName()).isEqualTo("input");
		assertThat(checkboxElement1.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement1.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement1.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement1.attribute("value").getValue()).isEqualTo("foo");
		assertThat(spanElement1.getStringValue()).isEqualTo("FOO");
		Element spanElement2 = document.getRootElement().elements().get(1);
		Element checkboxElement2 = spanElement2.elements().get(0);
		assertThat(checkboxElement2.getName()).isEqualTo("input");
		assertThat(checkboxElement2.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement2.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement2.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement2.attribute("value").getValue()).isEqualTo("bar");
		assertThat(spanElement2.getStringValue()).isEqualTo("BAR");
		Element spanElement3 = document.getRootElement().elements().get(2);
		Element checkboxElement3 = spanElement3.elements().get(0);
		assertThat(checkboxElement3.getName()).isEqualTo("input");
		assertThat(checkboxElement3.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement3.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement3.attribute("checked")).as("not checked").isNull();
		assertThat(checkboxElement3.attribute("value").getValue()).isEqualTo("baz");
		assertThat(spanElement3.getStringValue()).isEqualTo("BAZ");
	}

	@Test
	void withPetItemsMap() throws Exception {
		this.tag.setPath("someSet");
		Map m = new LinkedHashMap();
		m.put(new ItemPet("PET1"), "PET1Label");
		m.put(new ItemPet("PET2"), "PET2Label");
		m.put(new ItemPet("PET3"), "PET3Label");
		this.tag.setItems(m);
		tag.setItemValue("name");
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = document.getRootElement().elements().get(0);
		Element checkboxElement1 = spanElement1.elements().get(0);
		assertThat(checkboxElement1.getName()).isEqualTo("input");
		assertThat(checkboxElement1.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement1.attribute("name").getValue()).isEqualTo("someSet");
		assertThat(checkboxElement1.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement1.attribute("value").getValue()).isEqualTo("PET1");
		assertThat(spanElement1.getStringValue()).isEqualTo("PET1Label");
		Element spanElement2 = document.getRootElement().elements().get(1);
		Element checkboxElement2 = spanElement2.elements().get(0);
		assertThat(checkboxElement2.getName()).isEqualTo("input");
		assertThat(checkboxElement2.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement2.attribute("name").getValue()).isEqualTo("someSet");
		assertThat(checkboxElement2.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement2.attribute("value").getValue()).isEqualTo("PET2");
		assertThat(spanElement2.getStringValue()).isEqualTo("PET2Label");
		Element spanElement3 = document.getRootElement().elements().get(2);
		Element checkboxElement3 = spanElement3.elements().get(0);
		assertThat(checkboxElement3.getName()).isEqualTo("input");
		assertThat(checkboxElement3.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement3.attribute("name").getValue()).isEqualTo("someSet");
		assertThat(checkboxElement3.attribute("checked")).as("not checked").isNull();
		assertThat(checkboxElement3.attribute("value").getValue()).isEqualTo("PET3");
		assertThat(spanElement3.getStringValue()).isEqualTo("PET3Label");
	}

	@Test
	void withMultiValueMapWithDelimiter() throws Exception {
		String delimiter = " | ";
		this.tag.setDelimiter(delimiter);
		this.tag.setPath("stringArray");
		Map m = new LinkedHashMap();
		m.put("foo", "FOO");
		m.put("bar", "BAR");
		m.put("baz", "BAZ");
		this.tag.setItems(m);
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = document.getRootElement().elements().get(0);
		Element checkboxElement1 = spanElement1.elements().get(0);
		assertThat(checkboxElement1.getName()).isEqualTo("input");
		assertThat(checkboxElement1.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement1.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement1.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement1.attribute("value").getValue()).isEqualTo("foo");
		assertThat(spanElement1.getStringValue()).isEqualTo("FOO");
		Element spanElement2 = document.getRootElement().elements().get(1);
		Element checkboxElement2 = spanElement2.elements().get(0);
		assertThat(checkboxElement2.getName()).isEqualTo("input");
		assertThat(checkboxElement2.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement2.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement2.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement2.attribute("value").getValue()).isEqualTo("bar");
		assertThat(spanElement2.getStringValue()).isEqualTo((delimiter + "BAR"));
		Element spanElement3 = document.getRootElement().elements().get(2);
		Element checkboxElement3 = spanElement3.elements().get(0);
		assertThat(checkboxElement3.getName()).isEqualTo("input");
		assertThat(checkboxElement3.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement3.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement3.attribute("checked")).as("not checked").isNull();
		assertThat(checkboxElement3.attribute("value").getValue()).isEqualTo("baz");
		assertThat(spanElement3.getStringValue()).isEqualTo((delimiter + "BAZ"));
	}

	@Test
	void withMultiValueWithEditor() throws Exception {
		this.tag.setPath("stringArray");
		this.tag.setItems(new Object[] {"   foo", "   bar", "   baz"});
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(this.bean, COMMAND_NAME);
		MyStringTrimmerEditor editor = new MyStringTrimmerEditor();
		bindingResult.getPropertyEditorRegistry().registerCustomEditor(String.class, editor);
		getPageContext().getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + COMMAND_NAME, bindingResult);

		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);
		assertThat(editor.allProcessedValues).hasSize(3);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = document.getRootElement().elements().get(0);
		Element checkboxElement1 = spanElement1.elements().get(0);
		assertThat(checkboxElement1.getName()).isEqualTo("input");
		assertThat(checkboxElement1.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement1.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement1.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement1.attribute("value").getValue()).isEqualTo("   foo");
		Element spanElement2 = document.getRootElement().elements().get(1);
		Element checkboxElement2 = spanElement2.elements().get(0);
		assertThat(checkboxElement2.getName()).isEqualTo("input");
		assertThat(checkboxElement2.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement2.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement2.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement2.attribute("value").getValue()).isEqualTo("   bar");
		Element spanElement3 = document.getRootElement().elements().get(2);
		Element checkboxElement3 = spanElement3.elements().get(0);
		assertThat(checkboxElement3.getName()).isEqualTo("input");
		assertThat(checkboxElement3.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement3.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement3.attribute("checked")).as("not checked").isNull();
		assertThat(checkboxElement3.attribute("value").getValue()).isEqualTo("   baz");
	}

	@Test
	void withMultiValueWithReverseEditor() throws Exception {
		this.tag.setPath("stringArray");
		this.tag.setItems(new Object[] {"FOO", "BAR", "BAZ"});
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(this.bean, COMMAND_NAME);
		MyLowerCaseEditor editor = new MyLowerCaseEditor();
		bindingResult.getPropertyEditorRegistry().registerCustomEditor(String.class, editor);
		getPageContext().getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + COMMAND_NAME, bindingResult);

		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = document.getRootElement().elements().get(0);
		Element checkboxElement1 = spanElement1.elements().get(0);
		assertThat(checkboxElement1.getName()).isEqualTo("input");
		assertThat(checkboxElement1.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement1.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement1.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement1.attribute("value").getValue()).isEqualTo("FOO");
		Element spanElement2 = document.getRootElement().elements().get(1);
		Element checkboxElement2 = spanElement2.elements().get(0);
		assertThat(checkboxElement2.getName()).isEqualTo("input");
		assertThat(checkboxElement2.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement2.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement2.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement2.attribute("value").getValue()).isEqualTo("BAR");
		Element spanElement3 = document.getRootElement().elements().get(2);
		Element checkboxElement3 = spanElement3.elements().get(0);
		assertThat(checkboxElement3.getName()).isEqualTo("input");
		assertThat(checkboxElement3.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement3.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement3.attribute("checked")).as("not checked").isNull();
		assertThat(checkboxElement3.attribute("value").getValue()).isEqualTo("BAZ");
	}

	@Test
	void withMultiValueWithFormatter() throws Exception {
		this.tag.setPath("stringArray");
		this.tag.setItems(new Object[] {"   foo", "   bar", "   baz"});
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(this.bean, COMMAND_NAME);
		FormattingConversionService cs = new FormattingConversionService();
		cs.addFormatterForFieldType(String.class, new Formatter<String>() {
			@Override
			public String print(String object, Locale locale) {
				return object;
			}
			@Override
			public String parse(String text, Locale locale) {
				return text.trim();
			}
		});
		bindingResult.initConversion(cs);
		getPageContext().getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + COMMAND_NAME, bindingResult);

		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = document.getRootElement().elements().get(0);
		Element checkboxElement1 = spanElement1.elements().get(0);
		assertThat(checkboxElement1.getName()).isEqualTo("input");
		assertThat(checkboxElement1.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement1.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement1.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement1.attribute("value").getValue()).isEqualTo("   foo");
		Element spanElement2 = document.getRootElement().elements().get(1);
		Element checkboxElement2 = spanElement2.elements().get(0);
		assertThat(checkboxElement2.getName()).isEqualTo("input");
		assertThat(checkboxElement2.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement2.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement2.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement2.attribute("value").getValue()).isEqualTo("   bar");
		Element spanElement3 = document.getRootElement().elements().get(2);
		Element checkboxElement3 = spanElement3.elements().get(0);
		assertThat(checkboxElement3.getName()).isEqualTo("input");
		assertThat(checkboxElement3.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement3.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement3.attribute("checked")).as("not checked").isNull();
		assertThat(checkboxElement3.attribute("value").getValue()).isEqualTo("   baz");
	}

	@Test
	void collectionOfPets() throws Exception {
		this.tag.setPath("pets");
		List allPets = new ArrayList();
		allPets.add(new ItemPet("Rudiger"));
		allPets.add(new ItemPet("Spot"));
		allPets.add(new ItemPet("Checkers"));
		allPets.add(new ItemPet("Fluffy"));
		allPets.add(new ItemPet("Mufty"));
		this.tag.setItems(allPets);
		this.tag.setItemValue("name");
		this.tag.setItemLabel("label");

		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = document.getRootElement().elements().get(0);
		Element checkboxElement1 = spanElement1.elements().get(0);
		assertThat(checkboxElement1.getName()).isEqualTo("input");
		assertThat(checkboxElement1.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement1.attribute("name").getValue()).isEqualTo("pets");
		assertThat(checkboxElement1.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement1.attribute("value").getValue()).isEqualTo("Rudiger");
		assertThat(spanElement1.getStringValue()).isEqualTo("RUDIGER");
		Element spanElement2 = document.getRootElement().elements().get(1);
		Element checkboxElement2 = spanElement2.elements().get(0);
		assertThat(checkboxElement2.getName()).isEqualTo("input");
		assertThat(checkboxElement2.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement2.attribute("name").getValue()).isEqualTo("pets");
		assertThat(checkboxElement2.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement2.attribute("value").getValue()).isEqualTo("Spot");
		assertThat(spanElement2.getStringValue()).isEqualTo("SPOT");
		Element spanElement3 = document.getRootElement().elements().get(2);
		Element checkboxElement3 = spanElement3.elements().get(0);
		assertThat(checkboxElement3.getName()).isEqualTo("input");
		assertThat(checkboxElement3.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement3.attribute("name").getValue()).isEqualTo("pets");
		assertThat(checkboxElement3.attribute("checked")).as("not checked").isNull();
		assertThat(checkboxElement3.attribute("value").getValue()).isEqualTo("Checkers");
		assertThat(spanElement3.getStringValue()).isEqualTo("CHECKERS");
		Element spanElement4 = document.getRootElement().elements().get(3);
		Element checkboxElement4 = spanElement4.elements().get(0);
		assertThat(checkboxElement4.getName()).isEqualTo("input");
		assertThat(checkboxElement4.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement4.attribute("name").getValue()).isEqualTo("pets");
		assertThat(checkboxElement4.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement4.attribute("value").getValue()).isEqualTo("Fluffy");
		assertThat(spanElement4.getStringValue()).isEqualTo("FLUFFY");
		Element spanElement5 = document.getRootElement().elements().get(4);
		Element checkboxElement5 = spanElement5.elements().get(0);
		assertThat(checkboxElement5.getName()).isEqualTo("input");
		assertThat(checkboxElement5.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement5.attribute("name").getValue()).isEqualTo("pets");
		assertThat(checkboxElement5.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement5.attribute("value").getValue()).isEqualTo("Mufty");
		assertThat(spanElement5.getStringValue()).isEqualTo("MUFTY");
	}

	/**
	 * Test case where items toString() doesn't fit the item ID
	 */
	@Test
	void collectionOfItemPets() throws Exception {
		this.tag.setPath("someSet");
		List allPets = new ArrayList();
		allPets.add(new ItemPet("PET1"));
		allPets.add(new ItemPet("PET2"));
		allPets.add(new ItemPet("PET3"));
		this.tag.setItems(allPets);
		this.tag.setItemValue("name");
		this.tag.setItemLabel("label");

		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = document.getRootElement().elements().get(0);
		Element checkboxElement1 = spanElement1.elements().get(0);
		assertThat(checkboxElement1.getName()).isEqualTo("input");
		assertThat(checkboxElement1.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement1.attribute("name").getValue()).isEqualTo("someSet");
		assertThat(checkboxElement1.attribute("checked")).as("should be checked").isNotNull();
		assertThat(checkboxElement1.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement1.attribute("value").getValue()).isEqualTo("PET1");
		assertThat(spanElement1.getStringValue()).isEqualTo("PET1");
		Element spanElement2 = document.getRootElement().elements().get(1);
		Element checkboxElement2 = spanElement2.elements().get(0);
		assertThat(checkboxElement2.getName()).isEqualTo("input");
		assertThat(checkboxElement2.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement2.attribute("name").getValue()).isEqualTo("someSet");
		assertThat(checkboxElement2.attribute("checked")).as("should be checked").isNotNull();
		assertThat(checkboxElement2.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement2.attribute("value").getValue()).isEqualTo("PET2");
		assertThat(spanElement2.getStringValue()).isEqualTo("PET2");
		Element spanElement3 = document.getRootElement().elements().get(2);
		Element checkboxElement3 = spanElement3.elements().get(0);
		assertThat(checkboxElement3.getName()).isEqualTo("input");
		assertThat(checkboxElement3.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement3.attribute("name").getValue()).isEqualTo("someSet");
		assertThat(checkboxElement3.attribute("checked")).as("not checked").isNull();
		assertThat(checkboxElement3.attribute("value").getValue()).isEqualTo("PET3");
		assertThat(spanElement3.getStringValue()).isEqualTo("PET3");
	}

	@Test
	void collectionOfPetsWithEditor() throws Exception {
		this.tag.setPath("pets");
		List allPets = new ArrayList();
		allPets.add(new ItemPet("Rudiger"));
		allPets.add(new ItemPet("Spot"));
		allPets.add(new ItemPet("Checkers"));
		allPets.add(new ItemPet("Fluffy"));
		allPets.add(new ItemPet("Mufty"));
		this.tag.setItems(allPets);
		this.tag.setItemLabel("label");
		this.tag.setId("myId");

		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(this.bean, COMMAND_NAME);
		PropertyEditorSupport editor = new ItemPet.CustomEditor();
		bindingResult.getPropertyEditorRegistry().registerCustomEditor(ItemPet.class, editor);
		getPageContext().getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + COMMAND_NAME, bindingResult);

		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = document.getRootElement().elements().get(0);
		Element checkboxElement1 = spanElement1.elements().get(0);
		assertThat(checkboxElement1.getName()).isEqualTo("input");
		assertThat(checkboxElement1.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement1.attribute("name").getValue()).isEqualTo("pets");
		assertThat(checkboxElement1.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement1.attribute("value").getValue()).isEqualTo("Rudiger");
		assertThat(spanElement1.getStringValue()).isEqualTo("RUDIGER");
		Element spanElement2 = document.getRootElement().elements().get(1);
		Element checkboxElement2 = spanElement2.elements().get(0);
		assertThat(checkboxElement2.getName()).isEqualTo("input");
		assertThat(checkboxElement2.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement2.attribute("name").getValue()).isEqualTo("pets");
		assertThat(checkboxElement2.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement2.attribute("value").getValue()).isEqualTo("Spot");
		assertThat(spanElement2.getStringValue()).isEqualTo("SPOT");
		Element spanElement3 = document.getRootElement().elements().get(2);
		Element checkboxElement3 = spanElement3.elements().get(0);
		assertThat(checkboxElement3.getName()).isEqualTo("input");
		assertThat(checkboxElement3.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement3.attribute("name").getValue()).isEqualTo("pets");
		assertThat(checkboxElement3.attribute("checked")).as("not checked").isNull();
		assertThat(checkboxElement3.attribute("value").getValue()).isEqualTo("Checkers");
		assertThat(spanElement3.getStringValue()).isEqualTo("CHECKERS");
		Element spanElement4 = document.getRootElement().elements().get(3);
		Element checkboxElement4 = spanElement4.elements().get(0);
		assertThat(checkboxElement4.getName()).isEqualTo("input");
		assertThat(checkboxElement4.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement4.attribute("name").getValue()).isEqualTo("pets");
		assertThat(checkboxElement4.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement4.attribute("value").getValue()).isEqualTo("Fluffy");
		assertThat(spanElement4.getStringValue()).isEqualTo("FLUFFY");
		Element spanElement5 = document.getRootElement().elements().get(4);
		Element checkboxElement5 = spanElement5.elements().get(0);
		assertThat(checkboxElement5.getName()).isEqualTo("input");
		assertThat(checkboxElement5.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement5.attribute("name").getValue()).isEqualTo("pets");
		assertThat(checkboxElement5.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement5.attribute("value").getValue()).isEqualTo("Mufty");
		assertThat(spanElement5.getStringValue()).isEqualTo("MUFTY");
	}

	@Test
	void withNullValue() {
		this.tag.setPath("name");
		assertThatIllegalArgumentException().as("null value binding to a non-boolean").isThrownBy(
				this.tag::doStartTag);
	}

	@Test
	void hiddenElementOmittedOnDisabled() throws Exception {
		this.tag.setPath("stringArray");
		this.tag.setItems(new Object[] {"foo", "bar", "baz"});
		this.tag.setDisabled(true);
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);
		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertThat(rootElement.elements()).as("Both tag and hidden element rendered incorrectly").hasSize(3);
		Element spanElement = document.getRootElement().elements().get(0);
		Element checkboxElement = spanElement.elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement.attribute("disabled").getValue()).isEqualTo("disabled");
		assertThat(checkboxElement.attribute("value").getValue()).isEqualTo("foo");
	}

	@Test
	void spanElementCustomizable() throws Exception {
		this.tag.setPath("stringArray");
		this.tag.setItems(new Object[] {"foo", "bar", "baz"});
		this.tag.setElement("element");
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);
		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement = document.getRootElement().elements().get(0);
		assertThat(spanElement.getName()).isEqualTo("element");
	}

	@Test
	void dynamicTypeAttribute() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				this.tag.setDynamicAttribute(null, "type", "email"))
			.withMessage("Attribute type=\"email\" is not allowed");
	}


	private Date getDate() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, 10);
		cal.set(Calendar.MONTH, 10);
		cal.set(Calendar.DATE, 10);
		cal.set(Calendar.HOUR, 10);
		cal.set(Calendar.MINUTE, 10);
		cal.set(Calendar.SECOND, 10);
		return cal.getTime();
	}

	@Override
	protected TestBean createTestBean() {
		List colours = new ArrayList();
		colours.add(Colour.BLUE);
		colours.add(Colour.RED);
		colours.add(Colour.GREEN);

		List pets = new ArrayList();
		pets.add(new Pet("Rudiger"));
		pets.add(new Pet("Spot"));
		pets.add(new Pet("Fluffy"));
		pets.add(new Pet("Mufty"));

		Set someObjects = new HashSet();
		someObjects.add(new ItemPet("PET1"));
		someObjects.add(new ItemPet("PET2"));

		this.bean = new TestBean();
		this.bean.setDate(getDate());
		this.bean.setName("Rob Harrop");
		this.bean.setJedi(true);
		this.bean.setSomeBoolean(Boolean.TRUE);
		this.bean.setStringArray(new String[] {"bar", "foo"});
		this.bean.setSomeIntegerArray(new Integer[] {2, 1});
		this.bean.setOtherColours(colours);
		this.bean.setPets(pets);
		this.bean.setSomeSet(someObjects);
		List list = new ArrayList();
		list.add("foo");
		list.add("bar");
		this.bean.setSomeList(list);
		return this.bean;
	}


	private static class MyStringTrimmerEditor extends StringTrimmerEditor {

		public final Set allProcessedValues = new HashSet();

		public MyStringTrimmerEditor() {
			super(false);
		}

		@Override
		public void setAsText(String text) {
			super.setAsText(text);
			this.allProcessedValues.add(getValue());
		}
	}


	private static class MyLowerCaseEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			setValue(text.toLowerCase());
		}

		@Override
		public String getAsText() {
			return ObjectUtils.nullSafeToString(getValue()).toUpperCase();
		}
	}

}
