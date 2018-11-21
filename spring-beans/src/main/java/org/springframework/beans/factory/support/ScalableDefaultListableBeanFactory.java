/*
 * Copyright 2002-2018 the original author or authors.
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
package org.springframework.beans.factory.support;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.lang.Nullable;

/**
 * Extends the behavior of {@link DefaultListableBeanFactory} by providing a type to bean name index.
 * <p>
 * Since the configuration is frozen, bean definitions do not change and hence
 * we can build the mapping by going through all the beans in the registry once.
 * <p>
 * Spring's current implementation is O(N * M) where N is number of beans in
 * registry and M is the number of beans that get loaded during the
 * startup phase. To find the bean names, Spring computes the types for each of
 * the bean and matches that with the given type. It would do this against all
 * the N beans in the registry for every type that is being wired.
 * <p>
 * Instead the type is extracted once for all the beans and stored in a local
 * cache. The beans can then be retrieved in O(1) using the cache for all future
 * wiring of that type. Additionally we gather all the super types for our
 * bean-types for type match to satisfy wirings by parent types. If a given type
 * is not found in our cache, we revert to using Spring's default resolution
 * mechanism.
 *
 * @author  Rahul Shinde
 */
public class ScalableDefaultListableBeanFactory extends DefaultListableBeanFactory {

	/**
	 * Mapping of Class to 1..* bean names.
	 */
	private final Map<Class<?>, String[]> beanNamesByTypeMappings;

	/**
	 * Create a new ScalableDefaultListableBeanFactory.
	 */
	public ScalableDefaultListableBeanFactory() {
		this.beanNamesByTypeMappings = new ConcurrentHashMap<>();
	}

	/**
	 * Create a new DefaultListableBeanFactory with the given parent.
	 *
	 * @param parentBeanFactory the parent BeanFactory
	 */
	public ScalableDefaultListableBeanFactory(@Nullable BeanFactory parentBeanFactory) {
		super(parentBeanFactory);
		this.beanNamesByTypeMappings = new ConcurrentHashMap<>();
	}

	/**
	 * Looks for the mapping inside our local cache first,
	 * else delegates to the super class method {@link DefaultListableBeanFactory#getBeansOfType(Class, boolean, boolean)}.
	 */
	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		String[] resolvedBeanNames = null;
		if (type != null) {
			resolvedBeanNames = this.beanNamesByTypeMappings.get(type);
		}
		return resolvedBeanNames != null ? resolvedBeanNames : super.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}

	/**
	 * Acts as a hook to populates our cache just before creating the eager beans.
	 * The configuration should already be frozen as per Spring's implementation.
	 */
	@Override
	public void preInstantiateSingletons() throws BeansException {
		populateBeanTypeToNameMappings();
		super.preInstantiateSingletons();
	}

	/**
	 * Create mapping for types to bean-names using all the available bean definitions.
	 * Additionally we gather all the parent Class(es) for the given bean type as part of the mappings.
	 */
	private void populateBeanTypeToNameMappings() {
		Map<Class<?>, List<String>> localBeanTypeMappings = new HashMap<>();
		long startNanos = System.nanoTime();
		Arrays.stream(this.getBeanDefinitionNames())
			// Filter bean definitions names that are alias as these are redundant
			.filter(beanName -> !isAlias(beanName))
			.forEach(beanName -> {
				try {
					// Use's Spring implementation to identify the target class for a given bean name.
					Class<?> targetClass = getType(beanName);
					if (targetClass != null) {
						// fetch all the super types as well for this targetClass
						Set<Class<?>> classTypes = getSuperTypes(targetClass);
						// add the current type as well to the mapping
						classTypes.add(targetClass);
						classTypes.forEach(clazz -> localBeanTypeMappings.compute(clazz, (k, v) -> {
							v = v == null ? new ArrayList<>() : v;
							v.add(beanName);
							return v;
						}));
					}
				}
				catch (Exception ex) {
					// ignore the missing bean
					if (logger.isTraceEnabled()) {
						logger.trace("No bean named '" + beanName + "' found ");
					}
				}
			});

		// Convert values from List<String> to String[] as expected by getBeanNamesForType(...) API
		localBeanTypeMappings.forEach((k, v) -> {
			this.beanNamesByTypeMappings.put(k, v.toArray(new String[0]));
		});

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("DefaultListableBeanFactory beanNamesByType mappings populated with total records=%d in %d millis",
					this.beanNamesByTypeMappings.size(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)));
		}
	}

	/**
	 * Finds all the super types for the given {@link Class}.
	 *
	 * @param givenClazz input class
	 * @return set of all super types that it inherits from
	 */
	static Set<Class<?>> getSuperTypes(Class<?> givenClazz) {
		final Set<Class<?>> superTypeSet = new LinkedHashSet<>();
		final Queue<Class<?>> possibleCandidates = new ArrayDeque<>();
		possibleCandidates.add(givenClazz);
		if (givenClazz.isInterface()) {
			possibleCandidates.add(Object.class);
		}
		while (!possibleCandidates.isEmpty()) {
			Class<?> clz = possibleCandidates.remove();
			// skip the input class as we are only interested in parent types
			if (!clz.equals(givenClazz)) {
				superTypeSet.add(clz);
			}
			Class<?> superClz = clz.getSuperclass();
			if (superClz != null) {
				possibleCandidates.add(superClz);
			}
			possibleCandidates.addAll(Arrays.asList(clz.getInterfaces()));
		}
		return superTypeSet;
	}

}
