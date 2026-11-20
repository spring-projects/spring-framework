/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.view.groovy;

import java.util.Locale;

import org.springframework.web.servlet.view.AbstractTemplateViewResolver;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

/**
 * Convenience subclass of {@link AbstractTemplateViewResolver} that supports
 * {@link GroovyMarkupView} (i.e. Groovy XML/XHTML markup templates) and
 * custom subclasses of it.
 *
 * <p>The view class for all views created by this resolver can be specified
 * via {@link #setViewClass(Class)}.
 *
 * <p><b>Note:</b> When chaining ViewResolvers this resolver will check for the
 * existence of the specified template resources and only return a non-null
 * {@code View} object if a template is actually found.
 *
 * @author Brian Clozel
 * @since 4.1
 * @see GroovyMarkupConfigurer
 */
public class GroovyMarkupViewResolver extends AbstractTemplateViewResolver {

	/**
	 * Sets the default {@link #setViewClass view class} to {@link #requiredViewClass}:
	 * by default {@link GroovyMarkupView}.
	 */
	public GroovyMarkupViewResolver() {
		setViewClass(requiredViewClass());
	}

	/**
	 * A convenience constructor that allows for specifying the {@link #setPrefix prefix}
	 * and {@link #setSuffix suffix} as constructor arguments.
	 * @param prefix the prefix that gets prepended to view names when building a URL
	 * @param suffix the suffix that gets appended to view names when building a URL
	 * @since 4.3
	 */
	public GroovyMarkupViewResolver(String prefix, String suffix) {
		this();
		setPrefix(prefix);
		setSuffix(suffix);
	}


	@Override
	protected Class<?> requiredViewClass() {
		return GroovyMarkupView.class;
	}

	@Override
	protected AbstractUrlBasedView instantiateView() {
		return (getViewClass() == GroovyMarkupView.class ? new GroovyMarkupView() : super.instantiateView());
	}

	/**
	 * This resolver supports i18n, so cache keys should contain the locale.
	 */
	@Override
	protected Object getCacheKey(String viewName, Locale locale) {
		return viewName + '_' + locale;
	}

}
