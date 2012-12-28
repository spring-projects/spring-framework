/*
 * Copyright 2002-2010 the original author or authors.
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

import java.beans.PropertyEditorSupport;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import org.springframework.beans.Colour;
import org.springframework.beans.Pet;
import org.springframework.beans.TestBean;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.format.Formatter;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

/**
 * @author Thomas Risberg
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Benjamin Hoffmann
 * @author Jeremy Grelle
 */
public class CheckboxesTagTests extends AbstractFormTagTests {

	private CheckboxesTag tag;

	private TestBean bean;

	protected void onSetUp() {
		this.tag = new CheckboxesTag() {
			protected TagWriter createTagWriter() {
				return new TagWriter(getWriter());
			}
		};
		this.tag.setPageContext(getPageContext());
	}

	public void testWithMultiValueArray() throws Exception {
		this.tag.setPath("stringArray");
		this.tag.setItems(new Object[] {"foo", "bar", "baz"});
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";
		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = (Element) document.getRootElement().elements().get(0);
		Element checkboxElement1 = (Element) spanElement1.elements().get(0);
		assertEquals("input", checkboxElement1.getName());
		assertEquals("checkbox", checkboxElement1.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement1.attribute("name").getValue());
		assertEquals("checked", checkboxElement1.attribute("checked").getValue());
		assertEquals("foo", checkboxElement1.attribute("value").getValue());
		assertEquals("foo", spanElement1.getStringValue());
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element checkboxElement2 = (Element) spanElement2.elements().get(0);
		assertEquals("input", checkboxElement2.getName());
		assertEquals("checkbox", checkboxElement2.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement2.attribute("name").getValue());
		assertEquals("checked", checkboxElement2.attribute("checked").getValue());
		assertEquals("bar", checkboxElement2.attribute("value").getValue());
		assertEquals("bar", spanElement2.getStringValue());
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element checkboxElement3 = (Element) spanElement3.elements().get(0);
		assertEquals("input", checkboxElement3.getName());
		assertEquals("checkbox", checkboxElement3.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement3.attribute("name").getValue());
		assertNull("not checked", checkboxElement3.attribute("checked"));
		assertEquals("baz", checkboxElement3.attribute("value").getValue());
		assertEquals("baz", spanElement3.getStringValue());
	}

	public void testWithMultiValueArrayAndDynamicAttributes() throws Exception {
		String dynamicAttribute1 = "attr1";
		String dynamicAttribute2 = "attr2";

		this.tag.setPath("stringArray");
		this.tag.setItems(new Object[] {"foo", "bar", "baz"});
		this.tag.setDynamicAttribute(null, dynamicAttribute1, dynamicAttribute1);
		this.tag.setDynamicAttribute(null, dynamicAttribute2, dynamicAttribute2);

		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";
		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = (Element) document.getRootElement().elements().get(0);
		Element checkboxElement1 = (Element) spanElement1.elements().get(0);
		assertEquals("input", checkboxElement1.getName());
		assertEquals("checkbox", checkboxElement1.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement1.attribute("name").getValue());
		assertEquals("checked", checkboxElement1.attribute("checked").getValue());
		assertEquals("foo", checkboxElement1.attribute("value").getValue());
		assertEquals("foo", spanElement1.getStringValue());
		assertEquals(dynamicAttribute1, checkboxElement1.attribute(dynamicAttribute1).getValue());
		assertEquals(dynamicAttribute2, checkboxElement1.attribute(dynamicAttribute2).getValue());

		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element checkboxElement2 = (Element) spanElement2.elements().get(0);
		assertEquals("input", checkboxElement2.getName());
		assertEquals("checkbox", checkboxElement2.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement2.attribute("name").getValue());
		assertEquals("checked", checkboxElement2.attribute("checked").getValue());
		assertEquals("bar", checkboxElement2.attribute("value").getValue());
		assertEquals("bar", spanElement2.getStringValue());
		assertEquals(dynamicAttribute1, checkboxElement2.attribute(dynamicAttribute1).getValue());
		assertEquals(dynamicAttribute2, checkboxElement2.attribute(dynamicAttribute2).getValue());

		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element checkboxElement3 = (Element) spanElement3.elements().get(0);
		assertEquals("input", checkboxElement3.getName());
		assertEquals("checkbox", checkboxElement3.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement3.attribute("name").getValue());
		assertNull("not checked", checkboxElement3.attribute("checked"));
		assertEquals("baz", checkboxElement3.attribute("value").getValue());
		assertEquals("baz", spanElement3.getStringValue());
		assertEquals(dynamicAttribute1, checkboxElement3.attribute(dynamicAttribute1).getValue());
		assertEquals(dynamicAttribute2, checkboxElement3.attribute(dynamicAttribute2).getValue());

	}

