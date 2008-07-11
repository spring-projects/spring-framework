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
 * An expression variable.
 * @author Keith Donald
 */
public class ExpressionVariable {

	private String name;

	private String valueExpression;

	private ParserContext parserContext;

	/**
	 * Creates a new expression variable.
	 * @param name the name of the variable, acting as an convenient alias (required)
	 * @param valueExpression the value expression (required)
	 */
	public ExpressionVariable(String name, String valueExpression) {
		init(name, valueExpression, null);
	}

	/**
	 * Creates a new expression variable with a populated parser context.
	 * @param name the name of the variable, acting as an convenient alias (required)
	 * @param valueExpression the value expression (required)
	 * @param parserContext the parser context to use to parse the value expression (optional)
	 */
	public ExpressionVariable(String name, String valueExpression, ParserContext parserContext) {
		init(name, valueExpression, parserContext);
	}

	/**
	 * Returns the variable name.
	 * @return the variable name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the expression that will be evaluated when the variable is referenced by its name in another expression.
	 * @return the expression value.
	 */
	public String getValueExpression() {
		return valueExpression;
	}

	/**
	 * Returns the parser context to use to parse the variable's value expression.
	 * @return the value expression parser context
	 */
	public ParserContext getParserContext() {
		return parserContext;
	}

	public boolean equals(Object o) {
		if (!(o instanceof ExpressionVariable)) {
			return false;
		}
		ExpressionVariable var = (ExpressionVariable) o;
		return name.equals(var.name);
	}

	public int hashCode() {
		return name.hashCode();
	}

	public String toString() {
		return "[Expression Variable '" + name + "']";
	}

	private void init(String name, String valueExpression, ParserContext parserContext) {
		this.name = name;
		this.valueExpression = valueExpression;
		this.parserContext = parserContext;
	}

}
