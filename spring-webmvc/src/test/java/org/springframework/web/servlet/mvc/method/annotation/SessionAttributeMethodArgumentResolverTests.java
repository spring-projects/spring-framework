/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

/**
 * Tests for {@link SessionAttributeMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 */
class SessionAttributeMethodArgumentResolverTests extends AbstractRequestAttributesArgumentResolverTests {

	@Override
	protected HandlerMethodArgumentResolver createResolver() {
		return new SessionAttributeMethodArgumentResolver();
	}

	@Override
	protected String getHandleMethodName() {
		return "handleWithSessionAttribute";
	}

	@Override
	protected int getScope() {
		return RequestAttributes.SCOPE_SESSION;
	}

}
