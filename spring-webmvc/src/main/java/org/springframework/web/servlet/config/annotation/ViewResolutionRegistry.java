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
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfig;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.tiles3.TilesConfigurer;
import org.springframework.web.servlet.view.velocity.VelocityConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * Helps with configuring a list of view resolvers.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class ViewResolutionRegistry {

	private final List<ViewResolutionRegistration<?>> registrations = new ArrayList<ViewResolutionRegistration<?>>();

	private final ApplicationContext applicationContext;


	public ViewResolutionRegistry(ApplicationContext context) {
		this.applicationContext = context;
	}


	/**
	 * Register a custom {@link ViewResolver} bean.
	 */
	public ViewResolutionRegistration<ViewResolver> addViewResolver(ViewResolver viewResolver) {
		ViewResolutionRegistration<ViewResolver> registration = new ViewResolutionRegistration<ViewResolver>(this, viewResolver);
		registrations.add(registration);
		return registration;
	}

	/**
	 * Register an {@link org.springframework.web.servlet.view.InternalResourceViewResolver}
	 * bean with default "/WEB-INF/" prefix and ".jsp" suffix.
	 */
	public JspRegistration jsp() {
		JspRegistration registration = new JspRegistration(this);
		addAndCheckViewResolution(registration);
		return registration;
	}

	/**
	 * Register an {@link org.springframework.web.servlet.view.InternalResourceViewResolver}
	 * bean with specified prefix and suffix.
	 */
	public JspRegistration jsp(String prefix, String suffix) {
		JspRegistration registration = new JspRegistration(this, prefix, suffix);
		addAndCheckViewResolution(registration);
		return registration;
	}

	/**
	 * Register a {@link org.springframework.web.servlet.view.BeanNameViewResolver} bean.
	 */
	public BeanNameRegistration beanName() {
		BeanNameRegistration registration = new BeanNameRegistration(this);
		addAndCheckViewResolution(registration);
		return registration;
	}

	/**
	 * Register a {@link org.springframework.web.servlet.view.tiles3.TilesViewResolver} and
	 * a {@link org.springframework.web.servlet.view.tiles3.TilesConfigurer} with
	 * default "/WEB-INF/tiles.xml" definition and no Tiles definition check refresh.
	 */
	public TilesRegistration tiles() {
		if (!hasBeanOfType(TilesConfigurer.class)) {
			throw new BeanInitializationException(
					"It looks like you're trying to configure Tiles view resolution. " +
					"If not using @EnableWebMvc you must import WebMvcTilesConfiguration, " +
					"or declare your own TilesConfigurer bean.");
		}
		TilesRegistration registration = new TilesRegistration(this);
		addAndCheckViewResolution(registration);
		return registration;
	}

	/**
	 * Register a {@link org.springframework.web.servlet.view.velocity.VelocityLayoutViewResolver}
	 * and a {@link org.springframework.web.servlet.view.velocity.VelocityConfigurer} beans with
	 * default "" prefix, ".vm" suffix and "/WEB-INF/" resourceLoaderPath.
	 */
	public VelocityRegistration velocity() {
		if (!hasBeanOfType(VelocityConfigurer.class)) {
			throw new BeanInitializationException(
					"It looks like you're trying to configure Velocity view resolution. " +
					"If not using @EnableWebMvc you must import WebMvcVelocityConfiguration, " +
					"or declare your own VelocityConfigurer bean.");
		}
		VelocityRegistration registration = new VelocityRegistration(this);
		addAndCheckViewResolution(registration);
		return registration;
	}

	/**
	 * Register a {@link org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver}
	 * and a {@link org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer} beans with
	 * "" prefix, ".ftl" suffix and "/WEB-INF/" templateLoaderPath.
	 */
	public FreeMarkerRegistration freemarker() {
		if (!hasBeanOfType(FreeMarkerConfigurer.class)) {
			throw new BeanInitializationException(
					"It looks like you're trying to configure FreeMarker view resolution. " +
					"If not using @EnableWebMvc you must import WebMvcFreeMarkerConfiguration, " +
					"or declare your own FreeMarkerConfigurer bean.");
		}
		FreeMarkerRegistration registration = new FreeMarkerRegistration(this);
		addAndCheckViewResolution(registration);
		return registration;
	}

	protected boolean hasBeanOfType(Class<?> beanType) {
		return !ObjectUtils.isEmpty(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
				this.applicationContext, beanType, false, false));
	}

	/**
	 * Register a {@link org.springframework.web.servlet.view.ContentNegotiatingViewResolver} bean.
	 */
	public ContentNegotiatingRegistration contentNegotiating(View... defaultViews) {
		ContentNegotiatingRegistration registration = new ContentNegotiatingRegistration(this);
		registration.defaultViews(defaultViews);
		addAndCheckViewResolution(registration);
		return registration;
	}

	protected List<ViewResolver> getViewResolvers() {
		List<ViewResolver> viewResolvers = new ArrayList<ViewResolver>();
		for(ViewResolutionRegistration<?> registration : this.registrations) {
			viewResolvers.add(registration.getViewResolver());
		}
		return viewResolvers;
	}

	private void addAndCheckViewResolution(ViewResolutionRegistration<?> registration) {
		for(ViewResolutionRegistration<?> existingRegistration : this.registrations) {
			if(existingRegistration.getClass().equals(registration.getClass())) {
				throw new IllegalStateException("An instance of " + registration.getClass().getSimpleName()
						+ " is already registered, and multiple view resolvers and configurers beans are not supported by this registry");
			}
		}
		registrations.add(registration);
	}
}
