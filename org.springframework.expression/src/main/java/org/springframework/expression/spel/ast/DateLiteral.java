/*
 * Copyright 2004-2008 the original author or authors.
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
package org.springframework.expression.spel.ast;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.antlr.runtime.Token;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;

/**
 * Represents a date literal value in an expression (a java.util.Date object).
 * 
 * @author Andy Clement
 * 
 */
public class DateLiteral extends SpelNode {

	private DateFormat formatter = null;
	private Date formattedDate = null;

	public DateLiteral(Token payload) {
		super(payload);
	}

	@Override
	public Date getValue(ExpressionState state) throws EvaluationException {
		if (formatter == null) {
			if (getChildCount() > 1) {
				formatter = new SimpleDateFormat((String) getChild(1).getValue(state));
			} else {
				// http://java.sun.com/j2se/1.4.2/docs/api/java/text/SimpleDateFormat.html
				// this is something of this format: "Wed, 4 Jul 2001 12:08:56 GMT"
				formatter = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.UK);
			}
		}
		String valueToParse = (String) getChild(0).getValue(state);
		try {
			formattedDate = formatter.parse(valueToParse);
		} catch (ParseException e) {
			throw new SpelException(getCharPositionInLine(), e, SpelMessages.DATE_CANNOT_BE_PARSED, valueToParse,
					(formatter instanceof SimpleDateFormat ? ((SimpleDateFormat) formatter).toLocalizedPattern()
							: formatter));
		}
		return formattedDate;
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		sb.append("date(");
		sb.append(getChild(0).toStringAST());
		if (getChildCount() > 1) {
			sb.append(",").append(getChild(1).toStringAST());
		}
		sb.append(")");
		return sb.toString();
	}

}
