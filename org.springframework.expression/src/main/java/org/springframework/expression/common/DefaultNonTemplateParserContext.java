package org.springframework.expression.common;

import org.springframework.expression.ParserContext;

public class DefaultNonTemplateParserContext implements ParserContext {

	public static final DefaultNonTemplateParserContext INSTANCE = new DefaultNonTemplateParserContext();

	private DefaultNonTemplateParserContext() {
	}

	public String getExpressionPrefix() {
		return null;
	}

	public String getExpressionSuffix() {
		return null;
	}

	public boolean isTemplate() {
		return false;
	}

}
