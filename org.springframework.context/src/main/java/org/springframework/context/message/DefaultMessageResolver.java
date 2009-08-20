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

import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.style.ToStringCreator;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.support.StandardEvaluationContext;

final class DefaultMessageResolver implements MessageResolver, MessageSourceResolvable {

	private String[] codes;

	private Map<String, Object> args;

	private DefaultMessageFactory defaultMessageFactory;

	private ExpressionParser expressionParser;

	public DefaultMessageResolver(String[] codes, Map<String, Object> args,
			DefaultMessageFactory defaultMessageFactory, ExpressionParser expressionParser) {
		this.codes = codes;
		this.args = args;
		this.defaultMessageFactory = defaultMessageFactory;
		this.expressionParser = expressionParser;
	}

	// implementing MessageResolver

	public String resolveMessage(MessageSource messageSource, Locale locale) {
		if (messageSource == null) {
			if (defaultMessageFactory != null) {
				return defaultMessageFactory.createDefaultMessage();
			} else {
				throw new MessageResolutionException(
						"Unable to resolve message; MessagSource argument is null and no defaultMessage is configured");
			}
		}
		String messageString;
		try {
			messageString = messageSource.getMessage(this, locale);
		} catch (NoSuchMessageException e) {
			throw new MessageResolutionException("Unable to resolve message in" + messageSource, e);
		}
		Expression message;
		try {
			message = expressionParser.parseExpression(messageString, ParserContext.TEMPLATE_EXPRESSION);
		} catch (ParseException e) {
			throw new MessageResolutionException("Failed to parse message expression", e);
		}
		try {
			StandardEvaluationContext context = new StandardEvaluationContext();
			context.setRootObject(args);
			context.addPropertyAccessor(new MessageArgumentAccessor(messageSource, locale));
			return (String) message.getValue(context);
		} catch (EvaluationException e) {
			throw new MessageResolutionException("Failed to evaluate message expression '"
					+ message.getExpressionString() + "' to generate final message text", e);
		}
	}

	// implementing MessageSourceResolver

	public String[] getCodes() {
		return codes;
	}

	public Object[] getArguments() {
		return null;
	}

	public String getDefaultMessage() {
		return defaultMessageFactory.createDefaultMessage();
	}

	public String toString() {
		return new ToStringCreator(this).append("codes", codes).append("args", args).append("defaultMessageFactory",
				defaultMessageFactory).toString();
	}

	@SuppressWarnings("unchecked")
	static class MessageArgumentAccessor implements PropertyAccessor {

		private MessageSource messageSource;

		private Locale locale;

		public MessageArgumentAccessor(MessageSource messageSource, Locale locale) {
			this.messageSource = messageSource;
			this.locale = locale;
		}

		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			return true;
		}

		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			Map map = (Map) target;
			Object o = map.get(name);
			if (o == null) {
				throw new AccessException("No message argument named '" + name
						+ "' is defined in the argument map; arguments available are " + map.keySet(), null);
			}
			if (o instanceof MessageSourceResolvable) {
				String message = messageSource.getMessage((MessageSourceResolvable) o, locale);
				return new TypedValue(message);
			} else {
				return new TypedValue(o);
			}
		}

		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			return false;
		}

		public void write(EvaluationContext context, Object target, String name, Object newValue)
				throws AccessException {
			throw new UnsupportedOperationException("Should not be called");
		}

		public Class[] getSpecificTargetClasses() {
			return new Class[] { Map.class };
		}

	}

}