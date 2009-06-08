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
package org.springframework.ui.message.support;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.core.style.ToStringCreator;
import org.springframework.ui.message.Message;
import org.springframework.ui.message.MessageContext;
import org.springframework.ui.message.MessageResolver;
import org.springframework.ui.message.Severity;
import org.springframework.util.CachingMapDecorator;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * The default message context implementation.
 * Uses a {@link MessageSource} to resolve messages that are added by callers.
 * 
 * @author Keith Donald
 */
public class DefaultMessageContext implements MessageContext {

	private MessageSource messageSource;

	@SuppressWarnings("serial")
	private Map<String, ArrayList<Message>> messagesByElement = new CachingMapDecorator<String, ArrayList<Message>>(new LinkedHashMap<String, ArrayList<Message>>()) {
		protected ArrayList<Message> create(String element) {
			return new ArrayList<Message>();
		}
	};

	/**
	 * Creates a new default message context.
	 * Defaults to a message source that simply resolves default text and cannot resolve localized message codes.
	 */
	public DefaultMessageContext() {
		init(null);
	}

	/**
	 * Creates a new default message context.
	 * @param messageSource the message source to resolve messages added to this context
	 */
	public DefaultMessageContext(MessageSource messageSource) {
		init(messageSource);
	}

	/**
	 * The message source configured to resolve message text.
	 * @return the message source
	 */
	public MessageSource getMessageSource() {
		return messageSource;
	}

	// implementing message context

	@SuppressWarnings("unchecked")
	public Map<String, List<Message>> getMessages() {
		return Collections.unmodifiableMap(messagesByElement);
	}
	
	@SuppressWarnings("unchecked")
	public List<Message> getMessages(String element) {
		List<Message> messages = messagesByElement.get(element);
		if (messages.isEmpty()) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(messages);
	}

	public void add(MessageResolver messageResolver, String element) {
		List<Message> messages = messagesByElement.get(element);
		messages.add(new ResolvableMessage(messageResolver));
	}

	public String toString() {
		return new ToStringCreator(this).append("messagesByElement", messagesByElement).toString();
	}

	// internal helpers

	private void init(MessageSource messageSource) {
		setMessageSource(messageSource);
	}

	private void setMessageSource(MessageSource messageSource) {
		if (messageSource == null) {
			messageSource = new DefaultTextFallbackMessageSource();
		}
		this.messageSource = messageSource;
	}

	private static class DefaultTextFallbackMessageSource extends AbstractMessageSource {
		protected MessageFormat resolveCode(String code, Locale locale) {
			return null;
		}
	}
	
	class ResolvableMessage implements Message {

		private MessageResolver resolver;
		
		private Message resolvedMessage;
		
		public ResolvableMessage(MessageResolver resolver) {
			this.resolver = resolver;
		}
		
		public Severity getSeverity() {
			return getMessage().getSeverity();
		}

		public String getText() {
			return getMessage().getText();
		}

		public Message getMessage() {
			if (resolvedMessage == null) {
				resolvedMessage = resolver.resolveMessage(messageSource, LocaleContextHolder.getLocale());
			}
			return resolvedMessage;
		}
	}
}