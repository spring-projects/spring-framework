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

package org.springframework.messaging.rsocket.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.ProxyHints;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.Search;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import static org.springframework.core.annotation.MergedAnnotations.SearchStrategy.TYPE_HIERARCHY;

/**
 * An AOT {@link BeanRegistrationAotProcessor} that detects the presence of
 * {@link RSocketExchange @RSocketExchange} on methods and creates the required
 * proxy hints.
 *
 * @author Sebastien Deleuze
 * @author Olga Maciaszek-Sharma
 * @since 6.0.5
 * @see org.springframework.web.service.annotation.HttpExchangeBeanRegistrationAotProcessor
 */
class RSocketExchangeBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

	@Nullable
	@Override
	public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		Class<?> beanClass = registeredBean.getBeanClass();
		Set<Class<?>> exchangeInterfaces = new HashSet<>();
		Search search = MergedAnnotations.search(TYPE_HIERARCHY);
		for (Class<?> interfaceClass : ClassUtils.getAllInterfacesForClass(beanClass)) {
			ReflectionUtils.doWithMethods(interfaceClass, method -> {
				if (!exchangeInterfaces.contains(interfaceClass) &&
						search.from(method).isPresent(RSocketExchange.class)) {
					exchangeInterfaces.add(interfaceClass);
				}
			});
		}
		if (!exchangeInterfaces.isEmpty()) {
			return new RSocketExchangeBeanRegistrationContribution(exchangeInterfaces);
		}
		return null;
	}


	private static class RSocketExchangeBeanRegistrationContribution implements BeanRegistrationAotContribution {

		private final Set<Class<?>> rSocketExchangeInterfaces;

		public RSocketExchangeBeanRegistrationContribution(Set<Class<?>> rSocketExchangeInterfaces) {
			this.rSocketExchangeInterfaces = rSocketExchangeInterfaces;
		}

		@Override
		public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
			ProxyHints proxyHints = generationContext.getRuntimeHints().proxies();
			for (Class<?> rSocketExchangeInterface : this.rSocketExchangeInterfaces) {
				proxyHints.registerJdkProxy(AopProxyUtils.completeJdkProxyInterfaces(rSocketExchangeInterface));
			}
		}

	}

}
