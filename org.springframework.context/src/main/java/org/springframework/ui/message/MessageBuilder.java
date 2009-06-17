/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.ui.message;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.context.MessageSource;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * A builder for building {@link MessageResolver} objects.
 * Typically used by Controllers to {@link MessageContext#add(MessageResolver, String) add} messages to display in a user interface.
 * Supports MessageResolvers that hard-code the message text, as well as MessageResolvers that resolve the message text from a localized {@link MessageSource}.
 * Also supports named arguments whose values can be inserted into messages using #{eval expressions}.
 * <p>
 * Usage example: 
 * <pre>
 * new MessageBuilder().
 *     severity(Severity.ERROR).
 *     code("invalidFormat").
 *     arg("label", new LocalizedArgumentValue("mathForm.decimalField")).
 *     arg("format", "#,###.##").
 *     defaultText("The decimal field must be in format #,###.##").
 *     build();
 * </pre>
 * Example messages.properties loaded by the MessageSource:
 * <pre>
 * invalidFormat=The #{label} must be in format #{format}.
 * mathForm.decimalField=Decimal Field
 * </pre>
 * @author Keith Donald
 * @since 3.0 
 * @see MessageContext#add(MessageResolver, String)
 */
public class MessageBuilder {

	private Severity severity;
	
	private Set<String> codes = new LinkedHashSet<String>();

	private Map<String, Object> args = new LinkedHashMap<String, Object>();

	private String defaultText;

	private ExpressionParser expressionParser = new SpelExpressionParser();
	
	/**
	 * Set the severity of the message.
	 * @return this, for fluent API usage
	 */
	public MessageBuilder severity(Severity severity) {
		this.severity = severity;
		return this;
	}
	
	/**
	 * Add a code to use to resolve the template for generating the localized message text.
     * Successive calls to this method add additional codes.
	 * Codes are tried in the order they are added.
	 * @param code the message code
	 * @return this, for fluent API usage
	 */
	public MessageBuilder code(String code) {
		codes.add(code);
		return this;
	}

	/**
	 * Add a message argument to insert into the message text.
	 * Named message arguments are inserted by eval expressions denoted within the resolved message template.
	 * For example, the value of the 'format' argument would be inserted where a corresponding #{format} expression is defined in the message template.
	 * Successive calls to this method add additional arguments.
	 * May also add {@link ResolvableArgument resolvable arguments} whose values are resolved against the MessageSource passed to the {@link MessageResolver}.
	 * @param name the argument name
	 * @param value the argument value
	 * @return this, for fluent API usage
	 * @see ResolvableArgument
	 */
	public MessageBuilder arg(String name, Object value) {
		args.put(name, value);
		return this;
	}

	/**
	 * Set the fallback text for the message.
	 * If the message has no codes, this will always be used as the text.
	 * If the message has codes but none can be resolved, this will always be used as the text.
	 * @param text the default text
	 * @return this, for fluent API usage
	 */
	public MessageBuilder defaultText(String text) {
		defaultText = text;
		return this;
	}

	/**
	 * Builds the message that will be resolved.
	 * Call after recording all builder instructions.
	 * @return the built message resolver
	 * @throws Illegal
	 */
	public MessageResolver build() {
		if (severity == null) {
			severity = Severity.INFO;
		}
		if (codes == null && defaultText == null) {
			throw new IllegalStateException(
					"A message code or the message text is required to build this message resolver");
		}
		String[] codesArray = (String[]) codes.toArray(new String[codes.size()]);
		return new DefaultMessageResolver(severity, codesArray, args, defaultText, expressionParser);
	}

}