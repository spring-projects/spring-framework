/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;

/**
 * Helps with configuring a list of mapped interceptors.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @since 3.1
 */
public class InterceptorRegistry {

	private final List<InterceptorRegistration> registrations = new ArrayList<>();


	/**
	 * Adds the provided {@link HandlerInterceptor}.
	 * @param interceptor the interceptor to add
	 * @return an {@link InterceptorRegistration} that allows you optionally configure the
	 * registered interceptor further for example adding URL patterns it should apply to.
	 */
	public InterceptorRegistration addInterceptor(HandlerInterceptor interceptor) {
		InterceptorRegistration registration = new InterceptorRegistration(interceptor);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * Adds the provided {@link WebRequestInterceptor}.
	 * @param interceptor the interceptor to add
	 * @return an {@link InterceptorRegistration} that allows you optionally configure the
	 * registered interceptor further for example adding URL patterns it should apply to.
	 */
	public InterceptorRegistration addWebRequestInterceptor(WebRequestInterceptor interceptor) {
		WebRequestHandlerInterceptorAdapter adapted = new WebRequestHandlerInterceptorAdapter(interceptor);
		InterceptorRegistration registration = new InterceptorRegistration(adapted);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * Return all registered interceptors.
	 */
	protected List<Object> getInterceptors() {
		return this.registrations.stream()
				.sorted(INTERCEPTOR_ORDER_COMPARATOR)
				.map(InterceptorRegistration::getInterceptor)
				.toList();
	}


	private static final Comparator<Object> INTERCEPTOR_ORDER_COMPARATOR =
			OrderComparator.INSTANCE.withSourceProvider(object -> {
				if (object instanceof InterceptorRegistration interceptorRegistration) {
					return (Ordered) interceptorRegistration::getOrder;
				}
				return null;
			});

}