	public void testWithMultiValueArrayWithDelimiter() throws Exception {
		this.tag.setDelimiter("<br/>");
		this.tag.setPath("stringArray");
		this.tag.setItems(new Object[] {"foo", "bar", "baz"});
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";
		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = (Element) document.getRootElement().elements().get(0);
		Element delimiterElement1 = spanElement1.element("br");
		assertNull(delimiterElement1);
		Element checkboxElement1 = (Element) spanElement1.elements().get(0);
		assertEquals("input", checkboxElement1.getName());
		assertEquals("checkbox", checkboxElement1.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement1.attribute("name").getValue());
		assertEquals("checked", checkboxElement1.attribute("checked").getValue());
		assertEquals("foo", checkboxElement1.attribute("value").getValue());
		assertEquals("foo", spanElement1.getStringValue());
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element delimiterElement2 = (Element) spanElement2.elements().get(0);
		assertEquals("br", delimiterElement2.getName());
		Element checkboxElement2 = (Element) spanElement2.elements().get(1);
		assertEquals("input", checkboxElement2.getName());
		assertEquals("checkbox", checkboxElement2.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement2.attribute("name").getValue());
		assertEquals("checked", checkboxElement2.attribute("checked").getValue());
		assertEquals("bar", checkboxElement2.attribute("value").getValue());
		assertEquals("bar", spanElement2.getStringValue());
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element delimiterElement3 = (Element) spanElement3.elements().get(0);
		assertEquals("br", delimiterElement3.getName());
		Element checkboxElement3 = (Element) spanElement3.elements().get(1);
		assertEquals("input", checkboxElement3.getName());
		assertEquals("checkbox", checkboxElement3.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement3.attribute("name").getValue());
		assertNull("not checked", checkboxElement3.attribute("checked"));
		assertEquals("baz", checkboxElement3.attribute("value").getValue());
		assertEquals("baz", spanElement3.getStringValue());
	}

	public void testWithMultiValueMap() throws Exception {
		this.tag.setPath("stringArray");
		Map m = new LinkedHashMap();
		m.put("foo", "FOO");
		m.put("bar", "BAR");
		m.put("baz", "BAZ");
		this.tag.setItems(m);
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = (Element) document.getRootElement().elements().get(0);
		Element checkboxElement1 = (Element) spanElement1.elements().get(0);
		assertEquals("input", checkboxElement1.getName());
		assertEquals("checkbox", checkboxElement1.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement1.attribute("name").getValue());
		assertEquals("checked", checkboxElement1.attribute("checked").getValue());
		assertEquals("foo", checkboxElement1.attribute("value").getValue());
		assertEquals("FOO", spanElement1.getStringValue());
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element checkboxElement2 = (Element) spanElement2.elements().get(0);
		assertEquals("input", checkboxElement2.getName());
		assertEquals("checkbox", checkboxElement2.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement2.attribute("name").getValue());
		assertEquals("checked", checkboxElement2.attribute("checked").getValue());
		assertEquals("bar", checkboxElement2.attribute("value").getValue());
		assertEquals("BAR", spanElement2.getStringValue());
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element checkboxElement3 = (Element) spanElement3.elements().get(0);
		assertEquals("input", checkboxElement3.getName());
		assertEquals("checkbox", checkboxElement3.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement3.attribute("name").getValue());
		assertNull("not checked", checkboxElement3.attribute("checked"));
		assertEquals("baz", checkboxElement3.attribute("value").getValue());
		assertEquals("BAZ", spanElement3.getStringValue());
	}

