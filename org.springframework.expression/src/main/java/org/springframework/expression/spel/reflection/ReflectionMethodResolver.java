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
package org.springframework.expression.spel.reflection;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.spel.reflection.ReflectionUtils.DiscoveredMethod;

/**
 * A method resolver that uses reflection to locate the method that should be invoked
 * 
 * @author Andy Clement
 */
public class ReflectionMethodResolver implements MethodResolver {

	/*
	 * Indicates if this resolve will allow matches to be found that require some of the input arguments to be
	 * transformed by the conversion service.
	 */
	private boolean allowMatchesRequiringArgumentConversion = true;

	public ReflectionMethodResolver() {
	}

	public ReflectionMethodResolver(boolean allowMatchesRequiringArgumentConversion) {
		this.allowMatchesRequiringArgumentConversion = allowMatchesRequiringArgumentConversion;
	}

	public void setAllowMatchRequiringArgumentConversion(boolean allow) {
		this.allowMatchesRequiringArgumentConversion = allow;
	}

	public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name, Class<?>[] argumentTypes) throws AccessException {
		try {
			Class<?> relevantClass = (targetObject instanceof Class ? (Class<?>) targetObject : targetObject.getClass());
			DiscoveredMethod dMethod = ReflectionUtils.findMethod(context.getTypeUtils().getTypeConverter(), name,
					argumentTypes, relevantClass, allowMatchesRequiringArgumentConversion);
			if (dMethod == null) {
				return null;
			}
			return new ReflectionMethodExecutor(dMethod.theMethod, dMethod.argumentsRequiringConversion);
		} catch (EvaluationException e) {
			throw new AccessException(null,e);
		}
	}

}
