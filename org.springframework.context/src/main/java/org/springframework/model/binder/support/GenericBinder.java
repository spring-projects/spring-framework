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

import java.util.Map;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParserConfiguration;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.model.alert.Alert;
import org.springframework.model.alert.Severity;
import org.springframework.model.binder.Binder;
import org.springframework.model.binder.BindingResult;
import org.springframework.model.binder.BindingResults;

/**
 * A {@link Binder} implementation that accepts any target object and uses
 * Spring's Expression Language support to evaluate the keys in the field
 * value Map.
 * @author Mark Fisher
 * @since 3.0
 */
public class GenericBinder extends BinderSupport implements Binder<Object> {

	private final ExpressionParser parser = new SpelExpressionParser(
			SpelExpressionParserConfiguration.CreateObjectIfAttemptToReferenceNull
					| SpelExpressionParserConfiguration.GrowListsOnIndexBeyondSize);

	public BindingResults bind(Map<String, ? extends Object> fieldValues, Object model) {
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
		evaluationContext.setRootObject(model);
		FieldBinder fieldBinder = new EvaluationContextFieldBinder(parser, evaluationContext);
		return getBindTemplate().bind(fieldValues, fieldBinder);
	}

	private static class EvaluationContextFieldBinder implements FieldBinder {

		private final ExpressionParser parser;

		private final EvaluationContext context;

		private EvaluationContextFieldBinder(ExpressionParser parser, EvaluationContext context) {
			this.parser = parser;
			this.context = context;
		}

		public BindingResult bind(String key, Object value) {
			Alert alert = null;
			try {
				Expression e = this.parser.parseExpression(key);
				e.setValue(this.context, value);
				alert = new BindSuccessAlert();
			} catch (ParseException e) {
				alert = new ParseFailureAlert(e);
			} catch (EvaluationException e) {
				alert = new EvaluationFailureAlert(e);
			}
			return new AlertBindingResult(key, value, alert);
		}
	}

	private static class BindSuccessAlert implements Alert {

		public String getCode() {
			return "bindSuccess";
		}

		public String getMessage() {
			return "Binding successful";
		}

		public Severity getSeverity() {
			return Severity.INFO;
		}
	}

	private static class ParseFailureAlert implements Alert {

		private final ParseException exception;

		ParseFailureAlert(ParseException exception) {
			this.exception = exception;
		}

		public String getCode() {
			return "parserFailed";
		}

		public String getMessage() {
			return exception.getMessage();
		}

		public Severity getSeverity() {
			return Severity.ERROR;
		}
	}

	private static class EvaluationFailureAlert implements Alert {

		private final EvaluationException exception;

		EvaluationFailureAlert(EvaluationException exception) {
			this.exception = exception;
		}

		public String getCode() {
			return "evaluationFailed";
		}

		public String getMessage() {
			return exception.getMessage();
		}

		public Severity getSeverity() {
			return Severity.ERROR;
		}
	}

}
