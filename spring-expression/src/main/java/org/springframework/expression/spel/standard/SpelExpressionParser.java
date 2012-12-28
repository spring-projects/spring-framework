/*
 * Copyright 2002-2009 the original author or authors.
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

import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateAwareExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.util.Assert;

/**
 * SpEL parser. Instances are reusable and thread-safe.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class SpelExpressionParser extends TemplateAwareExpressionParser {

	private final SpelParserConfiguration configuration;


	/**
	 * Create a parser with standard configuration.
	 */
	public SpelExpressionParser() {
		this.configuration = new SpelParserConfiguration(false, false);
	}

	/**
	 * Create a parser with some configured behavior.
	 * @param configuration custom configuration options
	 */
	public SpelExpressionParser(SpelParserConfiguration configuration) {
		Assert.notNull(configuration, "SpelParserConfiguration must not be null");
		this.configuration = configuration;
	}


	@Override
	protected SpelExpression doParseExpression(String expressionString, ParserContext context) throws ParseException {
		return new InternalSpelExpressionParser(this.configuration).doParseExpression(expressionString, context);
	}

	public SpelExpression parseRaw(String expressionString) throws ParseException {
		return doParseExpression(expressionString, null);
	}

}
