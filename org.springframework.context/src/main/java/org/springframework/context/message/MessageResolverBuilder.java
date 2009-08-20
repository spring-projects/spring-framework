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
package org.springframework.context.message;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Builds a {@link MessageResolver} that can resolve a localized message for display in a user interface.
 * Allows convenient specification of the codes to try to resolve the message.
 * Also supports named arguments that can inserted into a message template using eval #{expressions}.
 * <p>
 * Usage example: 
 * <pre>
 * MessageResolver resolver = new MessageResolverBuilder().
 *     code("invalidFormat").
 *     arg("label", new ResolvableArgument("mathForm.decimalField")).
 *     arg("format", "#,###.##").
 *     defaultMessage("The decimal field must be in format #,###.##").
 *     build();
 * String message = resolver.resolveMessage(messageSource, locale);
 * </pre>
 * Example messages.properties loaded by the MessageSource:
 * <pre>
 * invalidFormat=The #{label} must be in format #{format}.
 * mathForm.decimalField=Decimal Field
 * </pre>
 * @author Keith Donald
 * @since 3.0
 * @see #code(String)
 * @see #arg(String, Object)
 * @see #defaultMessage(String)
 */
public class MessageResolverBuilder {

	private Set<String> codes = new LinkedHashSet<String>();

	private Map<String, Object> args = new LinkedHashMap<String, Object>();

	private DefaultMessageFactory defaultMessageFactory;

	private ExpressionParser expressionParser = new SpelExpressionParser();
	
	/**
	 * Add a code that will be tried to lookup the message template used to create the localized message.
     * Successive calls to this method add additional codes.
	 * Codes are tried in the order they are added.
	 * @param code a message code to try
	 * @return this, for fluent API usage
	 */
	public MessageResolverBuilder code(String code) {
		codes.add(code);
		return this;
	}

	/**
	 * Add an argument to insert into the message.
	 * Named arguments are inserted by eval #{expressions} denoted within the message template.
	 * For example, the value of the 'format' argument would be inserted where a corresponding #{format} expression is defined in the message template.
	 * Successive calls to this method add additional arguments.
	 * May also add {@link ResolvableArgument resolvable arguments} whose values are resolved against the MessageSource passed to
	 * {@link MessageResolver#resolveMessage(org.springframework.context.MessageSource, java.util.Locale)}.
	 * @param name the argument name
	 * @param value the argument value
	 * @return this, for fluent API usage
	 * @see ResolvableArgument
	 */
	public MessageResolverBuilder arg(String name, Object value) {
		args.put(name, value);
		return this;
	}

	/**
	 * Set the default message.
	 * If the MessageResolver has no codes to try, this will be used as the message.
	 * If the MessageResolver has codes to try but none of those resolve to a message, this will be used as the message.
	 * @param message the default text
	 * @return this, for fluent API usage
	 */
	public MessageResolverBuilder defaultMessage(String message) {
		return defaultMessage(new StaticDefaultMessageFactory(message));
	}

	/**
	 * Set the default message.
	 * If the MessageResolver has no codes to try, this will be used as the message.
	 * If the MessageResolver has codes to try but none of those resolve to a message, this will be used as the message.
	 * @param message the default text
	 * @return this, for fluent API usage
	 */
	public MessageResolverBuilder defaultMessage(DefaultMessageFactory defaultMessageFactory) {
		this.defaultMessageFactory = defaultMessageFactory;
		return this;
	}


	/**
	 * Builds the resolver for the message.
	 * Call after recording all builder instructions.
	 * @return the built message resolver
	 * @throws IllegalStateException if no codes have been added and there is no default message
	 */
	public MessageResolver build() {
		if (codes == null && defaultMessageFactory == null) {
			throw new IllegalStateException(
					"A message code or the message text is required to build this message resolver");
		}
		String[] codesArray = (String[]) codes.toArray(new String[codes.size()]);
		return new DefaultMessageResolver(codesArray, args, defaultMessageFactory, expressionParser);
	}

}