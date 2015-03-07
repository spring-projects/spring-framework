/*
 * Copyright 2002-2015 the original author or authors.
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

import java.lang.reflect.Method;

import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ObjectUtils;

/**
 * A method-based {@link org.springframework.expression.EvaluationContext} that
 * provides explicit support for method-based invocations.
 * <p>
 * Expose the actual method arguments using the following aliases:
 * <ol>
 * <li>pX where X is the index of the argument (p0 for the first argument)</li>
 * <li>aX where X is the index of the argument (a1 for the second argument)</li>
 * <li>the name of the parameter as discovered by a configurable {@link ParameterNameDiscoverer}</li>
 * </ol>
 *
 * @author Stephane Nicoll
 * @since 4.2.0
 */
public class MethodBasedEvaluationContext extends StandardEvaluationContext {

	private final Method method;

	private final Object[] args;

	private final ParameterNameDiscoverer paramDiscoverer;

	private boolean paramLoaded = false;

	public MethodBasedEvaluationContext(Object rootObject, Method method, Object[] args,
			ParameterNameDiscoverer paramDiscoverer) {

		super(rootObject);
		this.method = method;
		this.args = args;
		this.paramDiscoverer = paramDiscoverer;
	}

	@Override
	public Object lookupVariable(String name) {
		Object variable = super.lookupVariable(name);
		if (variable != null) {
			return variable;
		}
		if (!this.paramLoaded) {
			lazyLoadArguments();
			this.paramLoaded = true;
			variable = super.lookupVariable(name);
		}
		return variable;
	}

	/**
	 * Load the param information only when needed.
	 */
	protected void lazyLoadArguments() {
		// shortcut if no args need to be loaded
		if (ObjectUtils.isEmpty(this.args)) {
			return;
		}

		// save arguments as indexed variables
		for (int i = 0; i < this.args.length; i++) {
			setVariable("a" + i, this.args[i]);
			setVariable("p" + i, this.args[i]);
		}

		String[] parameterNames = this.paramDiscoverer.getParameterNames(this.method);
		// save parameter names (if discovered)
		if (parameterNames != null) {
			for (int i = 0; i < parameterNames.length; i++) {
				setVariable(parameterNames[i], this.args[i]);
			}
		}
	}

}
