/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.tags;

import java.beans.PropertyEditor;

import jakarta.servlet.jsp.JspException;
import org.jspecify.annotations.Nullable;

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
	 * @return the current PropertyEditor, or {@code null} if none
	 * @throws JspException if resolving the editor failed
	 */
	@Nullable PropertyEditor getEditor() throws JspException;

}
