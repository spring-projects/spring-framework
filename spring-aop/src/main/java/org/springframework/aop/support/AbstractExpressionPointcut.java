/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.aop.support;

import java.io.Serializable;

import org.springframework.lang.Nullable;

/**
 * Abstract superclass for expression pointcuts,
 * offering location and expression properties.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @since 2.0
 * @see #setLocation
 * @see #setExpression
 */
@SuppressWarnings("serial")
public abstract class AbstractExpressionPointcut implements ExpressionPointcut, Serializable {

	@Nullable
	private String location;

	@Nullable
	private String expression;


	/**
	 * Set the location for debugging.
	 */
	public void setLocation(@Nullable String location) {
		this.location = location;
	}

	/**
	 * Return location information about the pointcut expression
	 * if available. This is useful in debugging.
	 * @return location information as a human-readable String,
	 * or {@code null} if none is available
	 */
	@Nullable
	public String getLocation() {
		return this.location;
	}

	public void setExpression(@Nullable String expression) {
		this.expression = expression;
		try {
			onSetExpression(expression);
		}
		catch (IllegalArgumentException ex) {
			// Fill in location information if possible.
			if (this.location != null) {
				throw new IllegalArgumentException("Invalid expression at location [" + this.location + "]: " + ex);
			}
			else {
				throw ex;
			}
		}
	}

	/**
	 * Called when a new pointcut expression is set.
	 * The expression should be parsed at this point if possible.
	 * <p>This implementation is empty.
	 * @param expression the expression to set
	 * @throws IllegalArgumentException if the expression is invalid
	 * @see #setExpression
	 */
	protected void onSetExpression(@Nullable String expression) throws IllegalArgumentException {
	}

	/**
	 * Return this pointcut's expression.
	 */
	@Override
	@Nullable
	public String getExpression() {
		return this.expression;
	}

}