	public void testWithPetItemsMap() throws Exception {
		this.tag.setPath("someSet");
		Map m = new LinkedHashMap();
		m.put(new ItemPet("PET1"), "PET1Label");
		m.put(new ItemPet("PET2"), "PET2Label");
		m.put(new ItemPet("PET3"), "PET3Label");
		this.tag.setItems(m);
		tag.setItemValue("name");
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = (Element) document.getRootElement().elements().get(0);
		Element checkboxElement1 = (Element) spanElement1.elements().get(0);
		assertEquals("input", checkboxElement1.getName());
		assertEquals("checkbox", checkboxElement1.attribute("type").getValue());
		assertEquals("someSet", checkboxElement1.attribute("name").getValue());
		assertEquals("checked", checkboxElement1.attribute("checked").getValue());
		assertEquals("PET1", checkboxElement1.attribute("value").getValue());
		assertEquals("PET1Label", spanElement1.getStringValue());
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element checkboxElement2 = (Element) spanElement2.elements().get(0);
		assertEquals("input", checkboxElement2.getName());
		assertEquals("checkbox", checkboxElement2.attribute("type").getValue());
		assertEquals("someSet", checkboxElement2.attribute("name").getValue());
		assertEquals("checked", checkboxElement2.attribute("checked").getValue());
		assertEquals("PET2", checkboxElement2.attribute("value").getValue());
		assertEquals("PET2Label", spanElement2.getStringValue());
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element checkboxElement3 = (Element) spanElement3.elements().get(0);
		assertEquals("input", checkboxElement3.getName());
		assertEquals("checkbox", checkboxElement3.attribute("type").getValue());
		assertEquals("someSet", checkboxElement3.attribute("name").getValue());
		assertNull("not checked", checkboxElement3.attribute("checked"));
		assertEquals("PET3", checkboxElement3.attribute("value").getValue());
		assertEquals("PET3Label", spanElement3.getStringValue());
	}

	public void testWithMultiValueMapWithDelimiter() throws Exception {
		String delimiter = " | ";
		this.tag.setDelimiter(delimiter);
		this.tag.setPath("stringArray");
		Map m = new LinkedHashMap();
		m.put("foo", "FOO");
		m.put("bar", "BAR");
		m.put("baz", "BAZ");
		this.tag.setItems(m);
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = (Element) document.getRootElement().elements().get(0);
		Element checkboxElement1 = (Element) spanElement1.elements().get(0);
		assertEquals("input", checkboxElement1.getName());
		assertEquals("checkbox", checkboxElement1.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement1.attribute("name").getValue());
		assertEquals("checked", checkboxElement1.attribute("checked").getValue());
		assertEquals("foo", checkboxElement1.attribute("value").getValue());
		assertEquals("FOO", spanElement1.getStringValue());
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element checkboxElement2 = (Element) spanElement2.elements().get(0);
		assertEquals("input", checkboxElement2.getName());
		assertEquals("checkbox", checkboxElement2.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement2.attribute("name").getValue());
		assertEquals("checked", checkboxElement2.attribute("checked").getValue());
		assertEquals("bar", checkboxElement2.attribute("value").getValue());
		assertEquals(delimiter + "BAR", spanElement2.getStringValue());
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element checkboxElement3 = (Element) spanElement3.elements().get(0);
		assertEquals("input", checkboxElement3.getName());
		assertEquals("checkbox", checkboxElement3.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement3.attribute("name").getValue());
		assertNull("not checked", checkboxElement3.attribute("checked"));
		assertEquals("baz", checkboxElement3.attribute("value").getValue());
		assertEquals(delimiter + "BAZ", spanElement3.getStringValue());
	}

