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

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.style.ToStringCreator;

class DefaultMessageResolver implements MessageResolver, MessageSourceResolvable {

	private Severity severity;

	private String[] codes;

	private Object[] args;

	private String defaultText;

	public DefaultMessageResolver(Severity severity, String[] codes, Object[] args, String defaultText) {
		this.severity = severity;
		this.codes = codes;
		this.args = args;
		this.defaultText = defaultText;
	}
	
	// implementing MessageResolver

	public Message resolveMessage(MessageSource messageSource, Locale locale) {
		String text = messageSource.getMessage(this, locale);
		return new TextMessage(severity, text);
	}

	// implementing MessageSourceResolver

	public String[] getCodes() {
		return codes;
	}

	public Object[] getArguments() {
		return args;
	}

	public String getDefaultMessage() {
		return defaultText;
	}

	public String toString() {
		return new ToStringCreator(this).append("severity", severity).append("codes", codes).append("args", args).append("defaultText", defaultText).toString();
	}
	
	static class TextMessage implements Message {

		private Severity severity;
		
		private String text;
		
		public TextMessage(Severity severity, String text) {
			this.severity = severity;
			this.text = text;
		}
		
		public Severity getSeverity() {
			return severity;
		}

		public String getText() {
			return text;
		}
		
	}
	
}