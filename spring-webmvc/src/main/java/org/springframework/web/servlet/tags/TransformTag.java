/*
 * Copyright 2002-2017 the original author or authors.
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

import java.beans.PropertyEditor;
import java.io.IOException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.springframework.lang.Nullable;
import org.springframework.web.util.TagUtils;

/**
 * The {@code <transform>} tag provides transformation for reference data values
 * from controllers and other objects inside a {@code spring:bind} tag (or a
 * data-bound form element tag from Spring's form tag library).
 *
 * <p>The BindTag has a PropertyEditor that it uses to transform properties of
 * a bean to a String, usable in HTML forms. This tag uses that PropertyEditor
 * to transform objects passed into this tag.
 *
 * <table>
 * <caption>Attribute Summary</caption>
 * <thead>
 * <tr>
 * <th class="colFirst">Attribute</th>
 * <th class="colOne">Required?</th>
 * <th class="colOne">Runtime Expression?</th>
 * <th class="colLast">Description</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr class="altColor">
 * <td>htmlEscape</p></td>
 * <td>false</p></td>
 * <td>true</p></td>
 * <td>Set HTML escaping for this tag, as boolean value. Overrides the default HTML
 * escaping setting for the current page.</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td>scope</p></td>
 * <td>false</p></td>
 * <td>true</p></td>
 * <td>The scope to use when exported the result to a variable. This attribute
 * is only used when var is also set. Possible values are page, request, session
 * and application.</p></td>
 * </tr>
 * <tr class="altColor">
 * <td>value</p></td>
 * <td>true</p></td>
 * <td>true</p></td>
 * <td>The value to transform. This is the actual object you want to have
 * transformed (for instance a Date). Using the PropertyEditor that is currently
 * in use by the 'spring:bind' tag.</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td>var</p></td>
 * <td>false</p></td>
 * <td>true</p></td>
 * <td>The string to use when binding the result to the page, request, session
 * or application scope. If not specified, the result gets outputted to the
 * writer (i.e. typically directly to the JSP).</p></td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @since 20.09.2003
 * @see BindTag
 */
@SuppressWarnings("serial")
public class TransformTag extends HtmlEscapingAwareTag {

	/** the value to transform using the appropriate property editor */
	@Nullable
	private Object value;

	/** the variable to put the result in */
	@Nullable
	private String var;

	/** the scope of the variable the result will be put in */
	private String scope = TagUtils.SCOPE_PAGE;


	/**
	 * Set the value to transform, using the appropriate PropertyEditor
	 * from the enclosing BindTag.
	 * <p>The value can either be a plain value to transform (a hard-coded String
	 * value in a JSP or a JSP expression), or a JSP EL expression to be evaluated
	 * (transforming the result of the expression).
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * Set PageContext attribute name under which to expose
	 * a variable that contains the result of the transformation.
	 * @see #setScope
	 * @see javax.servlet.jsp.PageContext#setAttribute
	 */
	public void setVar(String var) {
		this.var = var;
	}

	/**
	 * Set the scope to export the variable to.
	 * Default is SCOPE_PAGE ("page").
	 * @see #setVar
	 * @see org.springframework.web.util.TagUtils#SCOPE_PAGE
	 * @see javax.servlet.jsp.PageContext#setAttribute
	 */
	public void setScope(String scope) {
		this.scope = scope;
	}


	@Override
	protected final int doStartTagInternal() throws JspException {
		if (this.value != null) {
			// Find the containing EditorAwareTag (e.g. BindTag), if applicable.
			EditorAwareTag tag = (EditorAwareTag) TagSupport.findAncestorWithClass(this, EditorAwareTag.class);
			if (tag == null) {
				throw new JspException("TransformTag can only be used within EditorAwareTag (e.g. BindTag)");
			}

			// OK, let's obtain the editor...
			String result = null;
			PropertyEditor editor = tag.getEditor();
			if (editor != null) {
				// If an editor was found, edit the value.
				editor.setValue(this.value);
				result = editor.getAsText();
			}
			else {
				// Else, just do a toString.
				result = this.value.toString();
			}
			result = htmlEscape(result);
			if (this.var != null) {
				pageContext.setAttribute(this.var, result, TagUtils.getScope(this.scope));
			}
			else {
				try {
					// Else, just print it out.
					pageContext.getOut().print(result);
				}
				catch (IOException ex) {
					throw new JspException(ex);
				}
			}
		}

		return SKIP_BODY;
	}

}
