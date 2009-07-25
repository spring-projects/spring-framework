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
package org.springframework.model.message;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Builds a localized message for display in a user interface.
 * Allows convenient specification of the codes to try to resolve the message.
 * Also supports named arguments that can inserted into a message template using eval #{expressions}.
 * <p>
 * Usage example: 
 * <pre>
 * String message = new MessageBuilder(messageSource).
 *     code("invalidFormat").
 *     arg("label", new ResolvableArgument("mathForm.decimalField")).
 *     arg("format", "#,###.##").
 *     defaultMessage("The decimal field must be in format #,###.##").
 *     build();
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
 * @see #locale(Locale)
 */
public class MessageBuilder {

	private MessageSource messageSource;
	
	private Locale locale;

	private MessageResolverBuilder builder = new MessageResolverBuilder();
	
	/**
	 * Create a new MessageBuilder that builds messages from message templates defined in the MessageSource
	 * @param messageSource the message source
	 */
	public MessageBuilder(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
	
	/**
	 * Add a code that will be tried to lookup the message template used to create the localized message.
     * Successive calls to this method add additional codes.
	 * Codes are tried in the order they are added.
	 * @param code a message code to try
	 * @return this, for fluent API usage
	 */
	public MessageBuilder code(String code) {
		builder.code(code);
		return this;
	}

	/**
	 * Add an argument to insert into the message.
	 * Named arguments are inserted by eval #{expressions} denoted within the message template.
	 * For example, the value of the 'format' argument would be inserted where a corresponding #{format} expression is defined in the message template.
	 * Successive calls to this method add additional arguments.
	 * May also add {@link ResolvableArgument resolvable arguments} whose values are resolved against the MessageSource.
	 * @param name the argument name
	 * @param value the argument value
	 * @return this, for fluent API usage
	 * @see ResolvableArgument
	 */
	public MessageBuilder arg(String name, Object value) {
		builder.arg(name, value);
		return this;
	}

	/**
	 * Set the default message.
	 * If there are no codes to try, this will be used as the message.
	 * If there are codes to try but none of those resolve to a message, this will be used as the message.
	 * @param message the default text
	 * @return this, for fluent API usage
	 */
	public MessageBuilder defaultMessage(String message) {
		builder.defaultMessage(message);
		return this;
	}
	
	/**
	 * Set the message locale.
	 * If not set, the default locale the Locale of the current request obtained from {@link LocaleContextHolder#getLocale()}.
	 * @param message the locale
	 * @return this, for fluent API usage
	 */
	public MessageBuilder locale(Locale locale) {
		this.locale = locale;
		return this;
	}

	/**
	 * Builds the resolver for the message.
	 * Call after recording all builder instructions.
	 * @return the built message resolver
	 * @throws IllegalStateException if no codes have been added and there is no default message
	 */
	public String build() {
		return builder.build().resolveMessage(messageSource, getLocale());
	}

	// internal helpers
	
	private Locale getLocale() {
		if (locale != null) {
			return locale;
		} else {
			return LocaleContextHolder.getLocale();
		}
	}

}