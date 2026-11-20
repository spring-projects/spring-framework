/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.filter;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.server.RequestPath;
import org.springframework.web.util.ServletRequestPathUtils;

/**
 * {@code Filter} that {@link ServletRequestPathUtils#parseAndCache parses and
 * caches} a {@link org.springframework.http.server.RequestPath} that can then
 * be accessed via {@link ServletRequestPathUtils#getParsedRequestPath}.
 * <p><strong>Note:</strong> The {@code DispatcherServlet} already does the same,
 * and therefore, this filter is mainly useful if you need to also have the
 * parsed path available in the filter chain before and after the
 * {@code DispatcherServlet}.
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public class ServletRequestPathFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		RequestPath previousRequestPath = (RequestPath) request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE);
		ServletRequestPathUtils.parseAndCache((HttpServletRequest) request);
		try {
			chain.doFilter(request, response);
		}
		finally {
			ServletRequestPathUtils.setParsedRequestPath(previousRequestPath, request);
		}
	}

}
