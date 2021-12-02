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

package org.springframework.web.servlet.tags.form;

import java.beans.PropertyEditorSupport;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.Tag;
import org.dom4j.Document;
import org.dom4j.Element;
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
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Jeremy Grelle
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
class CheckboxTagTests extends AbstractFormTagTests {

	private CheckboxTag tag;

	private TestBean bean;

	@Override
	@SuppressWarnings("serial")
	protected void onSetUp() {
		this.tag = new CheckboxTag() {
			@Override
			protected TagWriter createTagWriter() {
				return new TagWriter(getWriter());
			}
		};
		this.tag.setPageContext(getPageContext());
	}

	@Test
	void withSingleValueBooleanObjectChecked() throws Exception {
		this.tag.setPath("someBoolean");
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);
		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertThat(rootElement.elements().size()).as("Both tag and hidden element not rendered").isEqualTo(2);
		Element checkboxElement = rootElement.elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("id").getValue()).isEqualTo("someBoolean1");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("someBoolean");
		assertThat(checkboxElement.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement.attribute("value").getValue()).isEqualTo("true");
	}

	@Test
	void withIndexedBooleanObjectNotChecked() throws Exception {
		this.tag.setPath("someMap[key]");
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);
		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertThat(rootElement.elements().size()).as("Both tag and hidden element not rendered").isEqualTo(2);
		Element checkboxElement = rootElement.elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("id").getValue()).isEqualTo("someMapkey1");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("someMap[key]");
		assertThat(checkboxElement.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement.attribute("value").getValue()).isEqualTo("true");
	}

	@Test
	void withSingleValueBooleanObjectCheckedAndDynamicAttributes() throws Exception {
		String dynamicAttribute1 = "attr1";
		String dynamicAttribute2 = "attr2";

		this.tag.setPath("someBoolean");
		this.tag.setDynamicAttribute(null, dynamicAttribute1, dynamicAttribute1);
		this.tag.setDynamicAttribute(null, dynamicAttribute2, dynamicAttribute2);

		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);
		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertThat(rootElement.elements().size()).as("Both tag and hidden element not rendered").isEqualTo(2);
		Element checkboxElement = rootElement.elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("someBoolean");
		assertThat(checkboxElement.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement.attribute("value").getValue()).isEqualTo("true");
		assertThat(checkboxElement.attribute(dynamicAttribute1).getValue()).isEqualTo(dynamicAttribute1);
		assertThat(checkboxElement.attribute(dynamicAttribute2).getValue()).isEqualTo(dynamicAttribute2);
	}

	@Test
	void withSingleValueBooleanChecked() throws Exception {
		this.tag.setPath("jedi");
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);
		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = document.getRootElement().elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("jedi");
		assertThat(checkboxElement.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement.attribute("value").getValue()).isEqualTo("true");
	}

	@Test
	void withSingleValueBooleanObjectUnchecked() throws Exception {
		this.bean.setSomeBoolean(Boolean.FALSE);
		this.tag.setPath("someBoolean");
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = document.getRootElement().elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("someBoolean");
		assertThat(checkboxElement.attribute("checked")).isNull();
		assertThat(checkboxElement.attribute("value").getValue()).isEqualTo("true");
	}

	@Test
	void withSingleValueBooleanUnchecked() throws Exception {
		this.bean.setJedi(false);
		this.tag.setPath("jedi");
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = document.getRootElement().elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("jedi");
		assertThat(checkboxElement.attribute("checked")).isNull();
		assertThat(checkboxElement.attribute("value").getValue()).isEqualTo("true");
	}

	@Test
	void withSingleValueNull() throws Exception {
		this.bean.setName(null);
		this.tag.setPath("name");
		this.tag.setValue("Rob Harrop");
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = document.getRootElement().elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("name");
		assertThat(checkboxElement.attribute("checked")).isNull();
		assertThat(checkboxElement.attribute("value").getValue()).isEqualTo("Rob Harrop");
	}

	@Test
	void withSingleValueNotNull() throws Exception {
		this.bean.setName("Rob Harrop");
		this.tag.setPath("name");
		this.tag.setValue("Rob Harrop");
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = document.getRootElement().elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("name");
		assertThat(checkboxElement.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement.attribute("value").getValue()).isEqualTo("Rob Harrop");
	}

	@Test
	void withSingleValueAndEditor() throws Exception {
		this.bean.setName("Rob Harrop");
		this.tag.setPath("name");
		this.tag.setValue("   Rob Harrop");
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(this.bean, COMMAND_NAME);
		bindingResult.getPropertyEditorRegistry().registerCustomEditor(String.class, new StringTrimmerEditor(false));
		getPageContext().getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + COMMAND_NAME, bindingResult);

		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = document.getRootElement().elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("name");
		assertThat(checkboxElement.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement.attribute("value").getValue()).isEqualTo("   Rob Harrop");
	}

	@Test
	void withMultiValueChecked() throws Exception {
		this.tag.setPath("stringArray");
		this.tag.setValue("foo");
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = document.getRootElement().elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement.attribute("value").getValue()).isEqualTo("foo");
	}

	@Test
	void withMultiValueUnchecked() throws Exception {
		this.tag.setPath("stringArray");
		this.tag.setValue("abc");
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = document.getRootElement().elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement.attribute("checked")).isNull();
		assertThat(checkboxElement.attribute("value").getValue()).isEqualTo("abc");
	}

	@Test
	void withMultiValueWithEditor() throws Exception {
		this.tag.setPath("stringArray");
		this.tag.setValue("   foo");
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(this.bean, COMMAND_NAME);
		MyStringTrimmerEditor editor = new MyStringTrimmerEditor();
		bindingResult.getPropertyEditorRegistry().registerCustomEditor(String.class, editor);
		getPageContext().getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + COMMAND_NAME, bindingResult);

		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);
		assertThat(editor.count).isEqualTo(1);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = document.getRootElement().elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("stringArray");
		assertThat(checkboxElement.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement.attribute("value").getValue()).isEqualTo("   foo");
	}

	@Test
	void withMultiValueIntegerWithEditor() throws Exception {
		this.tag.setPath("someIntegerArray");
		this.tag.setValue("   1");
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(this.bean, COMMAND_NAME);
		MyIntegerEditor editor = new MyIntegerEditor();
		bindingResult.getPropertyEditorRegistry().registerCustomEditor(Integer.class, editor);
		getPageContext().getRequest().setAttribute(BindingResult.MODEL_KEY_PREFIX + COMMAND_NAME, bindingResult);

		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);
		assertThat(editor.count).isEqualTo(1);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = document.getRootElement().elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("someIntegerArray");
		assertThat(checkboxElement.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement.attribute("value").getValue()).isEqualTo("   1");
	}

	@Test
	void withCollection() throws Exception {
		this.tag.setPath("someList");
		this.tag.setValue("foo");
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = document.getRootElement().elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("someList");
		assertThat(checkboxElement.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement.attribute("value").getValue()).isEqualTo("foo");
	}

	@Test
	void withObjectChecked() throws Exception {
		this.tag.setPath("date");
		this.tag.setValue(getDate());

		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = document.getRootElement().elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("date");
		assertThat(checkboxElement.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement.attribute("value").getValue()).isEqualTo(getDate().toString());
	}

	@Test
	void withObjectUnchecked() throws Exception {
		this.tag.setPath("date");
		Date date = new Date();
		this.tag.setValue(date);

		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = document.getRootElement().elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("date");
		assertThat(checkboxElement.attribute("checked")).isNull();
		assertThat(checkboxElement.attribute("value").getValue()).isEqualTo(date.toString());
	}

	@Test
	void collectionOfColoursSelected() throws Exception {
		this.tag.setPath("otherColours");
		this.tag.setValue("RED");

		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = document.getRootElement().elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("otherColours");
		assertThat(checkboxElement.attribute("checked").getValue()).isEqualTo("checked");
	}

	@Test
	void collectionOfColoursNotSelected() throws Exception {
		this.tag.setPath("otherColours");
		this.tag.setValue("PURPLE");

		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = document.getRootElement().elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("otherColours");
		assertThat(checkboxElement.attribute("checked")).isNull();
	}

	@Test
	void collectionOfPetsAsString() throws Exception {
		this.tag.setPath("pets");
		this.tag.setValue("Spot");

		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = document.getRootElement().elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("pets");
		assertThat(checkboxElement.attribute("checked").getValue()).isEqualTo("checked");
	}

	@Test
	void collectionOfPetsAsStringNotSelected() throws Exception {
		this.tag.setPath("pets");
		this.tag.setValue("Santa's Little Helper");

		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = document.getRootElement().elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("pets");
		assertThat(checkboxElement.attribute("checked")).isNull();
	}

	@Test
	void collectionOfPets() throws Exception {
		this.tag.setPath("pets");
		this.tag.setValue(new Pet("Rudiger"));

		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = document.getRootElement().elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("pets");
		assertThat(checkboxElement.attribute("value").getValue()).isEqualTo("Rudiger");
		assertThat(checkboxElement.attribute("checked").getValue()).isEqualTo("checked");
	}

	@Test
	void collectionOfPetsNotSelected() throws Exception {
		this.tag.setPath("pets");
		this.tag.setValue(new Pet("Santa's Little Helper"));

		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);

		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = document.getRootElement().elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("pets");
		assertThat(checkboxElement.attribute("value").getValue()).isEqualTo("Santa's Little Helper");
		assertThat(checkboxElement.attribute("checked")).isNull();
	}

	@Test
	void collectionOfPetsWithEditor() throws Exception {
		this.tag.setPath("pets");
		this.tag.setValue(new ItemPet("Rudiger"));

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
		Element checkboxElement = document.getRootElement().elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("pets");
		assertThat(checkboxElement.attribute("value").getValue()).isEqualTo("Rudiger");
		assertThat(checkboxElement.attribute("checked").getValue()).isEqualTo("checked");
	}

	@Test
	void withNullValue() throws Exception {
		this.tag.setPath("name");
		assertThatIllegalArgumentException().as("null value binding to a non-boolean").isThrownBy(
				this.tag::doStartTag);
	}

	@Test
	void hiddenElementOmittedOnDisabled() throws Exception {
		this.tag.setPath("someBoolean");
		this.tag.setDisabled(true);
		int result = this.tag.doStartTag();
		assertThat(result).isEqualTo(Tag.SKIP_BODY);
		String output = getOutput();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertThat(rootElement.elements().size()).as("Both tag and hidden element rendered incorrectly").isEqualTo(1);
		Element checkboxElement = rootElement.elements().get(0);
		assertThat(checkboxElement.getName()).isEqualTo("input");
		assertThat(checkboxElement.attribute("type").getValue()).isEqualTo("checkbox");
		assertThat(checkboxElement.attribute("name").getValue()).isEqualTo("someBoolean");
		assertThat(checkboxElement.attribute("checked").getValue()).isEqualTo("checked");
		assertThat(checkboxElement.attribute("value").getValue()).isEqualTo("true");
	}

	@Test
	void dynamicTypeAttribute() throws JspException {
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

		List someList = new ArrayList();
		someList.add("foo");
		someList.add("bar");

		Map someMap = new LinkedHashMap();
		someMap.put("key", Boolean.TRUE);

		this.bean = new TestBean();
		this.bean.setDate(getDate());
		this.bean.setName("Rob Harrop");
		this.bean.setJedi(true);
		this.bean.setSomeBoolean(Boolean.TRUE);
		this.bean.setStringArray(new String[] {"bar", "foo"});
		this.bean.setSomeIntegerArray(new Integer[] {2, 1});
		this.bean.setOtherColours(colours);
		this.bean.setPets(pets);
		this.bean.setSomeList(someList);
		this.bean.setSomeMap(someMap);
		return this.bean;
	}


	private class MyStringTrimmerEditor extends StringTrimmerEditor {

		public int count = 0;

		public MyStringTrimmerEditor() {
			super(false);
		}

		@Override
		public void setAsText(String text) {
			this.count++;
			super.setAsText(text);
		}
	}


	private class MyIntegerEditor extends PropertyEditorSupport {

		public int count = 0;

		@Override
		public void setAsText(String text) {
			this.count++;
			setValue(Integer.valueOf(text.trim()));
		}
	}

}
