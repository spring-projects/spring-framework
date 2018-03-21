/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.expression.spel.support;

import java.lang.reflect.Method;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.lang.Nullable;

/**
 * A simple {@link org.springframework.expression.PropertyAccessor} variant that
 * uses reflection to access properties for reading and possibly also writing.
 *
 * <p>A property can be accessed through a public getter method (when being read)
 * or a public setter method (when being written), and also as a public field.
 *
 * <p>This accessor is explicitly designed for user-level property evaluation
 * and does not resolve technical properties on {@code java.lang.Object}.
 * For more resolution power, choose {@link ReflectivePropertyAccessor} instead.
 *
 * @author Juergen Hoeller
 * @since 4.3.15
 * @see SimpleEvaluationContext
 * @see StandardEvaluationContext
 * @see ReflectivePropertyAccessor
 */
public class SimplePropertyAccessor extends ReflectivePropertyAccessor {

	private final boolean allowWrite;


	/**
	 * Create a new property accessor for reading as well writing.
	 * @see #SimplePropertyAccessor(boolean)
	 */
	public SimplePropertyAccessor() {
		this.allowWrite = true;
	}

	/**
	 * Create a new property accessor for reading and possibly also writing.
	 * @param allowWrite whether to also allow for write operations
	 * @see #canWrite
	 */
	public SimplePropertyAccessor(boolean allowWrite) {
		this.allowWrite = allowWrite;
	}


	@Override
	public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
		return (this.allowWrite && super.canWrite(context, target, name));
	}

	@Override
	public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue)
			throws AccessException {

		if (!this.allowWrite) {
			throw new AccessException("PropertyAccessor for property '" + name +
					"' on target [" + target + "] does not allow write operations");
		}
		super.write(context, target, name, newValue);
	}

	@Override
	protected boolean isCandidateForProperty(Method method) {
		return (method.getDeclaringClass() != Object.class);
	}

}
