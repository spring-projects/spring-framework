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
package org.springframework.model.binder.support;

import org.springframework.context.MessageSource;
import org.springframework.context.alert.Alert;
import org.springframework.context.alert.Severity;
import org.springframework.context.expression.MapAccessor;
import org.springframework.context.message.DefaultMessageFactory;
import org.springframework.context.message.MessageBuilder;
import org.springframework.context.message.ResolvableArgument;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParserConfiguration;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.model.binder.Binder;
import org.springframework.model.binder.BindingResult;

/**
 * A {@link Binder} implementation that accepts any target object and uses
 * Spring's Expression Language (SpEL) to evaluate the keys in the field value Map.
 * @author Mark Fisher
 * @author Keith Donald
 * @since 3.0
 */
public class GenericBinder extends AbstractBinder<Object> {

	private final ExpressionParser expressionParser = new SpelExpressionParser(
			SpelExpressionParserConfiguration.CreateObjectIfAttemptToReferenceNull
					| SpelExpressionParserConfiguration.GrowListsOnIndexBeyondSize);

	@Override
	protected FieldBinder createFieldBinder(Object model) {
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.addPropertyAccessor(new MapAccessor());
		context.setRootObject(model);
		return new ExpressionFieldBinder(getMessageSource(), expressionParser, context);
	}

	private static class ExpressionFieldBinder implements FieldBinder {

		private final MessageSource messageSource;

		private final ExpressionParser expressionParser;

		private final EvaluationContext evaluationContext;

		private ExpressionFieldBinder(MessageSource messageSource, ExpressionParser expressionParser,
				EvaluationContext evaluationContext) {
			this.messageSource = messageSource;
			this.expressionParser = expressionParser;
			this.evaluationContext = evaluationContext;
		}

		public BindingResult bind(String fieldName, Object value) {
			Alert alert = null;
			try {
				Expression exp = expressionParser.parseExpression(fieldName);
				if (!exp.isWritable(evaluationContext)) {
					return new FieldNotEditableResult(fieldName, value, messageSource);
				}
				exp.setValue(evaluationContext, value);
				alert = new BindSuccessAlert(fieldName, value, messageSource);
			} catch (ParseException e) {
				alert = new InternalErrorAlert(e);
			} catch (EvaluationException e) {
				SpelEvaluationException spelException = (SpelEvaluationException) e;
				if (spelException.getMessageCode() == SpelMessage.EXCEPTION_DURING_PROPERTY_WRITE) {
					ConversionFailedException conversionFailure = findConversionFailureCause(spelException);
					if (conversionFailure != null) {
						alert = new TypeMismatchAlert(fieldName, value, conversionFailure, messageSource);						
					}
				}
				if (alert == null) {
					alert = new InternalErrorAlert(e);
				}
			}
			return new AlertBindingResult(fieldName, value, alert);
		}
		
		private ConversionFailedException findConversionFailureCause(Exception e) {
			Throwable cause = e.getCause();
			while (cause != null) {
				if (cause instanceof ConversionFailedException) {
					return (ConversionFailedException) cause;
				}
				cause = cause.getCause();
			}
			return null;
		}

	}

	private static class BindSuccessAlert implements Alert {

		private final String fieldName;

		private final Object value;

		private MessageSource messageSource;

		public BindSuccessAlert(String fieldName, Object value, MessageSource messageSource) {
			this.fieldName = fieldName;
			this.value = value;
			this.messageSource = messageSource;
		}

		public String getCode() {
			return "bindSuccess";
		}

		public String getMessage() {
			MessageBuilder builder = new MessageBuilder(messageSource);
			builder.code(getCode());
			builder.arg("label", new ResolvableArgument(fieldName));
			builder.arg("value", value);
			builder.defaultMessage(new DefaultMessageFactory() {
				public String createDefaultMessage() {
					return "Successfully bound submitted value " + value + " to field '" + fieldName + "'";
				}
			});
			return builder.build();
		}

		public Severity getSeverity() {
			return Severity.INFO;
		}
	}

	private static class TypeMismatchAlert implements Alert {

		private final String fieldName;

		private final Object value;

		private final ConversionFailedException cause;

		private MessageSource messageSource;

		public TypeMismatchAlert(String fieldName, Object value, ConversionFailedException cause,
				MessageSource messageSource) {
			this.fieldName = fieldName;
			this.value = value;
			this.cause = cause;
			this.messageSource = messageSource;
		}

		public String getCode() {
			return "typeMismatch";
		}

		public String getMessage() {
			MessageBuilder builder = new MessageBuilder(messageSource);
			builder.code(getCode());
			builder.arg("label", new ResolvableArgument(fieldName));
			builder.arg("value", value);
			builder.defaultMessage(new DefaultMessageFactory() {
				public String createDefaultMessage() {
					return "Failed to bind submitted value " + value + " to field '" + fieldName
							+ "'; value could not be converted to type [" + cause.getTargetType().getName() + "]";
				}
			});
			return builder.build();
		}

		public Severity getSeverity() {
			return Severity.ERROR;
		}
	}

	private static class InternalErrorAlert implements Alert {

		private final Exception cause;

		public InternalErrorAlert(Exception cause) {
			this.cause = cause;
		}

		public String getCode() {
			return "internalError";
		}

		public String getMessage() {
			return "An internal error occurred; message = [" + cause.getMessage() + "]";
		}

		public Severity getSeverity() {
			return Severity.FATAL;
		}
	}

}
