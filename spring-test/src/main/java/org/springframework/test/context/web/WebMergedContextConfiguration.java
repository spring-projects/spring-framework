/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context.web;

import java.util.Set;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.style.ToStringCreator;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@code WebMergedContextConfiguration} encapsulates the <em>merged</em>
 * context configuration declared on a test class and all of its superclasses
 * via {@link org.springframework.test.context.ContextConfiguration @ContextConfiguration},
 * {@link WebAppConfiguration @WebAppConfiguration}, and
 * {@link org.springframework.test.context.ActiveProfiles @ActiveProfiles}.
 *
 * <p>{@code WebMergedContextConfiguration} extends the contract of
 * {@link MergedContextConfiguration} by adding support for the {@link
 * #getResourceBasePath() resource base path} configured via {@code @WebAppConfiguration}.
 * This allows the {@link org.springframework.test.context.TestContext TestContext}
 * to properly cache the corresponding {@link
 * org.springframework.web.context.WebApplicationContext WebApplicationContext}
 * that was loaded using properties of this {@code WebMergedContextConfiguration}.
 *
 * @author Sam Brannen
 * @since 3.2
 * @see WebAppConfiguration
 * @see MergedContextConfiguration
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.ActiveProfiles
 * @see org.springframework.test.context.ContextConfigurationAttributes
 * @see org.springframework.test.context.SmartContextLoader#loadContext(MergedContextConfiguration)
 */
public class WebMergedContextConfiguration extends MergedContextConfiguration {

	private static final long serialVersionUID = 7323361588604247458L;

	private final String resourceBasePath;


	/**
	 * Create a new {@code WebMergedContextConfiguration} instance by copying
	 * all properties from the supplied {@code MergedContextConfiguration}.
	 * <p>If an <em>empty</em> value is supplied for the {@code resourceBasePath}
	 * an empty string will be used.
	 * @param resourceBasePath the resource path to the root directory of the web application
	 * @since 4.1
	 */
	public WebMergedContextConfiguration(MergedContextConfiguration mergedConfig, String resourceBasePath) {
		super(mergedConfig);
		this.resourceBasePath = !StringUtils.hasText(resourceBasePath) ? "" : resourceBasePath;
	}

	/**
	 * Create a new {@code WebMergedContextConfiguration} instance for the
	 * supplied parameters.
	 * <p>If a {@code null} value is supplied for {@code locations},
	 * {@code classes}, {@code activeProfiles}, {@code propertySourceLocations},
	 * or {@code propertySourceProperties} an empty array will be stored instead.
	 * If a {@code null} value is supplied for the
	 * {@code contextInitializerClasses} an empty set will be stored instead.
	 * If an <em>empty</em> value is supplied for the {@code resourceBasePath}
	 * an empty string will be used. Furthermore, active profiles will be sorted,
	 * and duplicate profiles will be removed.
	 * @param testClass the test class for which the configuration was merged
	 * @param locations the merged resource locations
	 * @param classes the merged annotated classes
	 * @param contextInitializerClasses the merged context initializer classes
	 * @param activeProfiles the merged active bean definition profiles
	 * @param propertySourceLocations the merged {@code PropertySource} locations
	 * @param propertySourceProperties the merged {@code PropertySource} properties
	 * @param resourceBasePath the resource path to the root directory of the web application
	 * @param contextLoader the resolved {@code ContextLoader}
	 * @param cacheAwareContextLoaderDelegate a cache-aware context loader
	 * delegate with which to retrieve the parent context
	 * @param parent the parent configuration or {@code null} if there is no parent
	 * @since 4.1
	 */
	public WebMergedContextConfiguration(Class<?> testClass, String[] locations, Class<?>[] classes,
			Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> contextInitializerClasses,
			String[] activeProfiles, String[] propertySourceLocations, String[] propertySourceProperties,
			String resourceBasePath, ContextLoader contextLoader,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate, MergedContextConfiguration parent) {

		this(testClass, locations, classes, contextInitializerClasses, activeProfiles, propertySourceLocations,
			propertySourceProperties, null, resourceBasePath, contextLoader, cacheAwareContextLoaderDelegate, parent);
	}

	/**
	 * Create a new {@code WebMergedContextConfiguration} instance for the
	 * supplied parameters.
	 * <p>If a {@code null} value is supplied for {@code locations},
	 * {@code classes}, {@code activeProfiles}, {@code propertySourceLocations},
	 * or {@code propertySourceProperties} an empty array will be stored instead.
	 * If a {@code null} value is supplied for {@code contextInitializerClasses}
	 * or {@code contextCustomizers}, an empty set will be stored instead.
	 * If an <em>empty</em> value is supplied for the {@code resourceBasePath}
	 * an empty string will be used. Furthermore, active profiles will be sorted,
	 * and duplicate profiles will be removed.
	 * @param testClass the test class for which the configuration was merged
	 * @param locations the merged context resource locations
	 * @param classes the merged annotated classes
	 * @param contextInitializerClasses the merged context initializer classes
	 * @param activeProfiles the merged active bean definition profiles
	 * @param propertySourceLocations the merged {@code PropertySource} locations
	 * @param propertySourceProperties the merged {@code PropertySource} properties
	 * @param contextCustomizers the context customizers
	 * @param resourceBasePath the resource path to the root directory of the web application
	 * @param contextLoader the resolved {@code ContextLoader}
	 * @param cacheAwareContextLoaderDelegate a cache-aware context loader
	 * delegate with which to retrieve the parent context
	 * @param parent the parent configuration or {@code null} if there is no parent
	 * @since 4.3
	 */
	public WebMergedContextConfiguration(Class<?> testClass, String[] locations, Class<?>[] classes,
			Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> contextInitializerClasses,
			String[] activeProfiles, String[] propertySourceLocations, String[] propertySourceProperties,
			Set<ContextCustomizer> contextCustomizers, String resourceBasePath, ContextLoader contextLoader,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate, MergedContextConfiguration parent) {

		super(testClass, locations, classes, contextInitializerClasses, activeProfiles, propertySourceLocations,
			propertySourceProperties, contextCustomizers, contextLoader, cacheAwareContextLoaderDelegate, parent);

		this.resourceBasePath = (StringUtils.hasText(resourceBasePath) ? resourceBasePath : "");
	}

	/**
	 * Get the resource path to the root directory of the web application for the
	 * {@linkplain #getTestClass() test class}, configured via {@code @WebAppConfiguration}.
	 * @see WebAppConfiguration
	 */
	public String getResourceBasePath() {
		return this.resourceBasePath;
	}


	/**
	 * Determine if the supplied object is equal to this {@code WebMergedContextConfiguration}
	 * instance by comparing both object's {@linkplain #getLocations() locations},
	 * {@linkplain #getClasses() annotated classes},
	 * {@linkplain #getContextInitializerClasses() context initializer classes},
	 * {@linkplain #getActiveProfiles() active profiles},
	 * {@linkplain #getResourceBasePath() resource base path},
	 * {@linkplain #getParent() parents}, and the fully qualified names of their
	 * {@link #getContextLoader() ContextLoaders}.
	 */
	@Override
	public boolean equals(Object other) {
		return (this == other || (super.equals(other) &&
				this.resourceBasePath.equals(((WebMergedContextConfiguration) other).resourceBasePath)));
	}

	/**
	 * Generate a unique hash code for all properties of this
	 * {@code WebMergedContextConfiguration} excluding the
	 * {@linkplain #getTestClass() test class}.
	 */
	@Override
	public int hashCode() {
		return super.hashCode() * 31 + this.resourceBasePath.hashCode();
	}

	/**
	 * Provide a String representation of the {@linkplain #getTestClass() test class},
	 * {@linkplain #getLocations() locations}, {@linkplain #getClasses() annotated classes},
	 * {@linkplain #getContextInitializerClasses() context initializer classes},
	 * {@linkplain #getActiveProfiles() active profiles},
	 * {@linkplain #getPropertySourceLocations() property source locations},
	 * {@linkplain #getPropertySourceProperties() property source properties},
	 * {@linkplain #getContextCustomizers() context customizers},
	 * {@linkplain #getResourceBasePath() resource base path}, the name of the
	 * {@link #getContextLoader() ContextLoader}, and the
	 * {@linkplain #getParent() parent configuration}.
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("testClass", getTestClass())
				.append("locations", ObjectUtils.nullSafeToString(getLocations()))
				.append("classes", ObjectUtils.nullSafeToString(getClasses()))
				.append("contextInitializerClasses", ObjectUtils.nullSafeToString(getContextInitializerClasses()))
				.append("activeProfiles", ObjectUtils.nullSafeToString(getActiveProfiles()))
				.append("propertySourceLocations", ObjectUtils.nullSafeToString(getPropertySourceLocations()))
				.append("propertySourceProperties", ObjectUtils.nullSafeToString(getPropertySourceProperties()))
				.append("contextCustomizers", getContextCustomizers())
				.append("resourceBasePath", getResourceBasePath())
				.append("contextLoader", nullSafeToString(getContextLoader()))
				.append("parent", getParent())
				.toString();
	}

}
