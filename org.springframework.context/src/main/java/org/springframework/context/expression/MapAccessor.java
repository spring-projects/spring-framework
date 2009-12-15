/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.context.expression;

import java.util.Map;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

/**
 * EL property accessor that knows how to traverse the keys
 * of a standard {@link java.util.Map}.
 *
 * @author Juergen Hoeller
 * @author Andy Clement
 * @since 3.0
 */
public class MapAccessor implements PropertyAccessor {

	public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
		return (((Map) target).containsKey(name));
	}

	public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
		return new TypedValue(((Map) target).get(name));
	}

	public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
		return true;
	}

	@SuppressWarnings("unchecked")
	public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
		((Map) target).put(name, newValue);
	}

	public Class[] getSpecificTargetClasses() {
		return new Class[] { Map.class };
	}
	
}
