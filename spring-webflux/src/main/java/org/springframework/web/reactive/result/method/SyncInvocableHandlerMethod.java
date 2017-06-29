/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.result.method;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;

/**
 * Extension of {@link HandlerMethod} that invokes the underlying method via
 * {@link InvocableHandlerMethod} but uses sync argument resolvers only and
 * thus can return directly a {@link HandlerResult} with no async wrappers.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class SyncInvocableHandlerMethod extends HandlerMethod {

	private final InvocableHandlerMethod delegate;


	public SyncInvocableHandlerMethod(HandlerMethod handlerMethod) {
		super(handlerMethod);
		this.delegate = new InvocableHandlerMethod(handlerMethod);
	}

	public SyncInvocableHandlerMethod(Object bean, Method method) {
		super(bean, method);
		this.delegate = new InvocableHandlerMethod(bean, method);
	}


	/**
	 * Configure the argument resolvers to use to use for resolving method
	 * argument values against a {@code ServerWebExchange}.
	 */
	public void setArgumentResolvers(List<SyncHandlerMethodArgumentResolver> resolvers) {
		this.delegate.setArgumentResolvers(new ArrayList<>(resolvers));
	}

	/**
	 * Return the configured argument resolvers.
	 */
	public List<SyncHandlerMethodArgumentResolver> getResolvers() {
		return this.delegate.getResolvers().stream()
				.map(resolver -> (SyncHandlerMethodArgumentResolver) resolver)
				.collect(Collectors.toList());
	}

	/**
	 * Set the ParameterNameDiscoverer for resolving parameter names when needed
	 * (e.g. default request attribute name).
	 * <p>Default is a {@link DefaultParameterNameDiscoverer}.
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer nameDiscoverer) {
		this.delegate.setParameterNameDiscoverer(nameDiscoverer);
	}

	/**
	 * Return the configured parameter name discoverer.
	 */
	public ParameterNameDiscoverer getParameterNameDiscoverer() {
		return this.delegate.getParameterNameDiscoverer();
	}


	/**
	 * Invoke the method for the given exchange.
	 * @param exchange the current exchange
	 * @param bindingContext the binding context to use
	 * @param providedArgs optional list of argument values to match by type
	 * @return Mono with a {@link HandlerResult}.
	 */
	@Nullable
	public HandlerResult invokeForHandlerResult(ServerWebExchange exchange,
			BindingContext bindingContext, Object... providedArgs) {

		// This will not block with only sync resolvers allowed
		return this.delegate.invoke(exchange, bindingContext, providedArgs).block();
	}

}
