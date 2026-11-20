/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.support;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ObjectUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * {@link org.springframework.web.WebApplicationInitializer WebApplicationInitializer}
 * to register a {@code DispatcherServlet} and use Java-based Spring configuration.
 *
 * <p>Implementations are required to implement:
 * <ul>
 * <li>{@link #getRootConfigClasses()} -- for "root" application context (non-web
 * infrastructure) configuration.
 * <li>{@link #getServletConfigClasses()} -- for {@code DispatcherServlet}
 * application context (Spring MVC infrastructure) configuration.
 * </ul>
 *
 * <p>If an application context hierarchy is not required, applications may
 * return all configuration via {@link #getRootConfigClasses()} and return
 * {@code null} from {@link #getServletConfigClasses()}.
 *
 * @author Arjen Poutsma
 * @author Chris Beams
 * @since 3.2
 */
public abstract class AbstractAnnotationConfigDispatcherServletInitializer
		extends AbstractDispatcherServletInitializer {

	/**
	 * {@inheritDoc}
	 * <p>This implementation creates an {@link AnnotationConfigWebApplicationContext},
	 * providing it the annotated classes returned by {@link #getRootConfigClasses()}.
	 * Returns {@code null} if {@link #getRootConfigClasses()} returns {@code null}.
	 */
	@Override
	protected @Nullable WebApplicationContext createRootApplicationContext() {
		Class<?>[] configClasses = getRootConfigClasses();
		if (!ObjectUtils.isEmpty(configClasses)) {
			AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
			context.register(configClasses);
			return context;
		}
		else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>This implementation creates an {@link AnnotationConfigWebApplicationContext},
	 * providing it the annotated classes returned by {@link #getServletConfigClasses()}.
	 */
	@Override
	protected WebApplicationContext createServletApplicationContext() {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		Class<?>[] configClasses = getServletConfigClasses();
		if (!ObjectUtils.isEmpty(configClasses)) {
			context.register(configClasses);
		}
		return context;
	}

	/**
	 * Specify {@code @Configuration} and/or {@code @Component} classes for the
	 * {@linkplain #createRootApplicationContext() root application context}.
	 * @return the configuration for the root application context, or {@code null}
	 * if creation and registration of a root context is not desired
	 */
	protected abstract Class<?> @Nullable [] getRootConfigClasses();

	/**
	 * Specify {@code @Configuration} and/or {@code @Component} classes for the
	 * {@linkplain #createServletApplicationContext() Servlet application context}.
	 * @return the configuration for the Servlet application context, or
	 * {@code null} if all configuration is specified through root config classes.
	 */
	protected abstract Class<?> @Nullable [] getServletConfigClasses();

}
