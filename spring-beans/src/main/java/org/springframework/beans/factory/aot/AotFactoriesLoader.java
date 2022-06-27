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
import java.util.List;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;

/**
 * AOT specific factory loading mechanism for internal use within the framework.
 *
 * <p>Loads and instantiates factories of a given type from
 * {@value #FACTORIES_RESOURCE_LOCATION} and merges them with matching beans
 * from a {@link ListableBeanFactory}.
 *
 * @author Phillip Webb
 * @since 6.0
 * @see SpringFactoriesLoader
 */
public class AotFactoriesLoader {

	/**
	 * The location to look for AOT factories.
	 */
	public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring/aot.factories";


	private final ListableBeanFactory beanFactory;

	private final SpringFactoriesLoader factoriesLoader;


	/**
	 * Create a new {@link AotFactoriesLoader} instance backed by the given bean
	 * factory.
	 * @param beanFactory the bean factory to use
	 */
	public AotFactoriesLoader(ListableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "'beanFactory' must not be null");
		ClassLoader classLoader = (beanFactory instanceof ConfigurableBeanFactory configurableBeanFactory)
				? configurableBeanFactory.getBeanClassLoader() : null;
		this.beanFactory = beanFactory;
		this.factoriesLoader = SpringFactoriesLoader.forResourceLocation(FACTORIES_RESOURCE_LOCATION,
				classLoader);
	}

	/**
	 * Create a new {@link AotFactoriesLoader} instance backed by the given bean
	 * factory and loading items from the given {@link SpringFactoriesLoader}
	 * rather than from {@value #FACTORIES_RESOURCE_LOCATION}.
	 * @param beanFactory the bean factory to use
	 * @param factoriesLoader the factories loader to use
	 */
	public AotFactoriesLoader(ListableBeanFactory beanFactory,
			SpringFactoriesLoader factoriesLoader) {

		Assert.notNull(beanFactory, "'beanFactory' must not be null");
		Assert.notNull(factoriesLoader, "'factoriesLoader' must not be null");
		this.beanFactory = beanFactory;
		this.factoriesLoader = factoriesLoader;
	}


	/**
	 * Load items from factories file and merge them with any beans defined in
	 * the {@link DefaultListableBeanFactory}.
	 * @param <T> the item type
	 * @param type the item type to load
	 * @return a list of loaded instances
	 */
	public <T> List<T> load(Class<T> type) {
		List<T> result = new ArrayList<>();
		result.addAll(BeanFactoryUtils
				.beansOfTypeIncludingAncestors(this.beanFactory, type, true, false)
				.values());
		result.addAll(this.factoriesLoader.load(type));
		AnnotationAwareOrderComparator.sort(result);
		return Collections.unmodifiableList(result);
	}

}
