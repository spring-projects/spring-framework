/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel.standard;

import org.jspecify.annotations.Nullable;

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
 * @author Sam Brannen
 * @since 3.0
 */
public class SpelExpressionParser extends TemplateAwareExpressionParser {

	private final SpelParserConfiguration configuration;


	/**
	 * Create a parser with default settings.
	 */
	public SpelExpressionParser() {
		this.configuration = SpelParserConfiguration.withDefaults();
	}

	/**
	 * Create a parser with the specified configuration.
	 * @param configuration custom configuration options
	 */
	public SpelExpressionParser(SpelParserConfiguration configuration) {
		Assert.notNull(configuration, "SpelParserConfiguration must not be null");
		this.configuration = configuration;
	}


	public SpelExpression parseRaw(String expressionString) throws ParseException {
		Assert.hasText(expressionString, "'expressionString' must not be null or blank");
		return doParseExpression(expressionString, null);
	}

	@Override
	protected SpelExpression doParseExpression(String expressionString, @Nullable ParserContext context) throws ParseException {
		return new InternalSpelExpressionParser(this.configuration).doParseExpression(expressionString, context);
	}

}
