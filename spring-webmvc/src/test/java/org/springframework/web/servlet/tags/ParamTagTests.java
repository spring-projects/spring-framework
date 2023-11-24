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

package org.springframework.web.servlet.tags;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.Tag;
import jakarta.servlet.jsp.tagext.TagSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockBodyContent;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link ParamTag}.
 *
 * @author Scott Andrews
 * @author Nicholas Williams
 */
public class ParamTagTests extends AbstractTagTests {

	private final ParamTag tag = new ParamTag();

	private MockParamSupportTag parent = new MockParamSupportTag();

	@BeforeEach
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

		assertThat(action).isEqualTo(Tag.EVAL_PAGE);
		assertThat(parent.getParam().getName()).isEqualTo("name");
		assertThat(parent.getParam().getValue()).isEqualTo("value");
	}

	@Test
	public void paramWithBodyValue() throws JspException {
		tag.setName("name");
		tag.setBodyContent(new MockBodyContent("value", new MockHttpServletResponse()));

		int action = tag.doEndTag();

		assertThat(action).isEqualTo(Tag.EVAL_PAGE);
		assertThat(parent.getParam().getName()).isEqualTo("name");
		assertThat(parent.getParam().getValue()).isEqualTo("value");
	}

	@Test
	public void paramWithImplicitNullValue() throws JspException {
		tag.setName("name");

		int action = tag.doEndTag();

		assertThat(action).isEqualTo(Tag.EVAL_PAGE);
		assertThat(parent.getParam().getName()).isEqualTo("name");
		assertThat(parent.getParam().getValue()).isNull();
	}

	@Test
	public void paramWithExplicitNullValue() throws JspException {
		tag.setName("name");
		tag.setValue(null);

		int action = tag.doEndTag();

		assertThat(action).isEqualTo(Tag.EVAL_PAGE);
		assertThat(parent.getParam().getName()).isEqualTo("name");
		assertThat(parent.getParam().getValue()).isNull();
	}

	@Test
	public void paramWithValueThenReleaseThenBodyValue() throws JspException {
		tag.setName("name1");
		tag.setValue("value1");

		int action = tag.doEndTag();

		assertThat(action).isEqualTo(Tag.EVAL_PAGE);
		assertThat(parent.getParam().getName()).isEqualTo("name1");
		assertThat(parent.getParam().getValue()).isEqualTo("value1");

		tag.release();

		parent = new MockParamSupportTag();
		tag.setPageContext(createPageContext());
		tag.setParent(parent);
		tag.setName("name2");
		tag.setBodyContent(new MockBodyContent("value2", new MockHttpServletResponse()));

		action = tag.doEndTag();

		assertThat(action).isEqualTo(Tag.EVAL_PAGE);
		assertThat(parent.getParam().getName()).isEqualTo("name2");
		assertThat(parent.getParam().getValue()).isEqualTo("value2");
	}

	@Test
	public void paramWithNoParent() throws Exception {
		tag.setName("name");
		tag.setValue("value");
		tag.setParent(null);
		assertThatExceptionOfType(JspException.class).isThrownBy(
				tag::doEndTag);
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
