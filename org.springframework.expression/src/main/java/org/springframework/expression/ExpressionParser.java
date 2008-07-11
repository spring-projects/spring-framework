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
package org.springframework.binding.expression;

/**
 * Parses expression strings into compiled expressions that can be evaluated. Supports parsing templates as well as
 * standard expression strings.
 * 
 * @author Keith Donald
 */
public interface ExpressionParser {

	/**
	 * Parse the expression string and return a compiled Expression object you can use for evaluation. Some examples:
	 * 
	 * <pre>
	 *     3 + 4
	 *     name.firstName
	 * </pre>
	 * 
	 * @param expressionString the raw expression string to parse
	 * @param context a context for influencing this expression parsing routine (optional)
	 * @return an evaluator for the parsed expression
	 * @throws ParserException an exception occurred during parsing
	 */
	public Expression parseExpression(String expressionString, ParserContext context) throws ParserException;

}