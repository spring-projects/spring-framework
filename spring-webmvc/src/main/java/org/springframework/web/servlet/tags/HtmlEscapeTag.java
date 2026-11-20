/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.tags;

import jakarta.servlet.jsp.JspException;

/**
 * The {@code <htmlEscape>} tag sets default HTML escape value for the current
 * page. The actual value can be overridden by escaping-aware tags.
 * The default is "false".
 *
 * <p>Note: You can also set a "defaultHtmlEscape" web.xml context-param.
 * A page-level setting overrides a context-param.
 *
 * <table>
 * <caption>Attribute Summary</caption>
 * <thead>
 * <tr>
 * <th>Attribute</th>
 * <th>Required?</th>
 * <th>Runtime Expression?</th>
 * <th>Description</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>defaultHtmlEscape</td>
 * <td>true</td>
 * <td>true</td>
 * <td>Set the default value for HTML escaping, to be put into the current
 * PageContext.</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Juergen Hoeller
 * @since 04.03.2003
 * @see HtmlEscapingAwareTag#setHtmlEscape
 */
@SuppressWarnings("serial")
public class HtmlEscapeTag extends RequestContextAwareTag {

	private boolean defaultHtmlEscape;


	/**
	 * Set the default value for HTML escaping,
	 * to be put into the current PageContext.
	 */
	public void setDefaultHtmlEscape(boolean defaultHtmlEscape) {
		this.defaultHtmlEscape = defaultHtmlEscape;
	}


	@Override
	protected int doStartTagInternal() throws JspException {
		getRequestContext().setDefaultHtmlEscape(this.defaultHtmlEscape);
		return EVAL_BODY_INCLUDE;
	}

}
