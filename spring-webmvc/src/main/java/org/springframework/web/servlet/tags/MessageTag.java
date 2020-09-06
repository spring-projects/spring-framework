/*
 * Copyright 2002-2020 the original author or authors.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.JavaScriptUtils;
import org.springframework.web.util.TagUtils;

/**
 * The {@code <message>} tag looks up a message in the scope of this page.
 * Messages are resolved using the ApplicationContext and thus support
 * internationalization.
 *
 * <p>Detects an HTML escaping setting, either on this tag instance, the page level,
 * or the {@code web.xml} level. Can also apply JavaScript escaping.
 *
 * <p>If "code" isn't set or cannot be resolved, "text" will be used as default
 * message. Thus, this tag can also be used for HTML escaping of any texts.
 *
 * <p>Message arguments can be specified via the {@link #setArguments(Object) arguments}
 * attribute or by using nested {@code <spring:argument>} tags.
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
 * <td>arguments</td>
 * <td>false</td>
 * <td>true</td>
 * <td>Set optional message arguments for this tag, as a (comma-)delimited
 * String (each String argument can contain JSP EL), an Object array (used as
 * argument array), or a single Object (used as single argument).</td>
 * </tr>
 * <tr>
 * <td>argumentSeparator</td>
 * <td>false</td>
 * <td>true</td>
 * <td>The separator character to be used for splitting the arguments string
 * value; defaults to a 'comma' (',').</td>
 * </tr>
 * <tr>
 * <td>code</td>
 * <td>false</td>
 * <td>true</td>
 * <td>The code (key) to use when looking up the message.
 * If code is not provided, the text attribute will be used.</td>
 * </tr>
 * <tr>
 * <td>htmlEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>Set HTML escaping for this tag, as boolean value.
 * Overrides the default HTML escaping setting for the current page.</td>
 * </tr>
 * <tr>
 * <td>javaScriptEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>Set JavaScript escaping for this tag, as boolean value.
 * Default is false.</td>
 * </tr>
 * <tr>
 * <td>message</td>
 * <td>false</td>
 * <td>true</td>
 * <td>A MessageSourceResolvable argument (direct or through JSP EL).
 * Fits nicely when used in conjunction with Spring’s own validation error
 * classes which all implement the MessageSourceResolvable interface.
 * For example, this allows you to iterate over all of the errors in a form,
 * passing each error (using a runtime expression) as the value of this
 * 'message' attribute, thus effecting the easy display of such error
 * messages.</td>
 * </tr>
 * <tr>
 * <td>scope</td>
 * <td>false</td>
 * <td>true</td>
 * <td>The scope to use when exporting the result to a variable. This attribute
 * is only used when var is also set. Possible values are page, request, session
 * and application.</td>
 * </tr>
 * <tr>
 * <td>text</td>
 * <td>false</td>
 * <td>true</td>
 * <td>Default text to output when a message for the given code could not be
 * found. If both text and code are not set, the tag will output null.</td>
 * </tr>
 * <tr>
 * <td>var</td>
 * <td>false</td>
 * <td>true</td>
 * <td>The string to use when binding the result to the page, request, session
 * or application scope. If not specified, the result gets outputted to the writer
 * (i.e. typically directly to the JSP).</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Nicholas Williams
 * @see #setCode
 * @see #setText
 * @see #setHtmlEscape
 * @see #setJavaScriptEscape
 * @see HtmlEscapeTag#setDefaultHtmlEscape
 * @see org.springframework.web.util.WebUtils#HTML_ESCAPE_CONTEXT_PARAM
 * @see ArgumentTag
 */
@SuppressWarnings("serial")
public class MessageTag extends HtmlEscapingAwareTag implements ArgumentAware {

	/**
	 * Default separator for splitting an arguments String: a comma (",").
	 */
	public static final String DEFAULT_ARGUMENT_SEPARATOR = ",";


	@Nullable
	private MessageSourceResolvable message;

	@Nullable
	private String code;

	@Nullable
	private Object arguments;

	private String argumentSeparator = DEFAULT_ARGUMENT_SEPARATOR;

	private List<Object> nestedArguments = Collections.emptyList();

	@Nullable
	private String text;

	@Nullable
	private String var;

	private String scope = TagUtils.SCOPE_PAGE;

	private boolean javaScriptEscape = false;


