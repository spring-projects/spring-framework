/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
import org.springframework.web.servlet.view.groovy.GroovyMarkupConfigurer;
import org.springframework.web.servlet.view.groovy.GroovyMarkupViewResolver;
import org.springframework.web.servlet.view.script.ScriptTemplateConfigurer;
import org.springframework.web.servlet.view.script.ScriptTemplateViewResolver;
import org.springframework.web.servlet.view.tiles3.TilesConfigurer;
import org.springframework.web.servlet.view.tiles3.TilesViewResolver;

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

	private ContentNegotiationManager contentNegotiationManager;

	private ApplicationContext applicationContext;

	private ContentNegotiatingViewResolver contentNegotiatingResolver;

	private final List<ViewResolver> viewResolvers = new ArrayList<ViewResolver>(4);

	private Integer order;


	/**
	 * Class constructor with {@link ContentNegotiationManager} and {@link ApplicationContext}.
	 * @since 4.3.12
	 */
	public ViewResolverRegistry(ContentNegotiationManager contentNegotiationManager, ApplicationContext context) {
		this.contentNegotiationManager = contentNegotiationManager;
		this.applicationContext = context;
	}

	@Deprecated
	public ViewResolverRegistry() {
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
	 * <p>If invoked multiple times the provided default views will be added to
	 * any other default views that may have been configured already.
	 * @see ContentNegotiatingViewResolver#setDefaultViews
	 */
	public void enableContentNegotiation(View... defaultViews) {
		initContentNegotiatingViewResolver(defaultViews);
	}

	/**
	 * Enable use of a {@link ContentNegotiatingViewResolver} to front all other
	 * configured view resolvers and select among all selected Views based on
	 * media types requested by the client (e.g. in the Accept header).
	 * <p>If invoked multiple times the provided default views will be added to
	 * any other default views that may have been configured already.
	 * @see ContentNegotiatingViewResolver#setDefaultViews
	 */
	public void enableContentNegotiation(boolean useNotAcceptableStatus, View... defaultViews) {
		initContentNegotiatingViewResolver(defaultViews);
		this.contentNegotiatingResolver.setUseNotAcceptableStatusCode(useNotAcceptableStatus);
	}

	private void initContentNegotiatingViewResolver(View[] defaultViews) {
		// ContentNegotiatingResolver in the registry: elevate its precedence!
		this.order = (this.order != null ? this.order : Ordered.HIGHEST_PRECEDENCE);

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
	 * Register JSP view resolver using a default view name prefix of "/WEB-INF/"
	 * and a default suffix of ".jsp".
	 * <p>When this method is invoked more than once, each call will register a
	 * new ViewResolver instance. Note that since it's not easy to determine
	 * if a JSP exists without forwarding to it, using multiple JSP-based view
	 * resolvers only makes sense in combination with the "viewNames" property
	 * on the resolver indicating which view names are handled by which resolver.
	 */
	public UrlBasedViewResolverRegistration jsp() {
		return jsp("/WEB-INF/", ".jsp");
	}

	/**
	 * Register JSP view resolver with the specified prefix and suffix.
	 * <p>When this method is invoked more than once, each call will register a
	 * new ViewResolver instance. Note that since it's not easy to determine
	 * if a JSP exists without forwarding to it, using multiple JSP-based view
	 * resolvers only makes sense in combination with the "viewNames" property
	 * on the resolver indicating which view names are handled by which resolver.
	 */
	public UrlBasedViewResolverRegistration jsp(String prefix, String suffix) {
		InternalResourceViewResolver resolver = new InternalResourceViewResolver();
		resolver.setPrefix(prefix);
		resolver.setSuffix(suffix);
		this.viewResolvers.add(resolver);
		return new UrlBasedViewResolverRegistration(resolver);
	}

	/**
	 * Register Tiles 3.x view resolver.
	 * <p><strong>Note</strong> that you must also configure Tiles by adding a
	 * {@link org.springframework.web.servlet.view.tiles3.TilesConfigurer} bean.
	 */
	public UrlBasedViewResolverRegistration tiles() {
		if (!checkBeanOfType(TilesConfigurer.class)) {
			throw new BeanInitializationException("In addition to a Tiles view resolver " +
					"there must also be a single TilesConfigurer bean in this web application context " +
					"(or its parent).");
		}
		TilesRegistration registration = new TilesRegistration();
		this.viewResolvers.add(registration.getViewResolver());
		return registration;
	}

	/**
	 * Register a FreeMarker view resolver with an empty default view name
	 * prefix and a default suffix of ".ftl".
	 * <p><strong>Note</strong> that you must also configure FreeMarker by adding a
	 * {@link org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer} bean.
	 */
	public UrlBasedViewResolverRegistration freeMarker() {
		if (!checkBeanOfType(FreeMarkerConfigurer.class)) {
			throw new BeanInitializationException("In addition to a FreeMarker view resolver " +
					"there must also be a single FreeMarkerConfig bean in this web application context " +
					"(or its parent): FreeMarkerConfigurer is the usual implementation. " +
					"This bean may be given any name.");
		}
		FreeMarkerRegistration registration = new FreeMarkerRegistration();
		this.viewResolvers.add(registration.getViewResolver());
		return registration;
	}

	/**
	 * Register Velocity view resolver with an empty default view name
	 * prefix and a default suffix of ".vm".
	 * <p><strong>Note</strong> that you must also configure Velocity by adding a
	 * {@link org.springframework.web.servlet.view.velocity.VelocityConfigurer} bean.
	 * @deprecated as of Spring 4.3, in favor of FreeMarker
	 */
	@Deprecated
	public UrlBasedViewResolverRegistration velocity() {
		if (!checkBeanOfType(org.springframework.web.servlet.view.velocity.VelocityConfigurer.class)) {
			throw new BeanInitializationException("In addition to a Velocity view resolver " +
					"there must also be a single VelocityConfig bean in this web application context " +
					"(or its parent): VelocityConfigurer is the usual implementation. " +
					"This bean may be given any name.");
		}
		VelocityRegistration registration = new VelocityRegistration();
		this.viewResolvers.add(registration.getViewResolver());
		return registration;
	}

	/**
	 * Register a Groovy markup view resolver with an empty default view name
	 * prefix and a default suffix of ".tpl".
	 */
	public UrlBasedViewResolverRegistration groovy() {
		if (!checkBeanOfType(GroovyMarkupConfigurer.class)) {
			throw new BeanInitializationException("In addition to a Groovy markup view resolver " +
					"there must also be a single GroovyMarkupConfig bean in this web application context " +
					"(or its parent): GroovyMarkupConfigurer is the usual implementation. " +
					"This bean may be given any name.");
		}
		GroovyMarkupRegistration registration = new GroovyMarkupRegistration();
		this.viewResolvers.add(registration.getViewResolver());
		return registration;
	}

	/**
	 * Register a script template view resolver with an empty default view name prefix and suffix.
	 * @since 4.2
	 */
	public UrlBasedViewResolverRegistration scriptTemplate() {
		if (!checkBeanOfType(ScriptTemplateConfigurer.class)) {
			throw new BeanInitializationException("In addition to a script template view resolver " +
					"there must also be a single ScriptTemplateConfig bean in this web application context " +
					"(or its parent): ScriptTemplateConfigurer is the usual implementation. " +
					"This bean may be given any name.");
		}
		ScriptRegistration registration = new ScriptRegistration();
		this.viewResolvers.add(registration.getViewResolver());
		return registration;
	}

	/**
	 * Register a bean name view resolver that interprets view names as the names
	 * of {@link org.springframework.web.servlet.View} beans.
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

	/**
	 * ViewResolver's registered through this registry are encapsulated in an
	 * instance of {@link org.springframework.web.servlet.view.ViewResolverComposite
	 * ViewResolverComposite} and follow the order of registration.
	 * This property determines the order of the ViewResolverComposite itself
	 * relative to any additional ViewResolver's (not registered here) present in
	 * the Spring configuration
	 * <p>By default this property is not set, which means the resolver is ordered
	 * at {@link Ordered#LOWEST_PRECEDENCE} unless content negotiation is enabled
	 * in which case the order (if not set explicitly) is changed to
	 * {@link Ordered#HIGHEST_PRECEDENCE}.
	 */
	public void order(int order) {
		this.order = order;
	}


	protected int getOrder() {
		return (this.order != null ? this.order : Ordered.LOWEST_PRECEDENCE);
	}

	protected List<ViewResolver> getViewResolvers() {
		if (this.contentNegotiatingResolver != null) {
			return Collections.<ViewResolver>singletonList(this.contentNegotiatingResolver);
		}
		else {
			return this.viewResolvers;
		}
	}

	private boolean checkBeanOfType(Class<?> beanType) {
		return (this.applicationContext == null ||
				!ObjectUtils.isEmpty(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
						this.applicationContext, beanType, false, false)));
	}

	@Deprecated
	protected boolean hasBeanOfType(Class<?> beanType) {
		return !ObjectUtils.isEmpty(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
				this.applicationContext, beanType, false, false));
	}

	@Deprecated
	protected void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
		this.contentNegotiationManager = contentNegotiationManager;
	}

	@Deprecated
	protected void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	private static class TilesRegistration extends UrlBasedViewResolverRegistration {

		public TilesRegistration() {
			super(new TilesViewResolver());
		}
	}


	private static class VelocityRegistration extends UrlBasedViewResolverRegistration {

		@SuppressWarnings("deprecation")
		public VelocityRegistration() {
			super(new org.springframework.web.servlet.view.velocity.VelocityViewResolver());
			getViewResolver().setSuffix(".vm");
		}
	}


	private static class FreeMarkerRegistration extends UrlBasedViewResolverRegistration {

		public FreeMarkerRegistration() {
			super(new FreeMarkerViewResolver());
			getViewResolver().setSuffix(".ftl");
		}
	}


	private static class GroovyMarkupRegistration extends UrlBasedViewResolverRegistration {

		public GroovyMarkupRegistration() {
			super(new GroovyMarkupViewResolver());
			getViewResolver().setSuffix(".tpl");
		}
	}


	private static class ScriptRegistration extends UrlBasedViewResolverRegistration {

		public ScriptRegistration() {
			super(new ScriptTemplateViewResolver());
			getViewResolver();
		}
	}

}
