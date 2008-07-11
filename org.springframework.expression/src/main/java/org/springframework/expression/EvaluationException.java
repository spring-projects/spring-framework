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
 * Indicates an expression evaluation failed.
 * 
 * @author Keith Donald
 */
public class EvaluationException extends RuntimeException {

	private Class contextClass;

	private String expressionString;

	/**
	 * Creates a new evaluation exception.
	 * @param contextClass the class of object upon which evaluation was attempted
	 * @param expressionString the string form of the expression that failed to evaluate
	 * @param message the exception message
	 */
	public EvaluationException(Class contextClass, String expressionString, String message) {
		this(contextClass, expressionString, message, null);
	}

	/**
	 * Creates a new evaluation exception.
	 * @param contextClass the class of object upon which evaluation was attempted
	 * @param expressionString the string form of the expression that failed to evaluate
	 * @param message the exception message
	 * @param cause the underlying cause of this evaluation exception
	 */
	public EvaluationException(Class contextClass, String expressionString, String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * The class of object upon which evaluation was attempted and failed.
	 * @return the context class
	 */
	public Class getContextClass() {
		return contextClass;
	}

	/**
	 * The string form of the expression that failed to evaluate against an instance of the the context class.
	 * @return the expression string
	 */
	public String getExpressionString() {
		return expressionString;
	}

}