/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.AbstractWebArgumentResolverAdapter;

/**
 * A Servlet-specific
 * {@link org.springframework.web.method.annotation.AbstractWebArgumentResolverAdapter}
 * that creates a {@link NativeWebRequest} from {@link ServletRequestAttributes}.
 *
 * <p><strong>Note:</strong> This class is provided for backwards compatibility.
 * However it is recommended to re-write a {@code WebArgumentResolver} as
 * {@code HandlerMethodArgumentResolver}. For more details see javadoc of
 * {@link org.springframework.web.method.annotation.AbstractWebArgumentResolverAdapter}.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletWebArgumentResolverAdapter extends AbstractWebArgumentResolverAdapter {

	public ServletWebArgumentResolverAdapter(WebArgumentResolver adaptee) {
		super(adaptee);
	}

	@Override
	protected NativeWebRequest getWebRequest() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		Assert.state(requestAttributes instanceof ServletRequestAttributes, "No ServletRequestAttributes");
		ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
		return new ServletWebRequest(servletRequestAttributes.getRequest());
	}
}
