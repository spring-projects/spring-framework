/*
 * Copyright 2002-2015 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.test.MockBodyContent;
import org.springframework.mock.web.test.MockHttpServletResponse;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ParamTag}.
 *
 * @author Scott Andrews
 * @author Nicholas Williams
 */
public class ParamTagTests extends AbstractTagTests {

	private final ParamTag tag = new ParamTag();

	private MockParamSupportTag parent = new MockParamSupportTag();

	@Before
	public void setUp() throws Exception {
		PageContext context = createPageContext();
		tag.setPageContext(context);
		tag.setParent(parent);
	}

	@Test
	public void paramWithNameAndValue() throws JspException {
		tag.setName("name");
		tag.setValue("value");

		int action = tag.doEndTag();

		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals("name", parent.getParam().getName());
		assertEquals("value", parent.getParam().getValue());
	}

	@Test
	public void paramWithBodyValue() throws JspException {
		tag.setName("name");
		tag.setBodyContent(new MockBodyContent("value", new MockHttpServletResponse()));

		int action = tag.doEndTag();

		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals("name", parent.getParam().getName());
		assertEquals("value", parent.getParam().getValue());
	}

	@Test
	public void paramWithImplicitNullValue() throws JspException {
		tag.setName("name");

		int action = tag.doEndTag();

		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals("name", parent.getParam().getName());
		assertNull(parent.getParam().getValue());
	}

	@Test
	public void paramWithExplicitNullValue() throws JspException {
		tag.setName("name");
		tag.setValue(null);

		int action = tag.doEndTag();

		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals("name", parent.getParam().getName());
		assertNull(parent.getParam().getValue());
	}

	@Test
	public void paramWithValueThenReleaseThenBodyValue() throws JspException {
		tag.setName("name1");
		tag.setValue("value1");

		int action = tag.doEndTag();

		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals("name1", parent.getParam().getName());
		assertEquals("value1", parent.getParam().getValue());

		tag.release();

		parent = new MockParamSupportTag();
		tag.setPageContext(createPageContext());
		tag.setParent(parent);
		tag.setName("name2");
		tag.setBodyContent(new MockBodyContent("value2", new MockHttpServletResponse()));

		action = tag.doEndTag();

		assertEquals(Tag.EVAL_PAGE, action);
		assertEquals("name2", parent.getParam().getName());
		assertEquals("value2", parent.getParam().getValue());
	}

	@Test(expected = JspException.class)
	public void paramWithNoParent() throws Exception {
		tag.setName("name");
		tag.setValue("value");
		tag.setParent(null);
		tag.doEndTag();
	}

	@SuppressWarnings("serial")
	private class MockParamSupportTag extends TagSupport implements ParamAware {

		private Param param;

		@Override
		public void addParam(Param param) {
			this.param = param;
		}

		public Param getParam() {
			return param;
		}

	}

}
