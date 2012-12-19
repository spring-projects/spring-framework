/*
 * Copyright 2002-2008 the original author or authors.
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.beans.Colour;
import org.springframework.beans.Pet;
import org.springframework.beans.TestBean;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

/**
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Scott Andrews
 * @author Jeremy Grelle
 */
public final class RadioButtonsTagTests extends AbstractFormTagTests {

	private RadioButtonsTag tag;

	private TestBean bean;

	@SuppressWarnings("serial")
	protected void onSetUp() {
		this.tag = new RadioButtonsTag() {
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
		Element radioButtonElement1 = (Element) spanElement1.elements().get(0);
		assertEquals("input", radioButtonElement1.getName());
		assertEquals("radio", radioButtonElement1.attribute("type").getValue());
		assertEquals("stringArray", radioButtonElement1.attribute("name").getValue());
		assertEquals("checked", radioButtonElement1.attribute("checked").getValue());
		assertEquals("foo", radioButtonElement1.attribute("value").getValue());
		assertEquals("foo", spanElement1.getStringValue());
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element radioButtonElement2 = (Element) spanElement2.elements().get(0);
		assertEquals("input", radioButtonElement2.getName());
		assertEquals("radio", radioButtonElement2.attribute("type").getValue());
		assertEquals("stringArray", radioButtonElement2.attribute("name").getValue());
		assertEquals("checked", radioButtonElement2.attribute("checked").getValue());
		assertEquals("bar", radioButtonElement2.attribute("value").getValue());
		assertEquals("bar", spanElement2.getStringValue());
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element radioButtonElement3 = (Element) spanElement3.elements().get(0);
		assertEquals("input", radioButtonElement3.getName());
		assertEquals("radio", radioButtonElement3.attribute("type").getValue());
		assertEquals("stringArray", radioButtonElement3.attribute("name").getValue());
		assertNull("not checked", radioButtonElement3.attribute("checked"));
		assertEquals("baz", radioButtonElement3.attribute("value").getValue());
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
		Element radioButtonElement1 = (Element) spanElement1.elements().get(0);
		assertEquals("input", radioButtonElement1.getName());
		assertEquals("radio", radioButtonElement1.attribute("type").getValue());
		assertEquals("stringArray", radioButtonElement1.attribute("name").getValue());
		assertEquals("checked", radioButtonElement1.attribute("checked").getValue());
		assertEquals("foo", radioButtonElement1.attribute("value").getValue());
		assertEquals("foo", spanElement1.getStringValue());
		assertEquals(dynamicAttribute1, radioButtonElement1.attribute(dynamicAttribute1).getValue());
		assertEquals(dynamicAttribute2, radioButtonElement1.attribute(dynamicAttribute2).getValue());

		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element radioButtonElement2 = (Element) spanElement2.elements().get(0);
		assertEquals("input", radioButtonElement2.getName());
		assertEquals("radio", radioButtonElement2.attribute("type").getValue());
		assertEquals("stringArray", radioButtonElement2.attribute("name").getValue());
		assertEquals("checked", radioButtonElement2.attribute("checked").getValue());
		assertEquals("bar", radioButtonElement2.attribute("value").getValue());
		assertEquals("bar", spanElement2.getStringValue());
		assertEquals(dynamicAttribute1, radioButtonElement2.attribute(dynamicAttribute1).getValue());
		assertEquals(dynamicAttribute2, radioButtonElement2.attribute(dynamicAttribute2).getValue());

		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element radioButtonElement3 = (Element) spanElement3.elements().get(0);
		assertEquals("input", radioButtonElement3.getName());
		assertEquals("radio", radioButtonElement3.attribute("type").getValue());
		assertEquals("stringArray", radioButtonElement3.attribute("name").getValue());
		assertNull("not checked", radioButtonElement3.attribute("checked"));
		assertEquals("baz", radioButtonElement3.attribute("value").getValue());
		assertEquals("baz", spanElement3.getStringValue());
		assertEquals(dynamicAttribute1, radioButtonElement3.attribute(dynamicAttribute1).getValue());
		assertEquals(dynamicAttribute2, radioButtonElement3.attribute(dynamicAttribute2).getValue());

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
		Element radioButtonElement1 = (Element) spanElement1.elements().get(0);
		assertEquals("input", radioButtonElement1.getName());
		assertEquals("radio", radioButtonElement1.attribute("type").getValue());
		assertEquals("stringArray", radioButtonElement1.attribute("name").getValue());
		assertEquals("checked", radioButtonElement1.attribute("checked").getValue());
		assertEquals("foo", radioButtonElement1.attribute("value").getValue());
		assertEquals("foo", spanElement1.getStringValue());
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element delimiterElement2 = (Element) spanElement2.elements().get(0);
		assertEquals("br", delimiterElement2.getName());
		Element radioButtonElement2 = (Element) spanElement2.elements().get(1);
		assertEquals("input", radioButtonElement2.getName());
		assertEquals("radio", radioButtonElement2.attribute("type").getValue());
		assertEquals("stringArray", radioButtonElement2.attribute("name").getValue());
		assertEquals("checked", radioButtonElement2.attribute("checked").getValue());
		assertEquals("bar", radioButtonElement2.attribute("value").getValue());
		assertEquals("bar", spanElement2.getStringValue());
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element delimiterElement3 = (Element) spanElement3.elements().get(0);
		assertEquals("br", delimiterElement3.getName());
		Element radioButtonElement3 = (Element) spanElement3.elements().get(1);
		assertEquals("input", radioButtonElement3.getName());
		assertEquals("radio", radioButtonElement3.attribute("type").getValue());
		assertEquals("stringArray", radioButtonElement3.attribute("name").getValue());
		assertNull("not checked", radioButtonElement3.attribute("checked"));
		assertEquals("baz", radioButtonElement3.attribute("value").getValue());
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
		Element radioButtonElement1 = (Element) spanElement1.elements().get(0);
		assertEquals("input", radioButtonElement1.getName());
		assertEquals("radio", radioButtonElement1.attribute("type").getValue());
		assertEquals("stringArray", radioButtonElement1.attribute("name").getValue());
		assertEquals("checked", radioButtonElement1.attribute("checked").getValue());
		assertEquals("foo", radioButtonElement1.attribute("value").getValue());
		assertEquals("FOO", spanElement1.getStringValue());
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element radioButtonElement2 = (Element) spanElement2.elements().get(0);
		assertEquals("input", radioButtonElement2.getName());
		assertEquals("radio", radioButtonElement2.attribute("type").getValue());
		assertEquals("stringArray", radioButtonElement2.attribute("name").getValue());
		assertEquals("checked", radioButtonElement2.attribute("checked").getValue());
		assertEquals("bar", radioButtonElement2.attribute("value").getValue());
		assertEquals("BAR", spanElement2.getStringValue());
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element radioButtonElement3 = (Element) spanElement3.elements().get(0);
		assertEquals("input", radioButtonElement3.getName());
		assertEquals("radio", radioButtonElement3.attribute("type").getValue());
		assertEquals("stringArray", radioButtonElement3.attribute("name").getValue());
		assertNull("not checked", radioButtonElement3.attribute("checked"));
		assertEquals("baz", radioButtonElement3.attribute("value").getValue());
		assertEquals("BAZ", spanElement3.getStringValue());
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
		Element radioButtonElement1 = (Element) spanElement1.elements().get(0);
		assertEquals("input", radioButtonElement1.getName());
		assertEquals("radio", radioButtonElement1.attribute("type").getValue());
		assertEquals("stringArray", radioButtonElement1.attribute("name").getValue());
		assertEquals("checked", radioButtonElement1.attribute("checked").getValue());
		assertEquals("foo", radioButtonElement1.attribute("value").getValue());
		assertEquals("FOO", spanElement1.getStringValue());
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element radioButtonElement2 = (Element) spanElement2.elements().get(0);
		assertEquals("input", radioButtonElement2.getName());
		assertEquals("radio", radioButtonElement2.attribute("type").getValue());
		assertEquals("stringArray", radioButtonElement2.attribute("name").getValue());
		assertEquals("checked", radioButtonElement2.attribute("checked").getValue());
		assertEquals("bar", radioButtonElement2.attribute("value").getValue());
		assertEquals(delimiter + "BAR", spanElement2.getStringValue());
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element radioButtonElement3 = (Element) spanElement3.elements().get(0);
		assertEquals("input", radioButtonElement3.getName());
		assertEquals("radio", radioButtonElement3.attribute("type").getValue());
		assertEquals("stringArray", radioButtonElement3.attribute("name").getValue());
		assertNull("not checked", radioButtonElement3.attribute("checked"));
		assertEquals("baz", radioButtonElement3.attribute("value").getValue());
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
		Element radioButtonElement1 = (Element) spanElement1.elements().get(0);
		assertEquals("input", radioButtonElement1.getName());
		assertEquals("radio", radioButtonElement1.attribute("type").getValue());
		assertEquals("stringArray", radioButtonElement1.attribute("name").getValue());
		assertEquals("checked", radioButtonElement1.attribute("checked").getValue());
		assertEquals("   foo", radioButtonElement1.attribute("value").getValue());
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element radioButtonElement2 = (Element) spanElement2.elements().get(0);
		assertEquals("input", radioButtonElement2.getName());
		assertEquals("radio", radioButtonElement2.attribute("type").getValue());
		assertEquals("stringArray", radioButtonElement2.attribute("name").getValue());
		assertEquals("checked", radioButtonElement2.attribute("checked").getValue());
		assertEquals("   bar", radioButtonElement2.attribute("value").getValue());
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element radioButtonElement3 = (Element) spanElement3.elements().get(0);
		assertEquals("input", radioButtonElement3.getName());
		assertEquals("radio", radioButtonElement3.attribute("type").getValue());
		assertEquals("stringArray", radioButtonElement3.attribute("name").getValue());
		assertNull("not checked", radioButtonElement3.attribute("checked"));
		assertEquals("   baz", radioButtonElement3.attribute("value").getValue());
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
		Element radioButtonElement1 = (Element) spanElement1.elements().get(0);
		assertEquals("input", radioButtonElement1.getName());
		assertEquals("radio", radioButtonElement1.attribute("type").getValue());
		assertEquals("pets", radioButtonElement1.attribute("name").getValue());
		assertEquals("checked", radioButtonElement1.attribute("checked").getValue());
		assertEquals("Rudiger", radioButtonElement1.attribute("value").getValue());
		assertEquals("RUDIGER", spanElement1.getStringValue());
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element radioButtonElement2 = (Element) spanElement2.elements().get(0);
		assertEquals("input", radioButtonElement2.getName());
		assertEquals("radio", radioButtonElement2.attribute("type").getValue());
		assertEquals("pets", radioButtonElement2.attribute("name").getValue());
		assertEquals("checked", radioButtonElement2.attribute("checked").getValue());
		assertEquals("Spot", radioButtonElement2.attribute("value").getValue());
		assertEquals("SPOT", spanElement2.getStringValue());
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element radioButtonElement3 = (Element) spanElement3.elements().get(0);
		assertEquals("input", radioButtonElement3.getName());
		assertEquals("radio", radioButtonElement3.attribute("type").getValue());
		assertEquals("pets", radioButtonElement3.attribute("name").getValue());
		assertNull("not checked", radioButtonElement3.attribute("checked"));
		assertEquals("Checkers", radioButtonElement3.attribute("value").getValue());
		assertEquals("CHECKERS", spanElement3.getStringValue());
		Element spanElement4 = (Element) document.getRootElement().elements().get(3);
		Element radioButtonElement4 = (Element) spanElement4.elements().get(0);
		assertEquals("input", radioButtonElement4.getName());
		assertEquals("radio", radioButtonElement4.attribute("type").getValue());
		assertEquals("pets", radioButtonElement4.attribute("name").getValue());
		assertEquals("checked", radioButtonElement4.attribute("checked").getValue());
		assertEquals("Fluffy", radioButtonElement4.attribute("value").getValue());
		assertEquals("FLUFFY", spanElement4.getStringValue());
		Element spanElement5 = (Element) document.getRootElement().elements().get(4);
		Element radioButtonElement5 = (Element) spanElement5.elements().get(0);
		assertEquals("input", radioButtonElement5.getName());
		assertEquals("radio", radioButtonElement5.attribute("type").getValue());
		assertEquals("pets", radioButtonElement5.attribute("name").getValue());
		assertEquals("checked", radioButtonElement5.attribute("checked").getValue());
		assertEquals("Mufty", radioButtonElement5.attribute("value").getValue());
		assertEquals("MUFTY", spanElement5.getStringValue());
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
		Element radioButtonElement1 = (Element) spanElement1.elements().get(0);
		assertEquals("input", radioButtonElement1.getName());
		assertEquals("radio", radioButtonElement1.attribute("type").getValue());
		assertEquals("pets", radioButtonElement1.attribute("name").getValue());
		assertEquals("checked", radioButtonElement1.attribute("checked").getValue());
		assertEquals("Rudiger", radioButtonElement1.attribute("value").getValue());
		assertEquals("RUDIGER", spanElement1.getStringValue());
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element radioButtonElement2 = (Element) spanElement2.elements().get(0);
		assertEquals("input", radioButtonElement2.getName());
		assertEquals("radio", radioButtonElement2.attribute("type").getValue());
		assertEquals("pets", radioButtonElement2.attribute("name").getValue());
		assertEquals("checked", radioButtonElement2.attribute("checked").getValue());
		assertEquals("Spot", radioButtonElement2.attribute("value").getValue());
		assertEquals("SPOT", spanElement2.getStringValue());
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element radioButtonElement3 = (Element) spanElement3.elements().get(0);
		assertEquals("input", radioButtonElement3.getName());
		assertEquals("radio", radioButtonElement3.attribute("type").getValue());
		assertEquals("pets", radioButtonElement3.attribute("name").getValue());
		assertNull("not checked", radioButtonElement3.attribute("checked"));
		assertEquals("Checkers", radioButtonElement3.attribute("value").getValue());
		assertEquals("CHECKERS", spanElement3.getStringValue());
		Element spanElement4 = (Element) document.getRootElement().elements().get(3);
		Element radioButtonElement4 = (Element) spanElement4.elements().get(0);
		assertEquals("input", radioButtonElement4.getName());
		assertEquals("radio", radioButtonElement4.attribute("type").getValue());
		assertEquals("pets", radioButtonElement4.attribute("name").getValue());
		assertEquals("checked", radioButtonElement4.attribute("checked").getValue());
		assertEquals("Fluffy", radioButtonElement4.attribute("value").getValue());
		assertEquals("FLUFFY", spanElement4.getStringValue());
		Element spanElement5 = (Element) document.getRootElement().elements().get(4);
		Element radioButtonElement5 = (Element) spanElement5.elements().get(0);
		assertEquals("input", radioButtonElement5.getName());
		assertEquals("radio", radioButtonElement5.attribute("type").getValue());
		assertEquals("pets", radioButtonElement5.attribute("name").getValue());
		assertEquals("checked", radioButtonElement5.attribute("checked").getValue());
		assertEquals("Mufty", radioButtonElement5.attribute("value").getValue());
		assertEquals("MUFTY", spanElement5.getStringValue());
	}

	public void testWithoutItemsEnumBindTarget() throws Exception {
		BeanWithEnum testBean = new BeanWithEnum();
		testBean.setTestEnum(TestEnum.VALUE_2);
		getPageContext().getRequest().setAttribute("testBean", testBean);

		this.tag.setPath("testEnum");
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = "<div>" + getOutput() + "</div>";
		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();

		assertEquals(2, rootElement.elements().size());
		Node value1 = rootElement.selectSingleNode("//input[@value = 'VALUE_1']");
		Node value2 = rootElement.selectSingleNode("//input[@value = 'VALUE_2']");
		assertEquals("TestEnum: VALUE_1", rootElement.selectSingleNode("//label[@for = '" + value1.valueOf("@id") + "']").getText());
		assertEquals("TestEnum: VALUE_2", rootElement.selectSingleNode("//label[@for = '" + value2.valueOf("@id") + "']").getText());
		assertEquals(value2, rootElement.selectSingleNode("//input[@checked]"));
	}

	public void testWithoutItemsEnumBindTargetWithExplicitLabelsAndValues() throws Exception {
		BeanWithEnum testBean = new BeanWithEnum();
		testBean.setTestEnum(TestEnum.VALUE_2);
		getPageContext().getRequest().setAttribute("testBean", testBean);

		this.tag.setPath("testEnum");
		this.tag.setItemLabel("enumLabel");
		this.tag.setItemValue("enumValue");
		int result = this.tag.doStartTag();
		assertEquals(Tag.SKIP_BODY, result);

		String output = "<div>" + getOutput() + "</div>";
		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();

		assertEquals(2, rootElement.elements().size());
		Node value1 = rootElement.selectSingleNode("//input[@value = 'Value: VALUE_1']");
		Node value2 = rootElement.selectSingleNode("//input[@value = 'Value: VALUE_2']");
		assertEquals("Label: VALUE_1", rootElement.selectSingleNode("//label[@for = '" + value1.valueOf("@id") + "']").getText());
		assertEquals("Label: VALUE_2", rootElement.selectSingleNode("//label[@for = '" + value2.valueOf("@id") + "']").getText());
		assertEquals(value2, rootElement.selectSingleNode("//input[@checked]"));
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
		Element radioButtonElement = (Element) spanElement.elements().get(0);
		assertEquals("input", radioButtonElement.getName());
		assertEquals("radio", radioButtonElement.attribute("type").getValue());
		assertEquals("stringArray", radioButtonElement.attribute("name").getValue());
		assertEquals("checked", radioButtonElement.attribute("checked").getValue());
		assertEquals("disabled", radioButtonElement.attribute("disabled").getValue());
		assertEquals("foo", radioButtonElement.attribute("value").getValue());
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

		this.bean = new TestBean();
		this.bean.setDate(getDate());
		this.bean.setName("Rob Harrop");
		this.bean.setJedi(true);
		this.bean.setSomeBoolean(new Boolean(true));
		this.bean.setStringArray(new String[] {"bar", "foo"});
		this.bean.setSomeIntegerArray(new Integer[] {new Integer(2), new Integer(1)});
		this.bean.setOtherColours(colours);
		this.bean.setPets(pets);
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

}
