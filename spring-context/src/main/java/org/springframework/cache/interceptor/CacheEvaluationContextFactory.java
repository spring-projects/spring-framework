/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.function.SingletonSupplier;

/**
 * A factory for {@link CacheEvaluationContext} that makes sure that internal
 * delegates are reused.
 *
 * @author Stephane Nicoll
 * @since 6.1.1
 */
class CacheEvaluationContextFactory {

	private final StandardEvaluationContext originalContext;

	@Nullable
	private Supplier<ParameterNameDiscoverer> parameterNameDiscoverer;

	CacheEvaluationContextFactory(StandardEvaluationContext originalContext) {
		this.originalContext = originalContext;
	}

	public void setParameterNameDiscoverer(Supplier<ParameterNameDiscoverer> parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	public ParameterNameDiscoverer getParameterNameDiscoverer() {
		if (this.parameterNameDiscoverer == null) {
			this.parameterNameDiscoverer = SingletonSupplier.of(new DefaultParameterNameDiscoverer());
		}
		return this.parameterNameDiscoverer.get();
	}

	/**
	 * Creates a {@link CacheEvaluationContext} for the specified operation.
	 * @param rootObject the {@code root} object to use for the context
	 * @param targetMethod the target cache {@link Method}
	 * @param args the arguments of the method invocation
	 * @return a context suitable for this cache operation
	 */
	public CacheEvaluationContext forOperation(CacheExpressionRootObject rootObject,
			Method targetMethod, Object[] args) {

		CacheEvaluationContext evaluationContext = new CacheEvaluationContext(
				rootObject, targetMethod, args, getParameterNameDiscoverer());
		this.originalContext.applyDelegatesTo(evaluationContext);
		return evaluationContext;
	}

}
