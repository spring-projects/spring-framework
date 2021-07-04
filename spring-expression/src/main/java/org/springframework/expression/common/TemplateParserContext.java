/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.expression.common;

import org.springframework.expression.ParserContext;

/**
 * Configurable {@link ParserContext} implementation for template parsing. Expects the
 * expression prefix and suffix as constructor arguments.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class TemplateParserContext implements ParserContext {

	private final String expressionPrefix;

	private final String expressionSuffix;


	/**
	 * Create a new TemplateParserContext with the default "#{" prefix and "}" suffix.
	 */
	public TemplateParserContext() {
		this("#{", "}");
	}

	/**
	 * Create a new TemplateParserContext for the given prefix and suffix.
	 * @param expressionPrefix the expression prefix to use
	 * @param expressionSuffix the expression suffix to use
	 */
	public TemplateParserContext(String expressionPrefix, String expressionSuffix) {
		this.expressionPrefix = expressionPrefix;
		this.expressionSuffix = expressionSuffix;
	}


	@Override
	public final boolean isTemplate() {
		return true;
	}

	@Override
	public final String getExpressionPrefix() {
		return this.expressionPrefix;
	}

	@Override
	public final String getExpressionSuffix() {
		return this.expressionSuffix;
	}

}
