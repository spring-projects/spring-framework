package org.springframework.expression.common;

import org.springframework.expression.ParserContext;

public class DefaultTemplateParserContext implements ParserContext {

	public static final DefaultTemplateParserContext INSTANCE = new DefaultTemplateParserContext();

	private DefaultTemplateParserContext() {
	}

	public String getExpressionPrefix() {
		return "${";
	}

	public String getExpressionSuffix() {
		return "}";
	}

	public boolean isTemplate() {
		return true;
	}

}
