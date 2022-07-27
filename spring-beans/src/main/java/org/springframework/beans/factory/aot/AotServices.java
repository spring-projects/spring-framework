/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.aot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A collection of AOT services that can be {@link Loader loaded} from
 * a {@link SpringFactoriesLoader} or obtained from a {@link ListableBeanFactory}.
 *
 * @author Phillip Webb
 * @since 6.0
 * @param <T> the service type
 */
public final class AotServices<T> implements Iterable<T> {

	/**
	 * The location to look for AOT factories.
	 */
	public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring/aot.factories";

	private final List<T> services;

	private final Map<String, T> beans;


	private AotServices(List<T> loaded, Map<String, T> beans) {
		List<T> services = new ArrayList<>();
		services.addAll(beans.values());
		services.addAll(loaded);
		AnnotationAwareOrderComparator.sort(services);
		this.services = Collections.unmodifiableList(services);
		this.beans = beans;
	}


	/**
	 * Return a new {@link Loader} that will obtain AOT services from
	 * {@value #FACTORIES_RESOURCE_LOCATION}.
	 * @return a new {@link Loader} instance
	 */
	public static Loader factories() {
		return factories((ClassLoader) null);
	}

	/**
	 * Return a new {@link Loader} that will obtain AOT services from
	 * {@value #FACTORIES_RESOURCE_LOCATION}.
	 * @param classLoader the class loader used to load the factories resource
	 * @return a new {@link Loader} instance
	 */
	public static Loader factories(@Nullable ClassLoader classLoader) {
		return factories(getSpringFactoriesLoader(classLoader));
	}

	/**
	 * Return a new {@link Loader} that will obtain AOT services from the given
	 * {@link SpringFactoriesLoader}.
	 * @param springFactoriesLoader the spring factories loader
	 * @return a new {@link Loader} instance
	 */
	public static Loader factories(SpringFactoriesLoader springFactoriesLoader) {
		Assert.notNull(springFactoriesLoader, "'springFactoriesLoader' must not be null");
		return new Loader(springFactoriesLoader, null);
	}

	/**
	 * Return a new {@link Loader} that will obtain AOT services from
	 * {@value #FACTORIES_RESOURCE_LOCATION} as well as the given
	 * {@link ListableBeanFactory}.
	 * @param beanFactory the bean factory
	 * @return a new {@link Loader} instance
	 */
	public static Loader factoriesAndBeans(ListableBeanFactory beanFactory) {
		ClassLoader classLoader = (beanFactory instanceof ConfigurableBeanFactory configurableBeanFactory)
				? configurableBeanFactory.getBeanClassLoader() : null;
		return factoriesAndBeans(getSpringFactoriesLoader(classLoader), beanFactory);
	}

	/**
	 * Return a new {@link Loader} that will obtain AOT services from the given
	 * {@link SpringFactoriesLoader} and {@link ListableBeanFactory}.
	 * @param springFactoriesLoader the spring factories loader
	 * @param beanFactory the bean factory
	 * @return a new {@link Loader} instance
	 */
	public static Loader factoriesAndBeans(SpringFactoriesLoader springFactoriesLoader, ListableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "'beanFactory' must not be null");
		Assert.notNull(springFactoriesLoader, "'springFactoriesLoader' must not be null");
		return new Loader(springFactoriesLoader, beanFactory);
	}

	private static SpringFactoriesLoader getSpringFactoriesLoader(
			@Nullable ClassLoader classLoader) {
		return SpringFactoriesLoader.forResourceLocation(FACTORIES_RESOURCE_LOCATION,
				classLoader);
	}

	@Override
	public Iterator<T> iterator() {
		return this.services.iterator();
	}

	/**
	 * Return a {@link Stream} of the AOT services.
	 * @return a stream of the services
	 */
	public Stream<T> stream() {
		return this.services.stream();
	}

	/**
	 * Return the AOT services as a {@link List}.
	 * @return a list of the services
	 */
	public List<T> asList() {
		return this.services;
	}

	/**
	 * Return the AOT services that was loaded for the given bean name.
	 * @param beanName the bean name
	 * @return the AOT service or {@code null}
	 */
	@Nullable
	public T findByBeanName(String beanName) {
		return this.beans.get(beanName);
	}


	/**
	 * Loader class used to actually load the services.
	 */
	public static class Loader {

		private final SpringFactoriesLoader springFactoriesLoader;

		private final ListableBeanFactory beanFactory;


		Loader(SpringFactoriesLoader springFactoriesLoader, @Nullable ListableBeanFactory beanFactory) {
			this.springFactoriesLoader = springFactoriesLoader;
			this.beanFactory = beanFactory;
		}


		/**
		 * Load all AOT services of the given type.
		 * @param <T> the service type
		 * @param type the service type
		 * @return a new {@link AotServices} instance
		 */
		public <T> AotServices<T> load(Class<T> type) {
			return new AotServices<T>(this.springFactoriesLoader.load(type), loadBeans(type));
		}

		private <T> Map<String, T> loadBeans(Class<T> type) {
			return (this.beanFactory != null) ? BeanFactoryUtils
					.beansOfTypeIncludingAncestors(this.beanFactory, type, true, false)
					: Collections.emptyMap();
		}

	}

}
