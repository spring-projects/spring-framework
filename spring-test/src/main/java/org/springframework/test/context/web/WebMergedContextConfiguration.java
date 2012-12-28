/*
 * Copyright 2002-2012 the original author or authors.
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
	 * Create a new {@code WebMergedContextConfiguration} instance for the
	 * supplied test class, resource locations, annotated classes, context
	 * initializers, active profiles, resource base path, and {@code ContextLoader}.
	 *
	 * <p>If a <code>null</code> value is supplied for <code>locations</code>,
	 * <code>classes</code>, or <code>activeProfiles</code> an empty array will
	 * be stored instead. If a <code>null</code> value is supplied for the
	 * <code>contextInitializerClasses</code> an empty set will be stored instead.
	 * If an <em>empty</em> value is supplied for the <code>resourceBasePath</code>
	 * an empty string will be used. Furthermore, active profiles will be sorted,
	 * and duplicate profiles will be removed.
	 *
	 * @param testClass the test class for which the configuration was merged
	 * @param locations the merged resource locations
	 * @param classes the merged annotated classes
	 * @param contextInitializerClasses the merged context initializer classes
	 * @param activeProfiles the merged active bean definition profiles
	 * @param resourceBasePath the resource path to the root directory of the web application
	 * @param contextLoader the resolved <code>ContextLoader</code>
	 */
	public WebMergedContextConfiguration(
			Class<?> testClass,
			String[] locations,
			Class<?>[] classes,
			Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> contextInitializerClasses,
			String[] activeProfiles, String resourceBasePath, ContextLoader contextLoader) {

		super(testClass, locations, classes, contextInitializerClasses, activeProfiles, contextLoader);

		this.resourceBasePath = !StringUtils.hasText(resourceBasePath) ? "" : resourceBasePath;
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
	 * Generate a unique hash code for all properties of this
	 * {@code WebMergedContextConfiguration} excluding the
	 * {@linkplain #getTestClass() test class}.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + resourceBasePath.hashCode();
		return result;
	}

	/**
	 * Determine if the supplied object is equal to this {@code WebMergedContextConfiguration}
	 * instance by comparing both object's {@linkplain #getLocations() locations},
	 * {@linkplain #getClasses() annotated classes},
	 * {@linkplain #getContextInitializerClasses() context initializer classes},
	 * {@linkplain #getActiveProfiles() active profiles},
	 * {@linkplain #getResourceBasePath() resource base path}, and the fully
	 * qualified names of their {@link #getContextLoader() ContextLoaders}.
	 */
	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}
		if (!(obj instanceof WebMergedContextConfiguration)) {
			return false;
		}

		final WebMergedContextConfiguration that = (WebMergedContextConfiguration) obj;

		return super.equals(that) && this.getResourceBasePath().equals(that.getResourceBasePath());
	}

	/**
	 * Provide a String representation of the {@linkplain #getTestClass() test class},
	 * {@linkplain #getLocations() locations}, {@linkplain #getClasses() annotated classes},
	 * {@linkplain #getContextInitializerClasses() context initializer classes},
	 * {@linkplain #getActiveProfiles() active profiles},
	 * {@linkplain #getResourceBasePath() resource base path}, and the name of the
	 * {@link #getContextLoader() ContextLoader}.
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)//
		.append("testClass", getTestClass())//
		.append("locations", ObjectUtils.nullSafeToString(getLocations()))//
		.append("classes", ObjectUtils.nullSafeToString(getClasses()))//
		.append("contextInitializerClasses", ObjectUtils.nullSafeToString(getContextInitializerClasses()))//
		.append("activeProfiles", ObjectUtils.nullSafeToString(getActiveProfiles()))//
		.append("resourceBasePath", getResourceBasePath())//
		.append("contextLoader", nullSafeToString(getContextLoader()))//
		.toString();
	}

}
