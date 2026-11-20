/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;

/**
 * Cache-specific evaluation context that adds method parameters as SpEL
 * variables, in a lazy manner. The lazy nature avoids unnecessary
 * parsing of a class's byte code for parameter discovery.
 *
 * <p>Also defines a set of "unavailable variables" (i.e. variables that should
 * lead to an exception as soon as they are accessed). This can be useful
 * to verify a condition does not match even when not all potential variables
 * are present.
 *
 * <p>To limit the creation of objects, an ugly constructor is used
 * (rather than a dedicated 'closure'-like class for deferred execution).
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 3.1
 */
class CacheEvaluationContext extends MethodBasedEvaluationContext {

	private final Set<String> unavailableVariables = new HashSet<>(1);


	CacheEvaluationContext(@Nullable Object rootObject, Method method, @Nullable Object[] arguments,
			ParameterNameDiscoverer parameterNameDiscoverer) {

		super(rootObject, method, arguments, parameterNameDiscoverer);
	}


	/**
	 * Add the specified variable name as unavailable for this context.
	 * <p>Any expression trying to access this variable should lead to an exception.
	 * <p>This permits the validation of expressions that could potentially access
	 * a variable even when such a variable isn't available yet. Any expression
	 * trying to use that variable should therefore fail to evaluate.
	 */
	public void addUnavailableVariable(String name) {
		this.unavailableVariables.add(name);
	}


	/**
	 * Load the param information only when needed.
	 */
	@Override
	public @Nullable Object lookupVariable(String name) {
		if (this.unavailableVariables.contains(name)) {
			throw new VariableNotAvailableException(name);
		}
		return super.lookupVariable(name);
	}

}
