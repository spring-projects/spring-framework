/*
 * Copyright 2008 the original author or authors.
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

package org.springframework.web.servlet.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import org.springframework.mock.web.test.MockBodyContent;
import org.springframework.mock.web.test.MockHttpServletResponse;

/**
 * Unit tests for ParamTag
 * 
 * @author Scott Andrews
 */
public class ParamTagTests extends AbstractTagTests {

	private ParamTag tag;

	private MockParamSupportTag parent;

	@Override
	protected void setUp() throws Exception {
		PageContext context = createPageContext();
		parent = new MockParamSupportTag();
		tag = new ParamTag();
		tag.setPageContext(context);
		tag.setParent(parent);
	}

	public void testParamWithNameAndValue() throws JspException {
		tag.setName("name");
		tag.setValue("value");

		int action = tag.doEndTag();

		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals("name", parent.getParam().getName());
		assertEquals("value", parent.getParam().getValue());
	}

	public void testParamWithBodyValue() throws JspException {
		tag.setName("name");
		tag.setBodyContent(new MockBodyContent("value",
				new MockHttpServletResponse()));

		int action = tag.doEndTag();

		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals("name", parent.getParam().getName());
		assertEquals("value", parent.getParam().getValue());
	}

	public void testParamWithNullValue() throws JspException {
		tag.setName("name");

		int action = tag.doEndTag();

		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals("name", parent.getParam().getName());
		assertNull(parent.getParam().getValue());
	}

	public void testParamWithNoParent() {
		tag.setName("name");
		tag.setValue("value");

		tag.setParent(null);

		try {
			tag.doEndTag();
			fail("expected JspException");
		}
		catch (JspException e) {
			// we want this
		}
	}

	private class MockParamSupportTag extends TagSupport implements ParamAware {

		private Param param;

		public void addParam(Param param) {
			this.param = param;
		}

		public Param getParam() {
			return param;
		}

	}

}
