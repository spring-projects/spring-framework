/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.rsocket.service;

import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.ProxyHints;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.Search;
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

	@Override
	public @Nullable BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
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
			return new AotContribution(exchangeInterfaces);
		}
		return null;
	}


	private static class AotContribution implements BeanRegistrationAotContribution {

		private final Set<Class<?>> rSocketExchangeInterfaces;

		public AotContribution(Set<Class<?>> rSocketExchangeInterfaces) {
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
