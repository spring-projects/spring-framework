/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
 * Tests for {@link ParamTag}.
 *
 * @author Scott Andrews
 * @author Nicholas Williams
 */
class ParamTagTests extends AbstractTagTests {

	private final ParamTag tag = new ParamTag();

	private MockParamSupportTag parent = new MockParamSupportTag();

	@BeforeEach
	void setUp() {
		PageContext context = createPageContext();
		tag.setPageContext(context);
		tag.setParent(parent);
	}

	@Test
	void paramWithNameAndValue() throws JspException {
		tag.setName("name");
		tag.setValue("value");

		int action = tag.doEndTag();

		assertThat(action).isEqualTo(Tag.EVAL_PAGE);
		assertThat(parent.getParam().getName()).isEqualTo("name");
		assertThat(parent.getParam().getValue()).isEqualTo("value");
	}

	@Test
	void paramWithBodyValue() throws JspException {
		tag.setName("name");
		tag.setBodyContent(new MockBodyContent("value", new MockHttpServletResponse()));

		int action = tag.doEndTag();

		assertThat(action).isEqualTo(Tag.EVAL_PAGE);
		assertThat(parent.getParam().getName()).isEqualTo("name");
		assertThat(parent.getParam().getValue()).isEqualTo("value");
	}

	@Test
	void paramWithImplicitNullValue() throws JspException {
		tag.setName("name");

		int action = tag.doEndTag();

		assertThat(action).isEqualTo(Tag.EVAL_PAGE);
		assertThat(parent.getParam().getName()).isEqualTo("name");
		assertThat(parent.getParam().getValue()).isNull();
	}

	@Test
	void paramWithExplicitNullValue() throws JspException {
		tag.setName("name");
		tag.setValue(null);

		int action = tag.doEndTag();

		assertThat(action).isEqualTo(Tag.EVAL_PAGE);
		assertThat(parent.getParam().getName()).isEqualTo("name");
		assertThat(parent.getParam().getValue()).isNull();
	}

	@Test
	void paramWithValueThenReleaseThenBodyValue() throws JspException {
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
	void paramWithNoParent() {
		tag.setName("name");
		tag.setValue("value");
		tag.setParent(null);
		assertThatExceptionOfType(JspException.class).isThrownBy(
				tag::doEndTag);
	}

	@SuppressWarnings("serial")
	private static class MockParamSupportTag extends TagSupport implements ParamAware {

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
