/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.context.support;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * Base class for message source implementations, providing support infrastructure
 * such as {@link java.text.MessageFormat} handling but not implementing concrete
 * methods defined in the {@link org.springframework.context.MessageSource}.
 *
 * <p>{@link AbstractMessageSource} derives from this class, providing concrete
 * {@code getMessage} implementations that delegate to a central template
 * method for message code resolution.
 *
 * @author Juergen Hoeller
 * @since 2.5.5
 */
public abstract class MessageSourceSupport {

	private static final MessageFormat INVALID_MESSAGE_FORMAT = new MessageFormat("");

	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	private boolean alwaysUseMessageFormat = false;

	/**
	 * Cache to hold already generated MessageFormats per message.
	 * Used for passed-in default messages. MessageFormats for resolved
	 * codes are cached on a specific basis in subclasses.
	 */
	private final Map<String, Map<Locale, MessageFormat>> messageFormatsPerMessage = new HashMap<>();


	/**
	 * Set whether to always apply the {@code MessageFormat} rules, parsing even
	 * messages without arguments.
	 * <p>Default is {@code false}: Messages without arguments are by default
	 * returned as-is, without parsing them through {@code MessageFormat}.
	 * Set this to {@code true} to enforce {@code MessageFormat} for all messages,
	 * expecting all message texts to be written with {@code MessageFormat} escaping.
	 * <p>For example, {@code MessageFormat} expects a single quote to be escaped
	 * as two adjacent single quotes ({@code "''"}). If your message texts are all
	 * written with such escaping, even when not defining argument placeholders,
	 * you need to set this flag to {@code true}. Otherwise, only message texts
	 * with actual arguments are supposed to be written with {@code MessageFormat}
	 * escaping.
	 * @see java.text.MessageFormat
	 */
	public void setAlwaysUseMessageFormat(boolean alwaysUseMessageFormat) {
		this.alwaysUseMessageFormat = alwaysUseMessageFormat;
	}

	/**
	 * Return whether to always apply the {@code MessageFormat} rules, parsing even
	 * messages without arguments.
	 */
	protected boolean isAlwaysUseMessageFormat() {
		return this.alwaysUseMessageFormat;
	}


	/**
	 * Render the given default message String. The default message is
	 * passed in as specified by the caller and can be rendered into
	 * a fully formatted default message shown to the user.
	 * <p>The default implementation passes the String to {@code formatMessage},
	 * resolving any argument placeholders found in them. Subclasses may override
	 * this method to plug in custom processing of default messages.
	 * @param defaultMessage the passed-in default message String
	 * @param args array of arguments that will be filled in for params within
	 * the message, or {@code null} if none.
	 * @param locale the Locale used for formatting
	 * @return the rendered default message (with resolved arguments)
	 * @see #formatMessage(String, Object[], java.util.Locale)
	 */
	protected String renderDefaultMessage(String defaultMessage, @Nullable Object[] args, Locale locale) {
		return formatMessage(defaultMessage, args, locale);
	}

	/**
	 * Format the given message String, using cached MessageFormats.
	 * By default invoked for passed-in default messages, to resolve
	 * any argument placeholders found in them.
	 * @param msg the message to format
	 * @param args array of arguments that will be filled in for params within
	 * the message, or {@code null} if none
	 * @param locale the Locale used for formatting
	 * @return the formatted message (with resolved arguments)
	 */
	protected String formatMessage(String msg, @Nullable Object[] args, Locale locale) {
		if (!isAlwaysUseMessageFormat() && ObjectUtils.isEmpty(args)) {
			return msg;
		}
		MessageFormat messageFormat = null;
		synchronized (this.messageFormatsPerMessage) {
			Map<Locale, MessageFormat> messageFormatsPerLocale = this.messageFormatsPerMessage.get(msg);
			if (messageFormatsPerLocale != null) {
				messageFormat = messageFormatsPerLocale.get(locale);
			}
			else {
				messageFormatsPerLocale = new HashMap<>();
				this.messageFormatsPerMessage.put(msg, messageFormatsPerLocale);
			}
			if (messageFormat == null) {
				try {
					messageFormat = createMessageFormat(msg, locale);
				}
				catch (IllegalArgumentException ex) {
					// Invalid message format - probably not intended for formatting,
					// rather using a message structure with no arguments involved...
					if (isAlwaysUseMessageFormat()) {
						throw ex;
					}
					// Silently proceed with raw message if format not enforced...
					messageFormat = INVALID_MESSAGE_FORMAT;
				}
				messageFormatsPerLocale.put(locale, messageFormat);
			}
		}
		if (messageFormat == INVALID_MESSAGE_FORMAT) {
			return msg;
		}
		synchronized (messageFormat) {
			return messageFormat.format(resolveArguments(args, locale));
		}
	}

	/**
	 * Create a {@code MessageFormat} for the given message and Locale.
	 * @param msg the message to create a {@code MessageFormat} for
	 * @param locale the Locale to create a {@code MessageFormat} for
	 * @return the {@code MessageFormat} instance
	 */
	protected MessageFormat createMessageFormat(String msg, Locale locale) {
		return new MessageFormat(msg, locale);
	}

	/**
	 * Template method for resolving argument objects.
	 * <p>The default implementation simply returns the given argument array as-is.
	 * Can be overridden in subclasses in order to resolve special argument types.
	 * @param args the original argument array
	 * @param locale the Locale to resolve against
	 * @return the resolved argument array
	 */
	protected Object[] resolveArguments(@Nullable Object[] args, Locale locale) {
		return (args != null ? args : new Object[0]);
	}

}
