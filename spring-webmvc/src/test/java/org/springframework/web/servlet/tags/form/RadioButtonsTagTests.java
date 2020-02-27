/*
 * Copyright 2002-2019 the original author or authors.
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
import java.util.Map;
import java.util.Set;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.junit.jupiter.api.Test;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.beans.testfixture.beans.Colour;
import org.springframework.beans.testfixture.beans.Pet;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Scott Andrews
 * @author Jeremy Grelle
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RadioButtonsTagTests extends AbstractFormTagTests {

	private RadioButtonsTag tag;

	private TestBean bean;

	@Override
	@SuppressWarnings("serial")
	protected void onSetUp() {
		this.tag = new RadioButtonsTag() {
			@Override
			protected TagWriter createTagWriter() {
				return new TagWriter(getWriter());
			}
		};
		this.tag.setPageContext(getPageContext());
	}

	@Test
	public void withMultiValueArray() throws Exception {
		this.tag.setPath("stringArray");
		this.tag.setItems(new Object[] {"foo", "bar", "baz"});
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";
		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = (Element) document.getRootElement().elements().get(0);
		Element radioButtonElement1 = (Element) spanElement1.elements().get(0);
		assertThat(radioButtonElement1.getName()).isEqualTo("input");
		assertThat(radioButtonElement1.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement1.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(radioButtonElement1.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(radioButtonElement1.attribute("value").getValue()).isEqualTo("foo");
		assertThat(spanElement1.getStringValue()).isEqualTo("foo");
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element radioButtonElement2 = (Element) spanElement2.elements().get(0);
		assertThat(radioButtonElement2.getName()).isEqualTo("input");
		assertThat(radioButtonElement2.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement2.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(radioButtonElement2.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(radioButtonElement2.attribute("value").getValue()).isEqualTo("bar");
		assertThat(spanElement2.getStringValue()).isEqualTo("bar");
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element radioButtonElement3 = (Element) spanElement3.elements().get(0);
		assertThat(radioButtonElement3.getName()).isEqualTo("input");
		assertThat(radioButtonElement3.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement3.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(radioButtonElement3.attribute("checked")).as("not checked").isNull();
		assertThat(radioButtonElement3.attribute("value").getValue()).isEqualTo("baz");
		assertThat(spanElement3.getStringValue()).isEqualTo("baz");
	}

	@Test
	public void withMultiValueArrayAndDynamicAttributes() throws Exception {
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
		Element spanElement1 = (Element) document.getRootElement().elements().get(0);
		Element radioButtonElement1 = (Element) spanElement1.elements().get(0);
		assertThat(radioButtonElement1.getName()).isEqualTo("input");
		assertThat(radioButtonElement1.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement1.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(radioButtonElement1.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(radioButtonElement1.attribute("value").getValue()).isEqualTo("foo");
		assertThat(spanElement1.getStringValue()).isEqualTo("foo");
		assertThat(radioButtonElement1.attribute(dynamicAttribute1).getValue()).isEqualTo(dynamicAttribute1);
		assertThat(radioButtonElement1.attribute(dynamicAttribute2).getValue()).isEqualTo(dynamicAttribute2);

		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element radioButtonElement2 = (Element) spanElement2.elements().get(0);
		assertThat(radioButtonElement2.getName()).isEqualTo("input");
		assertThat(radioButtonElement2.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement2.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(radioButtonElement2.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(radioButtonElement2.attribute("value").getValue()).isEqualTo("bar");
		assertThat(spanElement2.getStringValue()).isEqualTo("bar");
		assertThat(radioButtonElement2.attribute(dynamicAttribute1).getValue()).isEqualTo(dynamicAttribute1);
		assertThat(radioButtonElement2.attribute(dynamicAttribute2).getValue()).isEqualTo(dynamicAttribute2);

		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element radioButtonElement3 = (Element) spanElement3.elements().get(0);
		assertThat(radioButtonElement3.getName()).isEqualTo("input");
		assertThat(radioButtonElement3.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement3.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(radioButtonElement3.attribute("checked")).as("not checked").isNull();
		assertThat(radioButtonElement3.attribute("value").getValue()).isEqualTo("baz");
		assertThat(spanElement3.getStringValue()).isEqualTo("baz");
		assertThat(radioButtonElement3.attribute(dynamicAttribute1).getValue()).isEqualTo(dynamicAttribute1);
		assertThat(radioButtonElement3.attribute(dynamicAttribute2).getValue()).isEqualTo(dynamicAttribute2);

	}

	@Test
	public void withMultiValueArrayWithDelimiter() throws Exception {
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
		Element spanElement1 = (Element) document.getRootElement().elements().get(0);
		Element delimiterElement1 = spanElement1.element("br");
		assertThat(delimiterElement1).isNull();
		Element radioButtonElement1 = (Element) spanElement1.elements().get(0);
		assertThat(radioButtonElement1.getName()).isEqualTo("input");
		assertThat(radioButtonElement1.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement1.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(radioButtonElement1.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(radioButtonElement1.attribute("value").getValue()).isEqualTo("foo");
		assertThat(spanElement1.getStringValue()).isEqualTo("foo");
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element delimiterElement2 = (Element) spanElement2.elements().get(0);
		assertThat(delimiterElement2.getName()).isEqualTo("br");
		Element radioButtonElement2 = (Element) spanElement2.elements().get(1);
		assertThat(radioButtonElement2.getName()).isEqualTo("input");
		assertThat(radioButtonElement2.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement2.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(radioButtonElement2.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(radioButtonElement2.attribute("value").getValue()).isEqualTo("bar");
		assertThat(spanElement2.getStringValue()).isEqualTo("bar");
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element delimiterElement3 = (Element) spanElement3.elements().get(0);
		assertThat(delimiterElement3.getName()).isEqualTo("br");
		Element radioButtonElement3 = (Element) spanElement3.elements().get(1);
		assertThat(radioButtonElement3.getName()).isEqualTo("input");
		assertThat(radioButtonElement3.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement3.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(radioButtonElement3.attribute("checked")).as("not checked").isNull();
		assertThat(radioButtonElement3.attribute("value").getValue()).isEqualTo("baz");
		assertThat(spanElement3.getStringValue()).isEqualTo("baz");
	}

	@Test
	public void withMultiValueMap() throws Exception {
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
		Element spanElement1 = (Element) document.getRootElement().elements().get(0);
		Element radioButtonElement1 = (Element) spanElement1.elements().get(0);
		assertThat(radioButtonElement1.getName()).isEqualTo("input");
		assertThat(radioButtonElement1.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement1.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(radioButtonElement1.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(radioButtonElement1.attribute("value").getValue()).isEqualTo("foo");
		assertThat(spanElement1.getStringValue()).isEqualTo("FOO");
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element radioButtonElement2 = (Element) spanElement2.elements().get(0);
		assertThat(radioButtonElement2.getName()).isEqualTo("input");
		assertThat(radioButtonElement2.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement2.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(radioButtonElement2.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(radioButtonElement2.attribute("value").getValue()).isEqualTo("bar");
		assertThat(spanElement2.getStringValue()).isEqualTo("BAR");
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element radioButtonElement3 = (Element) spanElement3.elements().get(0);
		assertThat(radioButtonElement3.getName()).isEqualTo("input");
		assertThat(radioButtonElement3.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement3.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(radioButtonElement3.attribute("checked")).as("not checked").isNull();
		assertThat(radioButtonElement3.attribute("value").getValue()).isEqualTo("baz");
		assertThat(spanElement3.getStringValue()).isEqualTo("BAZ");
	}

	@Test
	public void withMultiValueMapWithDelimiter() throws Exception {
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
		Element spanElement1 = (Element) document.getRootElement().elements().get(0);
		Element radioButtonElement1 = (Element) spanElement1.elements().get(0);
		assertThat(radioButtonElement1.getName()).isEqualTo("input");
		assertThat(radioButtonElement1.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement1.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(radioButtonElement1.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(radioButtonElement1.attribute("value").getValue()).isEqualTo("foo");
		assertThat(spanElement1.getStringValue()).isEqualTo("FOO");
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element radioButtonElement2 = (Element) spanElement2.elements().get(0);
		assertThat(radioButtonElement2.getName()).isEqualTo("input");
		assertThat(radioButtonElement2.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement2.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(radioButtonElement2.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(radioButtonElement2.attribute("value").getValue()).isEqualTo("bar");
		assertThat(spanElement2.getStringValue()).isEqualTo((delimiter + "BAR"));
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element radioButtonElement3 = (Element) spanElement3.elements().get(0);
		assertThat(radioButtonElement3.getName()).isEqualTo("input");
		assertThat(radioButtonElement3.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement3.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(radioButtonElement3.attribute("checked")).as("not checked").isNull();
		assertThat(radioButtonElement3.attribute("value").getValue()).isEqualTo("baz");
		assertThat(spanElement3.getStringValue()).isEqualTo((delimiter + "BAZ"));
	}

	@Test
	public void withMultiValueWithEditor() throws Exception {
		this.tag.setPath("stringArray");
		this.tag.setItems(new Object[] {"   foo", "   bar", "   baz"});
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(this.bean, COMMAND_NAME);
		MyStringTrimmerEditor editor = new MyStringTrimmerEditor();
		bindingResult.getPropertyEditorRegistry().registerCustomEditor(String.class, editor);
		getPageContext().getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + COMMAND_NAME, bindingResult);

		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);
		assertThat(editor.allProcessedValues.size()).isEqualTo(3);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element spanElement1 = (Element) document.getRootElement().elements().get(0);
		Element radioButtonElement1 = (Element) spanElement1.elements().get(0);
		assertThat(radioButtonElement1.getName()).isEqualTo("input");
		assertThat(radioButtonElement1.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement1.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(radioButtonElement1.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(radioButtonElement1.attribute("value").getValue()).isEqualTo("   foo");
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element radioButtonElement2 = (Element) spanElement2.elements().get(0);
		assertThat(radioButtonElement2.getName()).isEqualTo("input");
		assertThat(radioButtonElement2.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement2.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(radioButtonElement2.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(radioButtonElement2.attribute("value").getValue()).isEqualTo("   bar");
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element radioButtonElement3 = (Element) spanElement3.elements().get(0);
		assertThat(radioButtonElement3.getName()).isEqualTo("input");
		assertThat(radioButtonElement3.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement3.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(radioButtonElement3.attribute("checked")).as("not checked").isNull();
		assertThat(radioButtonElement3.attribute("value").getValue()).isEqualTo("   baz");
	}

	@Test
	public void collectionOfPets() throws Exception {
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
		Element spanElement1 = (Element) document.getRootElement().elements().get(0);
		Element radioButtonElement1 = (Element) spanElement1.elements().get(0);
		assertThat(radioButtonElement1.getName()).isEqualTo("input");
		assertThat(radioButtonElement1.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement1.attribute("name").getValue()).isEqualTo("pets");
		assertThat(radioButtonElement1.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(radioButtonElement1.attribute("value").getValue()).isEqualTo("Rudiger");
		assertThat(spanElement1.getStringValue()).isEqualTo("RUDIGER");
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element radioButtonElement2 = (Element) spanElement2.elements().get(0);
		assertThat(radioButtonElement2.getName()).isEqualTo("input");
		assertThat(radioButtonElement2.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement2.attribute("name").getValue()).isEqualTo("pets");
		assertThat(radioButtonElement2.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(radioButtonElement2.attribute("value").getValue()).isEqualTo("Spot");
		assertThat(spanElement2.getStringValue()).isEqualTo("SPOT");
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element radioButtonElement3 = (Element) spanElement3.elements().get(0);
		assertThat(radioButtonElement3.getName()).isEqualTo("input");
		assertThat(radioButtonElement3.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement3.attribute("name").getValue()).isEqualTo("pets");
		assertThat(radioButtonElement3.attribute("checked")).as("not checked").isNull();
		assertThat(radioButtonElement3.attribute("value").getValue()).isEqualTo("Checkers");
		assertThat(spanElement3.getStringValue()).isEqualTo("CHECKERS");
		Element spanElement4 = (Element) document.getRootElement().elements().get(3);
		Element radioButtonElement4 = (Element) spanElement4.elements().get(0);
		assertThat(radioButtonElement4.getName()).isEqualTo("input");
		assertThat(radioButtonElement4.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement4.attribute("name").getValue()).isEqualTo("pets");
		assertThat(radioButtonElement4.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(radioButtonElement4.attribute("value").getValue()).isEqualTo("Fluffy");
		assertThat(spanElement4.getStringValue()).isEqualTo("FLUFFY");
		Element spanElement5 = (Element) document.getRootElement().elements().get(4);
		Element radioButtonElement5 = (Element) spanElement5.elements().get(0);
		assertThat(radioButtonElement5.getName()).isEqualTo("input");
		assertThat(radioButtonElement5.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement5.attribute("name").getValue()).isEqualTo("pets");
		assertThat(radioButtonElement5.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(radioButtonElement5.attribute("value").getValue()).isEqualTo("Mufty");
		assertThat(spanElement5.getStringValue()).isEqualTo("MUFTY");
	}

	@Test
	public void collectionOfPetsWithEditor() throws Exception {
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
		Element spanElement1 = (Element) document.getRootElement().elements().get(0);
		Element radioButtonElement1 = (Element) spanElement1.elements().get(0);
		assertThat(radioButtonElement1.getName()).isEqualTo("input");
		assertThat(radioButtonElement1.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement1.attribute("name").getValue()).isEqualTo("pets");
		assertThat(radioButtonElement1.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(radioButtonElement1.attribute("value").getValue()).isEqualTo("Rudiger");
		assertThat(spanElement1.getStringValue()).isEqualTo("RUDIGER");
		Element spanElement2 = (Element) document.getRootElement().elements().get(1);
		Element radioButtonElement2 = (Element) spanElement2.elements().get(0);
		assertThat(radioButtonElement2.getName()).isEqualTo("input");
		assertThat(radioButtonElement2.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement2.attribute("name").getValue()).isEqualTo("pets");
		assertThat(radioButtonElement2.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(radioButtonElement2.attribute("value").getValue()).isEqualTo("Spot");
		assertThat(spanElement2.getStringValue()).isEqualTo("SPOT");
		Element spanElement3 = (Element) document.getRootElement().elements().get(2);
		Element radioButtonElement3 = (Element) spanElement3.elements().get(0);
		assertThat(radioButtonElement3.getName()).isEqualTo("input");
		assertThat(radioButtonElement3.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement3.attribute("name").getValue()).isEqualTo("pets");
		assertThat(radioButtonElement3.attribute("checked")).as("not checked").isNull();
		assertThat(radioButtonElement3.attribute("value").getValue()).isEqualTo("Checkers");
		assertThat(spanElement3.getStringValue()).isEqualTo("CHECKERS");
		Element spanElement4 = (Element) document.getRootElement().elements().get(3);
		Element radioButtonElement4 = (Element) spanElement4.elements().get(0);
		assertThat(radioButtonElement4.getName()).isEqualTo("input");
		assertThat(radioButtonElement4.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement4.attribute("name").getValue()).isEqualTo("pets");
		assertThat(radioButtonElement4.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(radioButtonElement4.attribute("value").getValue()).isEqualTo("Fluffy");
		assertThat(spanElement4.getStringValue()).isEqualTo("FLUFFY");
		Element spanElement5 = (Element) document.getRootElement().elements().get(4);
		Element radioButtonElement5 = (Element) spanElement5.elements().get(0);
		assertThat(radioButtonElement5.getName()).isEqualTo("input");
		assertThat(radioButtonElement5.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement5.attribute("name").getValue()).isEqualTo("pets");
		assertThat(radioButtonElement5.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(radioButtonElement5.attribute("value").getValue()).isEqualTo("Mufty");
		assertThat(spanElement5.getStringValue()).isEqualTo("MUFTY");
	}

	@Test
	public void withoutItemsEnumBindTarget() throws Exception {
		BeanWithEnum testBean = new BeanWithEnum();
		testBean.setTestEnum(TestEnum.VALUE_2);
		getPageContext().getRequest().setAttribute("testBean", testBean);

		this.tag.setPath("testEnum");
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = "<div>" + getOutput() + "</div>";
		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();

		assertThat(rootElement.elements().size()).isEqualTo(2);
		Node value1 = rootElement.selectSingleNode("//input[@value = 'VALUE_1']");
		Node value2 = rootElement.selectSingleNode("//input[@value = 'VALUE_2']");
		assertThat(rootElement.selectSingleNode("//label[@for = '" + value1.valueOf("@id") + "']").getText()).isEqualTo("TestEnum: VALUE_1");
		assertThat(rootElement.selectSingleNode("//label[@for = '" + value2.valueOf("@id") + "']").getText()).isEqualTo("TestEnum: VALUE_2");
		assertThat(rootElement.selectSingleNode("//input[@checked]")).isEqualTo(value2);
	}

	@Test
	public void withoutItemsEnumBindTargetWithExplicitLabelsAndValues() throws Exception {
		BeanWithEnum testBean = new BeanWithEnum();
		testBean.setTestEnum(TestEnum.VALUE_2);
		getPageContext().getRequest().setAttribute("testBean", testBean);

		this.tag.setPath("testEnum");
		this.tag.setItemLabel("enumLabel");
		this.tag.setItemValue("enumValue");
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = "<div>" + getOutput() + "</div>";
		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();

		assertThat(rootElement.elements().size()).isEqualTo(2);
		Node value1 = rootElement.selectSingleNode("//input[@value = 'Value: VALUE_1']");
		Node value2 = rootElement.selectSingleNode("//input[@value = 'Value: VALUE_2']");
		assertThat(rootElement.selectSingleNode("//label[@for = '" + value1.valueOf("@id") + "']").getText()).isEqualTo("Label: VALUE_1");
		assertThat(rootElement.selectSingleNode("//label[@for = '" + value2.valueOf("@id") + "']").getText()).isEqualTo("Label: VALUE_2");
		assertThat(rootElement.selectSingleNode("//input[@checked]")).isEqualTo(value2);
	}

	@Test
	public void withNullValue() throws Exception {
		this.tag.setPath("name");
		assertThatIllegalArgumentException().as("null value when binding to a non-boolean").isThrownBy(
				this.tag::doStartTag);
	}

	@Test
	public void hiddenElementOmittedOnDisabled() throws Exception {
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
		assertThat(rootElement.elements().size()).as("Both tag and hidden element rendered incorrectly").isEqualTo(3);
		Element spanElement = (Element) document.getRootElement().elements().get(0);
		Element radioButtonElement = (Element) spanElement.elements().get(0);
		assertThat(radioButtonElement.getName()).isEqualTo("input");
		assertThat(radioButtonElement.attribute("type").getValue()).isEqualTo("radio");
		assertThat(radioButtonElement.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(radioButtonElement.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(radioButtonElement.attribute("disabled").getValue()).isEqualTo("disabled");
		assertThat(radioButtonElement.attribute("value").getValue()).isEqualTo("foo");
	}

	@Test
	public void spanElementCustomizable() throws Exception {
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
		Element spanElement = (Element) document.getRootElement().elements().get(0);
		assertThat(spanElement.getName()).isEqualTo("element");
	}

	@Test
	public void dynamicTypeAttribute() throws JspException {
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

		this.bean = new TestBean();
		this.bean.setDate(getDate());
		this.bean.setName("Rob Harrop");
		this.bean.setJedi(true);
		this.bean.setSomeBoolean(Boolean.TRUE);
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

		@Override
		public void setAsText(String text) {
			super.setAsText(text);
			this.allProcessedValues.add(getValue());
		}
	}

}