	public void testWithMultiValueWithEditor() throws Exception {
		this.tag.setPath("stringArray");
		this.tag.setItems(new Object[] {"   foo", "   bar", "   baz"});
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(this.bean, COMMAND_NAME);
		MyStringTrimmerEditor editor = new MyStringTrimmerEditor();
		bindingResult.getPropertyEditorRegistry().registerCustomEditor(String.class, editor);
		getPageContext().getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + COMMAND_NAME, bindingResult);

		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);
		assertEquals(3, editor.allProcessedValues.size());

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = (Element) document.getRootElement().elements().get(0);
		Element checkboxElement1 = (Element) spanElement1.elements().get(0);
		assertEquals("input", checkboxElement1.getName());
		assertEquals("checkbox", checkboxElement1.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement1.attribute("name").getValue());
		assertEquals("checked", checkboxElement1.attribute("checked").getValue());
		assertEquals("   foo", checkboxElement1.attribute("value").getValue());
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element checkboxElement2 = (Element) spanElement2.elements().get(0);
		assertEquals("input", checkboxElement2.getName());
		assertEquals("checkbox", checkboxElement2.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement2.attribute("name").getValue());
		assertEquals("checked", checkboxElement2.attribute("checked").getValue());
		assertEquals("   bar", checkboxElement2.attribute("value").getValue());
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element checkboxElement3 = (Element) spanElement3.elements().get(0);
		assertEquals("input", checkboxElement3.getName());
		assertEquals("checkbox", checkboxElement3.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement3.attribute("name").getValue());
		assertNull("not checked", checkboxElement3.attribute("checked"));
		assertEquals("   baz", checkboxElement3.attribute("value").getValue());
	}

	public void testWithMultiValueWithReverseEditor() throws Exception {
		this.tag.setPath("stringArray");
		this.tag.setItems(new Object[] {"FOO", "BAR", "BAZ"});
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(this.bean, COMMAND_NAME);
		MyLowerCaseEditor editor = new MyLowerCaseEditor();
		bindingResult.getPropertyEditorRegistry().registerCustomEditor(String.class, editor);
		getPageContext().getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + COMMAND_NAME, bindingResult);

		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = (Element) document.getRootElement().elements().get(0);
		Element checkboxElement1 = (Element) spanElement1.elements().get(0);
		assertEquals("input", checkboxElement1.getName());
		assertEquals("checkbox", checkboxElement1.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement1.attribute("name").getValue());
		assertEquals("checked", checkboxElement1.attribute("checked").getValue());
		assertEquals("FOO", checkboxElement1.attribute("value").getValue());
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element checkboxElement2 = (Element) spanElement2.elements().get(0);
		assertEquals("input", checkboxElement2.getName());
		assertEquals("checkbox", checkboxElement2.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement2.attribute("name").getValue());
		assertEquals("checked", checkboxElement2.attribute("checked").getValue());
		assertEquals("BAR", checkboxElement2.attribute("value").getValue());
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element checkboxElement3 = (Element) spanElement3.elements().get(0);
		assertEquals("input", checkboxElement3.getName());
		assertEquals("checkbox", checkboxElement3.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement3.attribute("name").getValue());
		assertNull("not checked", checkboxElement3.attribute("checked"));
		assertEquals("BAZ", checkboxElement3.attribute("value").getValue());
	}

	public void testWithMultiValueWithFormatter() throws Exception {
		this.tag.setPath("stringArray");
		this.tag.setItems(new Object[] {"   foo", "   bar", "   baz"});
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(this.bean, COMMAND_NAME);
		FormattingConversionService cs = new FormattingConversionService();
		cs.addFormatterForFieldType(String.class, new Formatter<String>() {
			public String print(String object, Locale locale) {
				return object;
			}
			public String parse(String text, Locale locale) throws ParseException {
				return text.trim();
			}
		});
		bindingResult.initConversion(cs);
		getPageContext().getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + COMMAND_NAME, bindingResult);

		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = (Element) document.getRootElement().elements().get(0);
		Element checkboxElement1 = (Element) spanElement1.elements().get(0);
		assertEquals("input", checkboxElement1.getName());
		assertEquals("checkbox", checkboxElement1.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement1.attribute("name").getValue());
		assertEquals("checked", checkboxElement1.attribute("checked").getValue());
		assertEquals("   foo", checkboxElement1.attribute("value").getValue());
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element checkboxElement2 = (Element) spanElement2.elements().get(0);
		assertEquals("input", checkboxElement2.getName());
		assertEquals("checkbox", checkboxElement2.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement2.attribute("name").getValue());
		assertEquals("checked", checkboxElement2.attribute("checked").getValue());
		assertEquals("   bar", checkboxElement2.attribute("value").getValue());
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element checkboxElement3 = (Element) spanElement3.elements().get(0);
		assertEquals("input", checkboxElement3.getName());
		assertEquals("checkbox", checkboxElement3.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement3.attribute("name").getValue());
		assertNull("not checked", checkboxElement3.attribute("checked"));
		assertEquals("   baz", checkboxElement3.attribute("value").getValue());
	}

	public void testCollectionOfPets() throws Exception {
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
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = (Element) document.getRootElement().elements().get(0);
		Element checkboxElement1 = (Element) spanElement1.elements().get(0);
		assertEquals("input", checkboxElement1.getName());
		assertEquals("checkbox", checkboxElement1.attribute("type").getValue());
		assertEquals("pets", checkboxElement1.attribute("name").getValue());
		assertEquals("checked", checkboxElement1.attribute("checked").getValue());
		assertEquals("Rudiger", checkboxElement1.attribute("value").getValue());
		assertEquals("RUDIGER", spanElement1.getStringValue());
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element checkboxElement2 = (Element) spanElement2.elements().get(0);
		assertEquals("input", checkboxElement2.getName());
		assertEquals("checkbox", checkboxElement2.attribute("type").getValue());
		assertEquals("pets", checkboxElement2.attribute("name").getValue());
		assertEquals("checked", checkboxElement2.attribute("checked").getValue());
		assertEquals("Spot", checkboxElement2.attribute("value").getValue());
		assertEquals("SPOT", spanElement2.getStringValue());
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element checkboxElement3 = (Element) spanElement3.elements().get(0);
		assertEquals("input", checkboxElement3.getName());
		assertEquals("checkbox", checkboxElement3.attribute("type").getValue());
		assertEquals("pets", checkboxElement3.attribute("name").getValue());
		assertNull("not checked", checkboxElement3.attribute("checked"));
		assertEquals("Checkers", checkboxElement3.attribute("value").getValue());
		assertEquals("CHECKERS", spanElement3.getStringValue());
		Element spanElement4 = (Element) document.getRootElement().elements().get(3);
		Element checkboxElement4 = (Element) spanElement4.elements().get(0);
		assertEquals("input", checkboxElement4.getName());
		assertEquals("checkbox", checkboxElement4.attribute("type").getValue());
		assertEquals("pets", checkboxElement4.attribute("name").getValue());
		assertEquals("checked", checkboxElement4.attribute("checked").getValue());
		assertEquals("Fluffy", checkboxElement4.attribute("value").getValue());
		assertEquals("FLUFFY", spanElement4.getStringValue());
		Element spanElement5 = (Element) document.getRootElement().elements().get(4);
		Element checkboxElement5 = (Element) spanElement5.elements().get(0);
		assertEquals("input", checkboxElement5.getName());
		assertEquals("checkbox", checkboxElement5.attribute("type").getValue());
		assertEquals("pets", checkboxElement5.attribute("name").getValue());
		assertEquals("checked", checkboxElement5.attribute("checked").getValue());
		assertEquals("Mufty", checkboxElement5.attribute("value").getValue());
		assertEquals("MUFTY", spanElement5.getStringValue());
	}

	/**
	 * Test case where items toString() doesn't fit the item ID
	 */
	public void testCollectionOfItemPets() throws Exception {
		this.tag.setPath("someSet");
		List allPets = new ArrayList();
		allPets.add(new ItemPet("PET1"));
		allPets.add(new ItemPet("PET2"));
		allPets.add(new ItemPet("PET3"));
		this.tag.setItems(allPets);
		this.tag.setItemValue("name");
		this.tag.setItemLabel("label");

		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = (Element) document.getRootElement().elements().get(0);
		Element checkboxElement1 = (Element) spanElement1.elements().get(0);
		assertEquals("input", checkboxElement1.getName());
		assertEquals("checkbox", checkboxElement1.attribute("type").getValue());
		assertEquals("someSet", checkboxElement1.attribute("name").getValue());
		assertNotNull("should be checked", checkboxElement1.attribute("checked"));
		assertEquals("checked", checkboxElement1.attribute("checked").getValue());
		assertEquals("PET1", checkboxElement1.attribute("value").getValue());
		assertEquals("PET1", spanElement1.getStringValue());
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element checkboxElement2 = (Element) spanElement2.elements().get(0);
		assertEquals("input", checkboxElement2.getName());
		assertEquals("checkbox", checkboxElement2.attribute("type").getValue());
		assertEquals("someSet", checkboxElement2.attribute("name").getValue());
		assertNotNull("should be checked", checkboxElement2.attribute("checked"));
		assertEquals("checked", checkboxElement2.attribute("checked").getValue());
		assertEquals("PET2", checkboxElement2.attribute("value").getValue());
		assertEquals("PET2", spanElement2.getStringValue());
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element checkboxElement3 = (Element) spanElement3.elements().get(0);
		assertEquals("input", checkboxElement3.getName());
		assertEquals("checkbox", checkboxElement3.attribute("type").getValue());
		assertEquals("someSet", checkboxElement3.attribute("name").getValue());
		assertNull("not checked", checkboxElement3.attribute("checked"));
		assertEquals("PET3", checkboxElement3.attribute("value").getValue());
		assertEquals("PET3", spanElement3.getStringValue());
	}

	public void testCollectionOfPetsWithEditor() throws Exception {
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
		assertEquals(Tag.SKIP_BODY, result);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = (Element) document.getRootElement().elements().get(0);
		Element checkboxElement1 = (Element) spanElement1.elements().get(0);
		assertEquals("input", checkboxElement1.getName());
		assertEquals("checkbox", checkboxElement1.attribute("type").getValue());
		assertEquals("pets", checkboxElement1.attribute("name").getValue());
		assertEquals("checked", checkboxElement1.attribute("checked").getValue());
		assertEquals("Rudiger", checkboxElement1.attribute("value").getValue());
		assertEquals("RUDIGER", spanElement1.getStringValue());
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element checkboxElement2 = (Element) spanElement2.elements().get(0);
		assertEquals("input", checkboxElement2.getName());
		assertEquals("checkbox", checkboxElement2.attribute("type").getValue());
		assertEquals("pets", checkboxElement2.attribute("name").getValue());
		assertEquals("checked", checkboxElement2.attribute("checked").getValue());
		assertEquals("Spot", checkboxElement2.attribute("value").getValue());
		assertEquals("SPOT", spanElement2.getStringValue());
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element checkboxElement3 = (Element) spanElement3.elements().get(0);
		assertEquals("input", checkboxElement3.getName());
		assertEquals("checkbox", checkboxElement3.attribute("type").getValue());
		assertEquals("pets", checkboxElement3.attribute("name").getValue());
		assertNull("not checked", checkboxElement3.attribute("checked"));
		assertEquals("Checkers", checkboxElement3.attribute("value").getValue());
		assertEquals("CHECKERS", spanElement3.getStringValue());
		Element spanElement4 = (Element) document.getRootElement().elements().get(3);
		Element checkboxElement4 = (Element) spanElement4.elements().get(0);
		assertEquals("input", checkboxElement4.getName());
		assertEquals("checkbox", checkboxElement4.attribute("type").getValue());
		assertEquals("pets", checkboxElement4.attribute("name").getValue());
		assertEquals("checked", checkboxElement4.attribute("checked").getValue());
		assertEquals("Fluffy", checkboxElement4.attribute("value").getValue());
		assertEquals("FLUFFY", spanElement4.getStringValue());
		Element spanElement5 = (Element) document.getRootElement().elements().get(4);
		Element checkboxElement5 = (Element) spanElement5.elements().get(0);
		assertEquals("input", checkboxElement5.getName());
		assertEquals("checkbox", checkboxElement5.attribute("type").getValue());
		assertEquals("pets", checkboxElement5.attribute("name").getValue());
		assertEquals("checked", checkboxElement5.attribute("checked").getValue());
		assertEquals("Mufty", checkboxElement5.attribute("value").getValue());
		assertEquals("MUFTY", spanElement5.getStringValue());
	}

	public void testWithNullValue() throws Exception {
		try {
			this.tag.setPath("name");
			this.tag.doStartTag();
			fail("Should not be able to render with a null value when binding to a non-boolean.");
		}
		catch (IllegalArgumentException ex) {
			// success
		}
	}

	public void testHiddenElementOmittedOnDisabled() throws Exception {
		this.tag.setPath("stringArray");
		this.tag.setItems(new Object[] {"foo", "bar", "baz"});
		this.tag.setDisabled("true");
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);
		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertEquals("Both tag and hidden element rendered incorrectly", 3, rootElement.elements().size());
		Element spanElement = (Element) document.getRootElement().elements().get(0);
		Element checkboxElement = (Element) spanElement.elements().get(0);
		assertEquals("input", checkboxElement.getName());
		assertEquals("checkbox", checkboxElement.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement.attribute("name").getValue());
		assertEquals("checked", checkboxElement.attribute("checked").getValue());
		assertEquals("disabled", checkboxElement.attribute("disabled").getValue());
		assertEquals("foo", checkboxElement.attribute("value").getValue());
	}

	public void testSpanElementCustomizable() throws Exception {
		this.tag.setPath("stringArray");
		this.tag.setItems(new Object[] {"foo", "bar", "baz"});
		this.tag.setElement("element");
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);
		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement = (Element) document.getRootElement().elements().get(0);
		assertEquals("element", spanElement.getName());
	}

	public void testDynamicTypeAttribute() throws JspException {
		try {
			this.tag.setDynamicAttribute(null, "type", "email");
			fail("Expected exception");
		}
		catch (IllegalArgumentException e) {
			assertEquals("Attribute type=\"email\" is not allowed", e.getMessage());
		}
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
		this.bean.setSomeBoolean(new Boolean(true));
		this.bean.setStringArray(new String[] {"bar", "foo"});
		this.bean.setSomeIntegerArray(new Integer[] {new Integer(2), new Integer(1)});
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

		public void setAsText(String text) {
			super.setAsText(text);
			this.allProcessedValues.add(getValue());
		}
	}


	private static class MyLowerCaseEditor extends PropertyEditorSupport {

		public void setAsText(String text) throws IllegalArgumentException {
			setValue(text.toLowerCase());
		}

		public String getAsText() {
			return ObjectUtils.nullSafeToString(getValue()).toUpperCase();
		}
	}

}
