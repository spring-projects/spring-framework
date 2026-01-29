package org.springframework.web.service.invoker;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.web.service.registry.HttpServiceGroup;

/**
 * Callback interface used during HttpService proxy creation. Allows manipulating the {@link ProxyFactory} creating the
 * HttpService.
 *
 * @author Dung Dang Minh
 */
public interface HttpServiceProxyPostProcessor {
	/**
	 * Manipulates the {@link ProxyFactory}, e.g. add further interceptors to it.
	 *
	 * @param factory will never be {@literal null}.
	 */
	void postProcess(ProxyFactory factory, Class<?> serviceType);
}