	/**
	 * Set the MessageSourceResolvable for this tag.
	 * <p>If a MessageSourceResolvable is specified, it effectively overrides
	 * any code, arguments or text specified on this tag.
	 */
	public void setMessage(MessageSourceResolvable message) {
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

	@Override
	public void addArgument(@Nullable Object argument) throws JspTagException {
		this.nestedArguments.add(argument);
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
	public void setJavaScriptEscape(boolean javaScriptEscape) throws JspException {
		this.javaScriptEscape = javaScriptEscape;
	}


	@Override
	protected final int doStartTagInternal() throws JspException, IOException {
		this.nestedArguments = new ArrayList<>();
		return EVAL_BODY_INCLUDE;
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
	public int doEndTag() throws JspException {
		try {
			// Resolve the unescaped message.
			String msg = resolveMessage();

			// HTML and/or JavaScript escape, if demanded.
			msg = htmlEscape(msg);
			msg = this.javaScriptEscape ? JavaScriptUtils.javaScriptEscape(msg) : msg;

			// Expose as variable, if demanded, else write to the page.
			if (this.var != null) {
				this.pageContext.setAttribute(this.var, msg, TagUtils.getScope(this.scope));
			}
			else {
				writeMessage(msg);
			}

			return EVAL_PAGE;
		}
		catch (IOException ex) {
			throw new JspTagException(ex.getMessage(), ex);
		}
		catch (NoSuchMessageException ex) {
			throw new JspTagException(getNoSuchMessageExceptionDescription(ex));
		}
	}

	@Override
	public void release() {
		super.release();
		this.arguments = null;
	}


	/**
	 * Resolve the specified message into a concrete message String.
	 * The returned message String should be unescaped.
	 */
	protected String resolveMessage() throws JspException, NoSuchMessageException {
		MessageSource messageSource = getMessageSource();

		// Evaluate the specified MessageSourceResolvable, if any.
		if (this.message != null) {
			// We have a given MessageSourceResolvable.
			return messageSource.getMessage(this.message, getRequestContext().getLocale());
		}

		if (this.code != null || this.text != null) {
			// We have a code or default text that we need to resolve.
			Object[] argumentsArray = resolveArguments(this.arguments);
			if (!this.nestedArguments.isEmpty()) {
				argumentsArray = appendArguments(argumentsArray, this.nestedArguments.toArray());
			}

			if (this.text != null) {
				// We have a fallback text to consider.
				String msg = messageSource.getMessage(
						this.code, argumentsArray, this.text, getRequestContext().getLocale());
				return (msg != null ? msg : "");
			}
			else {
				// We have no fallback text to consider.
				return messageSource.getMessage(
						this.code, argumentsArray, getRequestContext().getLocale());
			}
		}

		throw new JspTagException("No resolvable message");
	}

	private Object[] appendArguments(@Nullable Object[] sourceArguments, Object[] additionalArguments) {
		if (ObjectUtils.isEmpty(sourceArguments)) {
			return additionalArguments;
		}
		Object[] arguments = new Object[sourceArguments.length + additionalArguments.length];
		System.arraycopy(sourceArguments, 0, arguments, 0, sourceArguments.length);
		System.arraycopy(additionalArguments, 0, arguments, sourceArguments.length, additionalArguments.length);
		return arguments;
	}

	/**
	 * Resolve the given arguments Object into an arguments array.
	 * @param arguments the specified arguments Object
	 * @return the resolved arguments as array
	 * @throws JspException if argument conversion failed
	 * @see #setArguments
	 */
	@Nullable
	protected Object[] resolveArguments(@Nullable Object arguments) throws JspException {
		if (arguments instanceof String) {
			String[] stringArray =
					StringUtils.delimitedListToStringArray((String) arguments, this.argumentSeparator);
			if (stringArray.length == 1) {
				Object argument = stringArray[0];
				if (argument != null && argument.getClass().isArray()) {
					return ObjectUtils.toObjectArray(argument);
				}
				else {
					return new Object[] {argument};
				}
			}
			else {
				return stringArray;
			}
		}
		else if (arguments instanceof Object[]) {
			return (Object[]) arguments;
		}
		else if (arguments instanceof Collection) {
			return ((Collection<?>) arguments).toArray();
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
		this.pageContext.getOut().write(String.valueOf(msg));
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
