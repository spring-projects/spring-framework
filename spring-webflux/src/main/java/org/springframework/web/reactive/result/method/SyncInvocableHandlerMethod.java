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

import org.springframework.util.Assert;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;

/**
 * Extension of {@code InvocableHandlerMethod} that can only be configured with
 * synchronous argument resolvers and thus exposing an additional, synchronous
 * {@link #invokeForHandlerResult} returning a {@code HandlerResult} vs
 * {@code Mono<HandlerResult>}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class SyncInvocableHandlerMethod extends InvocableHandlerMethod {


	public SyncInvocableHandlerMethod(HandlerMethod handlerMethod) {
		super(handlerMethod);
	}

	public SyncInvocableHandlerMethod(Object bean, Method method) {
		super(bean, method);
	}


	/**
	 * {@inheritDoc}
	 * <p>Resolvers must be of type {@link SyncHandlerMethodArgumentResolver}.
	 * @see #setSyncArgumentResolvers(List)
	 */
	@Override
	public void setArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.forEach(this::assertSyncResolvers);
		super.setArgumentResolvers(resolvers);
	}

	private void assertSyncResolvers(HandlerMethodArgumentResolver resolver) {
		Assert.isInstanceOf(SyncHandlerMethodArgumentResolver.class, resolver,
				"SyncInvocableHandlerMethod requires resolvers of type " +
						"SyncHandlerMethodArgumentResolver");
	}

	/**
	 * Convenient alternative to {@link #setArgumentResolvers(List)} to configure
	 * synchronous argument resolvers.
	 */
	public void setSyncArgumentResolvers(List<SyncHandlerMethodArgumentResolver> resolvers) {
		setArgumentResolvers(new ArrayList<>(resolvers));
	}


	/**
	 * Delegate to the base class {@link #invoke} and also wait for the result.
	 * Since all argument resolvers are synchronous this won't actually block.
	 */
	public HandlerResult invokeForHandlerResult(ServerWebExchange exchange,
			BindingContext bindingContext, Object... providedArgs) {

		// This will not block with only sync resolvers allowed
		return super.invoke(exchange, bindingContext, providedArgs).block();
	}

}
