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
import org.springframework.expression.ConstructorExecutor;
import org.springframework.expression.ConstructorResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.reflection.ReflectionUtils.DiscoveredConstructor;

/**
 * A constructor resolver that uses reflection to locate the constructor that should be invoked
 * 
 * @author Andy Clement
 */
public class ReflectionConstructorResolver implements ConstructorResolver {

	/*
	 * Indicates if this resolve will allow matches to be found that require some of the input arguments to be
	 * transformed by the conversion service.
	 */
	private boolean allowMatchesRequiringArgumentConversion = true;

	public ReflectionConstructorResolver() {
	}

	public ReflectionConstructorResolver(boolean allowMatchesRequiringArgumentConversion) {
		this.allowMatchesRequiringArgumentConversion = allowMatchesRequiringArgumentConversion;
	}

	public void setAllowMatchRequiringArgumentConversion(boolean allow) {
		this.allowMatchesRequiringArgumentConversion = allow;
	}

	/**
	 * Locate a matching constructor or return null if non can be found.
	 */
	public ConstructorExecutor resolve(EvaluationContext context, String typename, Class<?>[] argumentTypes)
			throws AccessException {
		try {
			Class<?> c = context.getTypeUtils().getTypeLocator().findType(typename);
			DiscoveredConstructor dCtor = ReflectionUtils.findConstructor(context.getTypeUtils().getTypeConverter(), c,
					argumentTypes, allowMatchesRequiringArgumentConversion);
			if (dCtor == null) {
				return null;
			}
			return new ReflectionConstructorExecutor(dCtor.theConstructor, dCtor.argumentsRequiringConversion);
		} catch (EvaluationException e) {
			throw new AccessException(null,e);
		}
	}
}
