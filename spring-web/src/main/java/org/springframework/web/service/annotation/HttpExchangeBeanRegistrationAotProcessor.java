/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.service.annotation;

import java.util.ArrayList;
import java.util.List;

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
 * AOT {@code BeanRegistrationAotProcessor} that detects the presence of
 * {@link HttpExchange @HttpExchange} on methods and creates the required proxy
 * hints.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 */
class HttpExchangeBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

	@Override
	public @Nullable BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		Class<?> beanClass = registeredBean.getBeanClass();
		List<Class<?>> exchangeInterfaces = new ArrayList<>();
		Search search = MergedAnnotations.search(TYPE_HIERARCHY);
		for (Class<?> interfaceClass : ClassUtils.getAllInterfacesForClass(beanClass)) {
			ReflectionUtils.doWithMethods(interfaceClass, method -> {
				if (!exchangeInterfaces.contains(interfaceClass) &&
						search.from(method).isPresent(HttpExchange.class)) {
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

		private final List<Class<?>> httpExchangeInterfaces;

		public AotContribution(List<Class<?>> httpExchangeInterfaces) {
			this.httpExchangeInterfaces = httpExchangeInterfaces;
		}

		@Override
		public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
			ProxyHints proxyHints = generationContext.getRuntimeHints().proxies();
			for (Class<?> httpExchangeInterface : this.httpExchangeInterfaces) {
				proxyHints.registerJdkProxy(AopProxyUtils.completeJdkProxyInterfaces(httpExchangeInterface));
			}
		}

	}

}
