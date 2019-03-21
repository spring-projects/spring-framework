/*
 * Copyright 2002-2018 the original author or authors.
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

import javax.servlet.jsp.JspException;

/**
 * Databinding-aware JSP tag for rendering an HTML '{@code textarea}'.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class TextareaTag extends AbstractHtmlInputElementTag {

	public static final String ROWS_ATTRIBUTE = "rows";

	public static final String COLS_ATTRIBUTE = "cols";

	public static final String ONSELECT_ATTRIBUTE = "onselect";

	@Deprecated
	public static final String READONLY_ATTRIBUTE = "readonly";


	private String rows;

	private String cols;

	private String onselect;


	/**
	 * Set the value of the '{@code rows}' attribute.
	 * May be a runtime expression.
	 */
	public void setRows(String rows) {
		this.rows = rows;
	}

	/**
	 * Get the value of the '{@code rows}' attribute.
	 */
	protected String getRows() {
		return this.rows;
	}

	/**
	 * Set the value of the '{@code cols}' attribute.
	 * May be a runtime expression.
	 */
	public void setCols(String cols) {
		this.cols = cols;
	}

	/**
	 * Get the value of the '{@code cols}' attribute.
	 */
	protected String getCols() {
		return this.cols;
	}

	/**
	 * Set the value of the '{@code onselect}' attribute.
	 * May be a runtime expression.
	 */
	public void setOnselect(String onselect) {
		this.onselect = onselect;
	}

	/**
	 * Get the value of the '{@code onselect}' attribute.
	 */
	protected String getOnselect() {
		return this.onselect;
	}


	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		tagWriter.startTag("textarea");
		writeDefaultAttributes(tagWriter);
		writeOptionalAttribute(tagWriter, ROWS_ATTRIBUTE, getRows());
		writeOptionalAttribute(tagWriter, COLS_ATTRIBUTE, getCols());
		writeOptionalAttribute(tagWriter, ONSELECT_ATTRIBUTE, getOnselect());
		String value = getDisplayString(getBoundValue(), getPropertyEditor());
		tagWriter.appendValue("\r\n" + processFieldValue(getName(), value, "textarea"));
		tagWriter.endTag();
		return SKIP_BODY;
	}

}
