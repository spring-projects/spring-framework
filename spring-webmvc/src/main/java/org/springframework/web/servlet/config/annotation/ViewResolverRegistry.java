/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.config.annotation;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;
import org.springframework.web.servlet.view.tiles3.TilesConfigurer;
import org.springframework.web.servlet.view.tiles3.TilesViewResolver;
import org.springframework.web.servlet.view.velocity.VelocityConfigurer;
import org.springframework.web.servlet.view.velocity.VelocityViewResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Assist with the configuration of a chain of
 * {@link org.springframework.web.servlet.ViewResolver ViewResolver} instances.
 * This class is expected to be used via {@link WebMvcConfigurer#configureViewResolvers}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class ViewResolverRegistry {

	private ContentNegotiatingViewResolver contentNegotiatingResolver;

	private final List<ViewResolver> viewResolvers = new ArrayList<ViewResolver>(4);

	private int order = Ordered.LOWEST_PRECEDENCE;

	private ContentNegotiationManager contentNegotiationManager;

	private ApplicationContext applicationContext;


	protected void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
		this.contentNegotiationManager = contentNegotiationManager;
	}

	protected void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * Whether any view resolvers have been registered.
	 */
	public boolean hasRegistrations() {
		return (this.contentNegotiatingResolver != null || !this.viewResolvers.isEmpty());
	}


	/**
	 * Enable use of a {@link ContentNegotiatingViewResolver} to front all other
	 * configured view resolvers and select among all selected Views based on
	 * media types requested by the client (e.g. in the Accept header).
	 *
	 * <p>If invoked multiple times the provided default views will be added to
	 * any other default views that may have been configured already.
	 *
	 * @see ContentNegotiatingViewResolver#setDefaultViews
	 */
	public void enableContentNegotiation(View... defaultViews) {
		initContentNegotiatingViewResolver(defaultViews);
	}

	/**
	 * Enable use of a {@link ContentNegotiatingViewResolver} to front all other
	 * configured view resolvers and select among all selected Views based on
	 * media types requested by the client (e.g. in the Accept header).
	 *
	 * <p>If invoked multiple times the provided default views will be added to
	 * any other default views that may have been configured already.
	 *
	 * @see ContentNegotiatingViewResolver#setDefaultViews
	 */
	public void enableContentNegotiation(boolean useNotAcceptableStatus, View... defaultViews) {
		initContentNegotiatingViewResolver(defaultViews);
		this.contentNegotiatingResolver.setUseNotAcceptableStatusCode(useNotAcceptableStatus);
	}

	private void initContentNegotiatingViewResolver(View[] defaultViews) {

		// ContentNegotiatingResolver in the registry: elevate its precedence!
		this.order = Ordered.HIGHEST_PRECEDENCE;

		if (this.contentNegotiatingResolver != null) {
			if (!ObjectUtils.isEmpty(defaultViews)) {
				if (!CollectionUtils.isEmpty(this.contentNegotiatingResolver.getDefaultViews())) {
					List<View> views = new ArrayList<View>(this.contentNegotiatingResolver.getDefaultViews());
					views.addAll(Arrays.asList(defaultViews));
					this.contentNegotiatingResolver.setDefaultViews(views);
				}
			}
		}
		else {
			this.contentNegotiatingResolver = new ContentNegotiatingViewResolver();
			this.contentNegotiatingResolver.setDefaultViews(Arrays.asList(defaultViews));
			this.contentNegotiatingResolver.setViewResolvers(this.viewResolvers);
			this.contentNegotiatingResolver.setContentNegotiationManager(this.contentNegotiationManager);
		}
	}

	/**
	 * Enable view resolution by forwarding to JSP pages with a default view name
	 * prefix of "/WEB-INF/" and a default suffix of ".jsp".
	 *
	 * <p>This method may be invoked multiple and each call will register a
	 * separate ViewResolver instance. Note that since it's not easy to determine
	 * if a JSP exists without forwarding to it, using multiple JSP-based view
	 * resolvers only makes sense in combination with the "viewNames" property
	 * that indicates which view names are handled by which resolver.
	 */
	public UrlBasedViewResolverRegistration jsp() {
		return jsp("/WEB-INF/", ".jsp");
	}

	/**
	 * Enable view resolution by forwarding to JSP pages with the specified
	 * prefix and suffix.
	 *
	 * <p>This method may be invoked multiple and each call will register a
	 * separate ViewResolver instance. Note that since it's not easy to determine
	 * if a JSP exists without forwarding to it, using multiple JSP-based view
	 * resolvers only makes sense in combination with the "viewNames" property
	 * that indicates which view names are handled by which resolver.
	 */
	public UrlBasedViewResolverRegistration jsp(String prefix, String suffix) {
		InternalResourceViewResolver resolver = new InternalResourceViewResolver();
		resolver.setPrefix(prefix);
		resolver.setSuffix(suffix);
		this.viewResolvers.add(resolver);
		return new UrlBasedViewResolverRegistration(resolver);
	}

	/**
	 * Enable Tiles-based view resolution.
	 *
	 * <p>By default tiles definitions are expected to be in "/WEB-INF/tiles.xml".
	 * To change that and other Tiles-related options please also implement the
	 * interface {@link TilesWebMvcConfigurer}.
	 */
	public UrlBasedViewResolverRegistration tiles() {
		if (this.applicationContext != null && !hasBeanOfType(TilesConfigurer.class)) {
			throw new BeanInitializationException(
					"It looks like you're trying to configure Tiles view resolution. " +
					"If not using @EnableWebMvc you must import WebMvcTilesConfiguration, " +
					"or declare your own TilesConfigurer bean.");
		}
		TilesRegistration registration = new TilesRegistration();
		this.viewResolvers.add(registration.getViewResolver());
		return registration;
	}

	/**
	 * Enable FreeMarker-based view resolution with an empty default view name
	 * prefix and a default suffix of ".ftl".
	 *
	 * <p>By default the FreeMarker template loader path is set to "/WEB-INF/".
	 * To change that and other FreeMarker-related options please also implement
	 * the interface {@link FreeMarkerWebMvcConfigurer}.
	 */
	public UrlBasedViewResolverRegistration freeMarker() {
		if (this.applicationContext != null && !hasBeanOfType(FreeMarkerConfigurer.class)) {
			throw new BeanInitializationException(
					"It looks like you're trying to configure FreeMarker view resolution. " +
							"If not using @EnableWebMvc you must import WebMvcFreeMarkerConfiguration, " +
							"or declare your own FreeMarkerConfigurer bean.");
		}
		FreeMarkerRegistration registration = new FreeMarkerRegistration();
		this.viewResolvers.add(registration.getViewResolver());
		return registration;
	}

	/**
	 * Enable Velocity-based view resolution with an empty default view name
	 * prefix, a default suffix of ".vm".
	 *
	 * <p>By default the Velocity resource loader path is set to "/WEB-INF/".
	 * To change that and other Velocity-related options please also implement
	 * the interface {@link VelocityWebMvcConfigurer}.
	 */
	public UrlBasedViewResolverRegistration velocity() {
		if (this.applicationContext != null && !hasBeanOfType(VelocityConfigurer.class)) {
			throw new BeanInitializationException(
					"It looks like you're trying to configure Velocity view resolution. " +
					"If not using @EnableWebMvc you must import WebMvcVelocityConfiguration, " +
					"or declare your own VelocityConfigurer bean.");
		}
		VelocityRegistration registration = new VelocityRegistration();
		this.viewResolvers.add(registration.getViewResolver());
		return registration;
	}

	/**
	 * Enable the ability to map view names returned from controllers to
	 * {@link org.springframework.web.servlet.View} beans.
	 */
	public void beanName() {
		BeanNameViewResolver resolver = new BeanNameViewResolver();
		this.viewResolvers.add(resolver);
	}

	/**
	 * Register a {@link ViewResolver} bean instance. This may be useful to
	 * configure a custom (or 3rd party) resolver implementation. It may also be
	 * used as an alternative to other registration methods in this class when
	 * they don't expose some more advanced property that needs to be set.
	 */
	public void viewResolver(ViewResolver viewResolver) {
		if (viewResolver instanceof ContentNegotiatingViewResolver) {
			throw new BeanInitializationException(
					"addViewResolver cannot be used to configure a ContentNegotiatingViewResolver. " +
					"Please use the method enableContentNegotiation instead.");
		}
		this.viewResolvers.add(viewResolver);
	}

	protected boolean hasBeanOfType(Class<?> beanType) {
		return !ObjectUtils.isEmpty(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
				this.applicationContext, beanType, false, false));
	}


	protected int getOrder() {
		return this.order;
	}

	protected List<ViewResolver> getViewResolvers() {
		if (this.contentNegotiatingResolver != null) {
			return Collections.<ViewResolver>singletonList(this.contentNegotiatingResolver);
		}
		else {
			return this.viewResolvers;
		}
	}


	private static class TilesRegistration extends UrlBasedViewResolverRegistration {

		private TilesRegistration() {
			super(new TilesViewResolver());
		}
	}

	private static class VelocityRegistration extends UrlBasedViewResolverRegistration {

		private VelocityRegistration() {
			super(new VelocityViewResolver());
			getViewResolver().setSuffix(".vm");
		}
	}

	private static class FreeMarkerRegistration extends UrlBasedViewResolverRegistration {

		private FreeMarkerRegistration() {
			super(new FreeMarkerViewResolver());
			getViewResolver().setSuffix(".ftl");
		}
	}

}
