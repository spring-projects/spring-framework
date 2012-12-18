/*
 * Copyright 2002-2008 the original author or authors.
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

import java.io.IOException;
import java.util.Collection;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ExpressionEvaluationUtils;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.JavaScriptUtils;
import org.springframework.web.util.TagUtils;

/**
 * Custom JSP tag to look up a message in the scope of this page. Messages are
 * resolved using the ApplicationContext and thus support internationalization.
 *
 * <p>Detects an HTML escaping setting, either on this tag instance, the page level,
 * or the <code>web.xml</code> level. Can also apply JavaScript escaping.
 *
 * <p>If "code" isn't set or cannot be resolved, "text" will be used as default
 * message. Thus, this tag can also be used for HTML escaping of any texts.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setCode
 * @see #setText
 * @see #setHtmlEscape
 * @see #setJavaScriptEscape
 * @see HtmlEscapeTag#setDefaultHtmlEscape
 * @see org.springframework.web.util.WebUtils#HTML_ESCAPE_CONTEXT_PARAM
 */
public class MessageTag extends HtmlEscapingAwareTag {

	/**
	 * Default separator for splitting an arguments String: a comma (",")
	 */
	public static final String DEFAULT_ARGUMENT_SEPARATOR = ",";


	private Object message;

	private String code;

	private Object arguments;

	private String argumentSeparator = DEFAULT_ARGUMENT_SEPARATOR;

	private String text;

	private String var;

	private String scope = TagUtils.SCOPE_PAGE;

	private boolean javaScriptEscape = false;


	/**
	 * Set the MessageSourceResolvable for this tag.
	 * Accepts a direct MessageSourceResolvable instance as well as a JSP
	 * expression language String that points to a MessageSourceResolvable.
	 * <p>If a MessageSourceResolvable is specified, it effectively overrides
	 * any code, arguments or text specified on this tag.
	 */
	public void setMessage(Object message) {
		this.message = message;
	}

	/**
	 * Set the message code for this tag.
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * Set optional message arguments for this tag, as a comma-delimited
	 * String (each String argument can contain JSP EL), an Object array
	 * (used as argument array), or a single Object (used as single argument).
	 */
	public void setArguments(Object arguments) {
		this.arguments = arguments;
	}

	/**
	 * Set the separator to use for splitting an arguments String.
	 * Default is a comma (",").
	 * @see #setArguments
	 */
	public void setArgumentSeparator(String argumentSeparator) {
		this.argumentSeparator = argumentSeparator;
	}

	/**
	 * Set the message text for this tag.
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Set PageContext attribute name under which to expose
	 * a variable that contains the resolved message.
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

	/**
	 * Set JavaScript escaping for this tag, as boolean value.
	 * Default is "false".
	 */
	public void setJavaScriptEscape(String javaScriptEscape) throws JspException {
		this.javaScriptEscape =
				ExpressionEvaluationUtils.evaluateBoolean("javaScriptEscape", javaScriptEscape, pageContext);
	}


	/**
	 * Resolves the message, escapes it if demanded,
	 * and writes it to the page (or exposes it as variable).
	 * @see #resolveMessage()
	 * @see org.springframework.web.util.HtmlUtils#htmlEscape(String)
	 * @see org.springframework.web.util.JavaScriptUtils#javaScriptEscape(String)
	 * @see #writeMessage(String)
	 */
	@Override
	protected final int doStartTagInternal() throws JspException, IOException {
		try {
			// Resolve the unescaped message.
			String msg = resolveMessage();

			// HTML and/or JavaScript escape, if demanded.
			msg = isHtmlEscape() ? HtmlUtils.htmlEscape(msg) : msg;
			msg = this.javaScriptEscape ? JavaScriptUtils.javaScriptEscape(msg) : msg;

			// Expose as variable, if demanded, else write to the page.
			String resolvedVar = ExpressionEvaluationUtils.evaluateString("var", this.var, pageContext);
			if (resolvedVar != null) {
				String resolvedScope = ExpressionEvaluationUtils.evaluateString("scope", this.scope, pageContext);
				pageContext.setAttribute(resolvedVar, msg, TagUtils.getScope(resolvedScope));
			}
			else {
				writeMessage(msg);
			}

			return EVAL_BODY_INCLUDE;
		}
		catch (NoSuchMessageException ex) {
			throw new JspTagException(getNoSuchMessageExceptionDescription(ex));
		}
	}

