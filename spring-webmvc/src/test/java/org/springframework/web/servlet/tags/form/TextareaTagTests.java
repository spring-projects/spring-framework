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

import jakarta.servlet.jsp.tagext.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.validation.BeanPropertyBindingResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Jeremy Grelle
 */
public class TextareaTagTests extends AbstractFormTagTests {

	private TextareaTag tag;

	private TestBean rob;

	@Override
	@SuppressWarnings("serial")
	protected void onSetUp() {
		this.tag = new TextareaTag() {
			@Override
			protected TagWriter createTagWriter() {
				return new TagWriter(getWriter());
			}
		};
		this.tag.setPageContext(getPageContext());
	}

	@Test
	public void simpleBind() throws Exception {
		this.tag.setPath("name");
		this.tag.setReadonly(true);

		assertThat(this.tag.doStartTag()).isEqualTo(Tag.SKIP_BODY);
		String output = getOutput();
		assertContainsAttribute(output, "name", "name");
		assertContainsAttribute(output, "readonly", "readonly");
		assertBlockTagContains(output, "Rob");
	}

	@Test
	public void simpleBindWithDynamicAttributes() throws Exception {
		String dynamicAttribute1 = "attr1";
		String dynamicAttribute2 = "attr2";

		this.tag.setPath("name");
		this.tag.setReadonly(true);
		this.tag.setDynamicAttribute(null, dynamicAttribute1, dynamicAttribute1);
		this.tag.setDynamicAttribute(null, dynamicAttribute2, dynamicAttribute2);

		assertThat(this.tag.doStartTag()).isEqualTo(Tag.SKIP_BODY);
		String output = getOutput();
		assertContainsAttribute(output, "name", "name");
		assertContainsAttribute(output, "readonly", "readonly");
		assertContainsAttribute(output, dynamicAttribute1, dynamicAttribute1);
		assertContainsAttribute(output, dynamicAttribute2, dynamicAttribute2);
		assertBlockTagContains(output, "Rob");
	}

	@Test
	public void complexBind() throws Exception {
		String onselect = "doSelect()";

		this.tag.setPath("spouse.name");
		this.tag.setOnselect(onselect);

		assertThat(this.tag.doStartTag()).isEqualTo(Tag.SKIP_BODY);
		String output = getOutput();
		assertContainsAttribute(output, "name", "spouse.name");
		assertContainsAttribute(output, "onselect", onselect);
		assertAttributeNotPresent(output, "readonly");
	}

	@Test
	public void simpleBindWithHtmlEscaping() throws Exception {
		final String NAME = "Rob \"I Love Mangos\" Harrop";
		final String HTML_ESCAPED_NAME = "Rob &quot;I Love Mangos&quot; Harrop";

		this.tag.setPath("name");
		this.rob.setName(NAME);

		assertThat(this.tag.doStartTag()).isEqualTo(Tag.SKIP_BODY);
		String output = getOutput();
		System.out.println(output);
		assertContainsAttribute(output, "name", "name");
		assertBlockTagContains(output, HTML_ESCAPED_NAME);
	}

	@Test
	public void customBind() throws Exception {
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(createTestBean(), "testBean");
		result.getPropertyAccessor().registerCustomEditor(Float.class, new SimpleFloatEditor());
		exposeBindingResult(result);
		this.tag.setPath("myFloat");
		assertThat(this.tag.doStartTag()).isEqualTo(Tag.SKIP_BODY);
		String output = getOutput();
		assertContainsAttribute(output, "name", "myFloat");
		assertBlockTagContains(output, "12.34f");
	}

	@Test
	public void firstNewLine() throws Exception {
		this.tag.setPath("name");
		this.tag.setReadonly(true);

		assertThat(this.tag.doStartTag()).isEqualTo(Tag.SKIP_BODY);
		String output = getOutput();
		assertBlockTagContains(output, "\r\nRob");
	}

	@Override
	protected TestBean createTestBean() {
		// set up test data
		this.rob = new TestBean();
		rob.setName("Rob");
		rob.setMyFloat(12.34f);

		TestBean sally = new TestBean();
		sally.setName("Sally");
		rob.setSpouse(sally);

		return rob;
	}

}
