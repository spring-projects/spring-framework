/*
 * Copyright 2002-2007 the original author or authors.
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

import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.Tag;

import org.springframework.beans.CustomEnum;
import org.springframework.beans.GenericBean;
import org.springframework.web.servlet.support.BindStatus;

/**
 * @author Juergen Hoeller
 */
public class OptionTagEnumTests extends AbstractHtmlElementTagTests {

	private OptionTag tag;

	private SelectTag parentTag;

	protected void onSetUp() {
		this.tag = new OptionTag() {
			protected TagWriter createTagWriter() {
				return new TagWriter(getWriter());
			}
		};
		this.parentTag = new SelectTag() {
			public String getName() {
				// Should not be used other than to delegate to
				// RequestDataValueDataProcessor
				return "testName";
			}
		};
		this.tag.setParent(this.parentTag);
		this.tag.setPageContext(getPageContext());
	}

	public void testWithJavaEnum() throws Exception {
		GenericBean testBean = new GenericBean();
		testBean.setCustomEnum(CustomEnum.VALUE_1);
		getPageContext().getRequest().setAttribute("testBean", testBean);
		String selectName = "testBean.customEnum";
		getPageContext().setAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE, new BindStatus(getRequestContext(), selectName, false));

		this.tag.setValue("VALUE_1");

		int result = this.tag.doStartTag();
		assertEquals(BodyTag.EVAL_BODY_BUFFERED, result);
		result = this.tag.doEndTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getWriter().toString();

		assertOptionTagOpened(output);
		assertOptionTagClosed(output);
		assertContainsAttribute(output, "value", "VALUE_1");
		assertContainsAttribute(output, "selected", "selected");
	}

	private void assertOptionTagOpened(String output) {
		assertTrue(output.startsWith("<option"));
	}

	private void assertOptionTagClosed(String output) {
		assertTrue(output.endsWith("</option>"));
	}

}
