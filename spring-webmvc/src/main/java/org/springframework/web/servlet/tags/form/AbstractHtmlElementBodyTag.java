/*
 * Copyright 2002-present the original author or authors.
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

import java.io.IOException;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.BodyContent;
import jakarta.servlet.jsp.tagext.BodyTag;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Convenient superclass for many html tags that render content using the databinding
 * features of the {@link AbstractHtmlElementTag AbstractHtmlElementTag}. The only thing
 * sub-tags need to do is override {@link #renderDefaultContent(TagWriter)}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public abstract class AbstractHtmlElementBodyTag extends AbstractHtmlElementTag implements BodyTag {

	private @Nullable BodyContent bodyContent;

	private @Nullable TagWriter tagWriter;


	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		onWriteTagContent();
		this.tagWriter = tagWriter;
		if (shouldRender()) {
			exposeAttributes();
			return EVAL_BODY_BUFFERED;
		}
		else {
			return SKIP_BODY;
		}
	}

	/**
	 * If {@link #shouldRender rendering}, flush any buffered
	 * {@link BodyContent} or, if no {@link BodyContent} is supplied,
	 * {@link #renderDefaultContent render the default content}.
	 * @return a {@link jakarta.servlet.jsp.tagext.Tag#EVAL_PAGE} result
	 */
	@Override
	public int doEndTag() throws JspException {
		if (shouldRender()) {
			Assert.state(this.tagWriter != null, "No TagWriter set");
			if (this.bodyContent != null && StringUtils.hasText(this.bodyContent.getString())) {
				renderFromBodyContent(this.bodyContent, this.tagWriter);
			}
			else {
				renderDefaultContent(this.tagWriter);
			}
		}
		return EVAL_PAGE;
	}

	/**
	 * Render the tag contents based on the supplied {@link BodyContent}.
	 * <p>The default implementation simply {@link #flushBufferedBodyContent flushes}
	 * the {@link BodyContent} directly to the output. Subclasses may choose to
	 * override this to add additional content to the output.
	 */
	protected void renderFromBodyContent(BodyContent bodyContent, TagWriter tagWriter) throws JspException {
		flushBufferedBodyContent(bodyContent);
	}

	/**
	 * Clean up any attributes and stored resources.
	 */
	@Override
	public void doFinally() {
		super.doFinally();
		removeAttributes();
		this.tagWriter = null;
		this.bodyContent = null;
	}


	//---------------------------------------------------------------------
	// Template methods
	//---------------------------------------------------------------------

	/**
	 * Called at the start of {@link #writeTagContent} allowing subclasses to perform
	 * any precondition checks or setup tasks that might be necessary.
	 */
	protected void onWriteTagContent() {
	}

	/**
	 * Should rendering of this tag proceed at all. Returns '{@code true}' by default
	 * causing rendering to occur always, Subclasses can override this if they
	 * provide conditional rendering.
	 */
	protected boolean shouldRender() throws JspException {
		return true;
	}

	/**
	 * Called during {@link #writeTagContent} allowing subclasses to add any attributes to the
	 * {@link jakarta.servlet.jsp.PageContext} as needed.
	 */
	protected void exposeAttributes() throws JspException {
	}

	/**
	 * Called by {@link #doFinally} allowing subclasses to remove any attributes from the
	 * {@link jakarta.servlet.jsp.PageContext} as needed.
	 */
	protected void removeAttributes() {
	}

	/**
	 * The user customised the output of the error messages - flush the
	 * buffered content into the main {@link jakarta.servlet.jsp.JspWriter}.
	 */
	protected void flushBufferedBodyContent(BodyContent bodyContent) throws JspException {
		try {
			bodyContent.writeOut(bodyContent.getEnclosingWriter());
		}
		catch (IOException ex) {
			throw new JspException("Unable to write buffered body content.", ex);
		}
	}

	protected abstract void renderDefaultContent(TagWriter tagWriter) throws JspException;


	//---------------------------------------------------------------------
	// BodyTag implementation
	//---------------------------------------------------------------------

	@Override
	public void doInitBody() throws JspException {
		// no op
	}

	@Override
	public void setBodyContent(BodyContent bodyContent) {
		this.bodyContent = bodyContent;
	}

}
