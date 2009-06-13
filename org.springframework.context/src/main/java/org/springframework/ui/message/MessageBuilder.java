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
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.style.ToStringCreator;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * A convenient builder for building {@link MessageResolver} objects programmatically.
 * Often used by model code such as validation logic to conveniently record validation messages.
 * Supports the production of message resolvers that hard-code their message text,
 * as well as message resolvers that retrieve their text from a {@link MessageSource}.
 * 
 * Usage example:
 * <p>
 * <pre>
 * new MessageBuilder().
 *     severity(Severity.ERROR).
 *     code(&quot;invalidFormat&quot;).
 *     arg("mathForm.decimalField").
 *     arg("#,###.##").
 *     defaultText(&quot;The decimal field must be in format #,###.##&quot;).
 *     build();
 * </pre>
 * </p>
 * @author Keith Donald
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
	 * Add a message code to use to resolve the message text.
     * Successive calls to this method add additional codes.
	 * Codes are applied in the order they are added.
	 * @param code the message code
	 * @return this, for fluent API usage
	 */
	public MessageBuilder code(String code) {
		codes.add(code);
		return this;
	}

	/**
	 * Add a message argument.
	 * Successive calls to this method add additional args.
	 * @param name the argument name
	 * @param value the argument value
	 * @return this, for fluent API usage
	 */
	public MessageBuilder arg(String name, Object value) {
		args.put(name, value);
		return this;
	}

	/**
	 * Add a message argument whose value is a resolvable message code.
	 * Successive calls to this method add additional resolvable arguements.
	 * @param name the argument name
	 * @param value the argument value
	 * @return this, for fluent API usage
	 */
	public MessageBuilder resolvableArg(String name, Object value) {
		args.put(name, new ResolvableArgumentValue(value));
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

	static class ResolvableArgumentValue implements MessageSourceResolvable {

		private Object value;

		public ResolvableArgumentValue(Object value) {
			this.value = value;
		}

		public Object[] getArguments() {
			return null;
		}

		public String[] getCodes() {
			return new String[] { value.toString() };
		}

		public String getDefaultMessage() {
			return String.valueOf(value);
		}

		public String toString() {
			return new ToStringCreator(this).append("value", value).toString();
		}

	}

}