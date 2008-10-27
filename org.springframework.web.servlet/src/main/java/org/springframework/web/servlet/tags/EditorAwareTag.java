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

package org.springframework.web.servlet.tags;

import java.beans.PropertyEditor;

import javax.servlet.jsp.JspException;

/**
 * Interface to be implemented by JSP tags that expose a
 * PropertyEditor for a property that they are currently bound to.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see BindTag
 * @see org.springframework.web.servlet.tags.form.AbstractDataBoundFormElementTag
 */
public interface EditorAwareTag {

	/**
	 * Retrieve the PropertyEditor for the property that this tag is
	 * currently bound to. Intended for cooperating nesting tags.
	 * @return the current PropertyEditor, or <code>null</code> if none
	 * @throws JspException if resolving the editor failed
	 */
	PropertyEditor getEditor() throws JspException;

}
