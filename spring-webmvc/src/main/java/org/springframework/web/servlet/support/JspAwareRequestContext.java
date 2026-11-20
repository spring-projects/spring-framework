/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.support;

import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.jstl.core.Config;
import org.jspecify.annotations.Nullable;

/**
 * JSP-aware (and JSTL-aware) subclass of RequestContext, allowing for
 * population of the context from a {@code jakarta.servlet.jsp.PageContext}.
 *
 * <p>This context will detect a JSTL locale attribute in page/request/session/application
 * scope, in addition to the fallback locale strategy provided by the base class.
 *
 * @author Juergen Hoeller
 * @since 1.1.4
 * @see #getFallbackLocale
 */
public class JspAwareRequestContext extends RequestContext {

	private final PageContext pageContext;


	/**
	 * Create a new JspAwareRequestContext for the given page context,
	 * using the request attributes for Errors retrieval.
	 * @param pageContext current JSP page context
	 */
	public JspAwareRequestContext(PageContext pageContext) {
		this(pageContext, null);
	}

	/**
	 * Create a new JspAwareRequestContext for the given page context,
	 * using the given model attributes for Errors retrieval.
	 * @param pageContext current JSP page context
	 * @param model the model attributes for the current view
	 * (can be {@code null}, using the request attributes for Errors retrieval)
	 */
	public JspAwareRequestContext(PageContext pageContext, @Nullable Map<String, Object> model) {
		super((HttpServletRequest) pageContext.getRequest(), (HttpServletResponse) pageContext.getResponse(),
				pageContext.getServletContext(), model);
		this.pageContext = pageContext;
	}


	/**
	 * Return the underlying PageContext.
	 * Only intended for cooperating classes in this package.
	 */
	protected final PageContext getPageContext() {
		return this.pageContext;
	}

	/**
	 * This implementation checks for a JSTL locale attribute in page,
	 * request, session or application scope; if not found, returns the
	 * {@code HttpServletRequest.getLocale()}.
	 */
	@Override
	protected Locale getFallbackLocale() {
		if (JSTL_PRESENT) {
			Locale locale = JstlPageLocaleResolver.getJstlLocale(getPageContext());
			if (locale != null) {
				return locale;
			}
		}
		return getRequest().getLocale();
	}

	/**
	 * This implementation checks for a JSTL time zone attribute in page,
	 * request, session or application scope; if not found, returns {@code null}.
	 */
	@Override
	protected @Nullable TimeZone getFallbackTimeZone() {
		if (JSTL_PRESENT) {
			TimeZone timeZone = JstlPageLocaleResolver.getJstlTimeZone(getPageContext());
			if (timeZone != null) {
				return timeZone;
			}
		}
		return null;
	}


	/**
	 * Inner class that isolates the JSTL dependency.
	 * Just called to resolve the fallback locale if the JSTL API is present.
	 */
	private static class JstlPageLocaleResolver {

		public static @Nullable Locale getJstlLocale(PageContext pageContext) {
			Object localeObject = Config.find(pageContext, Config.FMT_LOCALE);
			return (localeObject instanceof Locale locale ? locale : null);
		}

		public static @Nullable TimeZone getJstlTimeZone(PageContext pageContext) {
			Object timeZoneObject = Config.find(pageContext, Config.FMT_TIME_ZONE);
			return (timeZoneObject instanceof TimeZone timeZone ? timeZone : null);
		}
	}

}
