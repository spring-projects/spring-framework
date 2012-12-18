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

package org.springframework.expression;

/**
 * Input provided to an expression parser that can influence an expression parsing/compilation routine.
 *
 * @author Keith Donald
 * @author Andy Clement
 * @since 3.0
 */
public interface ParserContext {

	/**
	 * Whether or not the expression being parsed is a template. A template expression consists of literal text that can
	 * be mixed with evaluatable blocks. Some examples:
	 *
	 * <pre>
	 * 	   Some literal text
	 *     Hello #{name.firstName}!
	 *     #{3 + 4}
	 * </pre>
	 *
	 * @return true if the expression is a template, false otherwise
	 */
	boolean isTemplate();

	/**
	 * For template expressions, returns the prefix that identifies the start of an expression block within a string.
	 * For example: "${"
	 *
	 * @return the prefix that identifies the start of an expression
	 */
	String getExpressionPrefix();

	/**
	 * For template expressions, return the prefix that identifies the end of an expression block within a string.
	 * For example: "}"
	 *
	 * @return the suffix that identifies the end of an expression
	 */
	String getExpressionSuffix();

	/**
	 * The default ParserContext implementation that enables template expression parsing mode.
	 * The expression prefix is #{ and the expression suffix is }.
	 * @see #isTemplate()
	 */
	public static final ParserContext TEMPLATE_EXPRESSION = new ParserContext() {

		public String getExpressionPrefix() {
			return "#{";
		}

		public String getExpressionSuffix() {
			return "}";
		}

		public boolean isTemplate() {
			return true;
		}

	};

}