	/**
	 * Resolve the specified message into a concrete message String.
	 * The returned message String should be unescaped.
	 */
	protected String resolveMessage() throws JspException, NoSuchMessageException {
		MessageSource messageSource = getMessageSource();
		if (messageSource == null) {
			throw new JspTagException("No corresponding MessageSource found");
		}

		// Evaluate the specified MessageSourceResolvable, if any.
		MessageSourceResolvable resolvedMessage = null;
		if (this.message instanceof MessageSourceResolvable) {
			resolvedMessage = (MessageSourceResolvable) this.message;
		}
		else if (this.message != null) {
			String expr = this.message.toString();
			resolvedMessage = (MessageSourceResolvable)
					ExpressionEvaluationUtils.evaluate("message", expr, MessageSourceResolvable.class, pageContext);
		}

		if (resolvedMessage != null) {
			// We have a given MessageSourceResolvable.
			return messageSource.getMessage(resolvedMessage, getRequestContext().getLocale());
		}

		String resolvedCode = ExpressionEvaluationUtils.evaluateString("code", this.code, pageContext);
		String resolvedText = ExpressionEvaluationUtils.evaluateString("text", this.text, pageContext);

		if (resolvedCode != null || resolvedText != null) {
			// We have a code or default text that we need to resolve.
			Object[] argumentsArray = resolveArguments(this.arguments);
			if (resolvedText != null) {
				// We have a fallback text to consider.
				return messageSource.getMessage(
						resolvedCode, argumentsArray, resolvedText, getRequestContext().getLocale());
			}
			else {
				// We have no fallback text to consider.
				return messageSource.getMessage(
						resolvedCode, argumentsArray, getRequestContext().getLocale());
			}
		}

		// All we have is a specified literal text.
		return resolvedText;
	}

	/**
	 * Resolve the given arguments Object into an arguments array.
	 * @param arguments the specified arguments Object
	 * @return the resolved arguments as array
	 * @throws JspException if argument conversion failed
	 * @see #setArguments
	 */
	protected Object[] resolveArguments(Object arguments) throws JspException {
		if (arguments instanceof String) {
			String[] stringArray =
					StringUtils.delimitedListToStringArray((String) arguments, this.argumentSeparator);
			if (stringArray.length == 1) {
				Object argument = ExpressionEvaluationUtils.evaluate("argument", stringArray[0], pageContext);
				if (argument != null && argument.getClass().isArray()) {
					return ObjectUtils.toObjectArray(argument);
				}
				else {
					return new Object[] {argument};
				}
			}
			else {
				Object[] argumentsArray = new Object[stringArray.length];
				for (int i = 0; i < stringArray.length; i++) {
					argumentsArray[i] =
							ExpressionEvaluationUtils.evaluate("argument[" + i + "]", stringArray[i], pageContext);
				}
				return argumentsArray;
			}
		}
		else if (arguments instanceof Object[]) {
			return (Object[]) arguments;
		}
		else if (arguments instanceof Collection) {
			return ((Collection) arguments).toArray();
		}
		else if (arguments != null) {
			// Assume a single argument object.
			return new Object[] {arguments};
		}
		else {
			return null;
		}
	}

	/**
	 * Write the message to the page.
	 * <p>Can be overridden in subclasses, e.g. for testing purposes.
	 * @param msg the message to write
	 * @throws IOException if writing failed
	 */
	protected void writeMessage(String msg) throws IOException {
		pageContext.getOut().write(String.valueOf(msg));
	}

	/**
	 * Use the current RequestContext's application context as MessageSource.
	 */
	protected MessageSource getMessageSource() {
		return getRequestContext().getMessageSource();
	}

	/**
	 * Return default exception message.
	 */
	protected String getNoSuchMessageExceptionDescription(NoSuchMessageException ex) {
		return ex.getMessage();
	}

}
