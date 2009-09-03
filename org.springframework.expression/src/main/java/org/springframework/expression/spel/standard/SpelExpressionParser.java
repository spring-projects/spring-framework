/*
 * Copyright 2008-2009 the original author or authors.
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
package org.springframework.expression.spel.standard;

import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateAwareExpressionParser;
import org.springframework.expression.spel.SpelExpression;
import org.springframework.expression.spel.standard.internal.InternalSpelExpressionParser;

/**
 * SpEL parser.  Instances are reusable and thread safe.
 * 
 * @author Andy Clement
 * @since 3.0
 */
public class SpelExpressionParser extends TemplateAwareExpressionParser {

	private int configuration;
	
	/**
	 * Create a parser with some configured behaviour.  Supported configuration
	 * bit flags can be seen in {@link SpelExpressionParserConfiguration}
	 * @param configuration bitflags for configuration options
	 */
	public SpelExpressionParser(int configuration) {
		this.configuration = configuration;
	}

	/**
	 * Create a parser with default behaviour.
	 */
	public SpelExpressionParser() {
		this(0);
	}

	@Override
	protected Expression doParseExpression(String expressionString, ParserContext context) throws ParseException {
		return new InternalSpelExpressionParser(configuration).doParseExpression(expressionString, context);
	}

	public SpelExpression parse(String expressionString) throws ParseException {
		return new InternalSpelExpressionParser(configuration).parse(expressionString);
	}
	
}
