/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.context.annotation;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.parsing.ProblemCollector;
import org.springframework.beans.factory.support.BeanDefinitionDefaults;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.config.AbstractFeatureSpecification;
import org.springframework.context.config.FeatureSpecificationExecutor;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AspectJTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Specifies the configuration of Spring's <em>component-scanning</em> feature.
 * May be used directly within a {@link Feature @Feature} method, or indirectly
 * through the {@link ComponentScan @ComponentScan} annotation.
 *
 * @author Chris Beams
 * @since 3.1
 * @see ComponentScan
 * @see ComponentScanAnnotationParser
 * @see ComponentScanBeanDefinitionParser
 * @see ComponentScanExecutor
 */
public final class ComponentScanSpec extends AbstractFeatureSpecification {

	private static final Class<? extends FeatureSpecificationExecutor> EXECUTOR_TYPE = ComponentScanExecutor.class;

	private Boolean includeAnnotationConfig = null;
	private String resourcePattern = null;
	private List<String> basePackages = new ArrayList<String>();
	private Object beanNameGenerator = null;
	private Object scopeMetadataResolver = null;
	private Object scopedProxyMode = null;
	private Boolean useDefaultFilters = null;
	private List<Object> includeFilters = new ArrayList<Object>();
	private List<Object> excludeFilters = new ArrayList<Object>();

	private BeanDefinitionDefaults beanDefinitionDefaults;
	private String[] autowireCandidatePatterns;

	private ClassLoader classLoader;

	/**
	 * Package-visible constructor for use by {@link ComponentScanBeanDefinitionParser}.
	 * End users should always call String... or Class<?>... constructors to specify
	 * base packages.
	 *
	 * @see #validate()
	 */
	ComponentScanSpec() {
		super(EXECUTOR_TYPE);
	}

	/**
	 * 
	 * @param basePackages
	 * @see #forDelimitedPackages(String)
	 */
	public ComponentScanSpec(String... basePackages) {
		this();
		Assert.notEmpty(basePackages, "At least one base package must be specified");
		for (String basePackage : basePackages) {
			addBasePackage(basePackage);
		}
	}

	public ComponentScanSpec(Class<?>... basePackageClasses) {
		this(packagesFor(basePackageClasses));
	}


	public ComponentScanSpec includeAnnotationConfig(Boolean includeAnnotationConfig) {
		this.includeAnnotationConfig = includeAnnotationConfig;
		return this;
	}

	ComponentScanSpec includeAnnotationConfig(String includeAnnotationConfig) {
		if (StringUtils.hasText(includeAnnotationConfig)) {
			this.includeAnnotationConfig = Boolean.valueOf(includeAnnotationConfig);
		}
		return this;
	}

	Boolean includeAnnotationConfig() {
		return this.includeAnnotationConfig;
	}

	public ComponentScanSpec resourcePattern(String resourcePattern) {
		if (StringUtils.hasText(resourcePattern)) {
			this.resourcePattern = resourcePattern;
		}
		return this;
	}

	String resourcePattern() {
		return resourcePattern;
	}

	ComponentScanSpec addBasePackage(String basePackage) {
		if (StringUtils.hasText(basePackage)) {
			this.basePackages.add(basePackage);
		}
		return this;
	}

	/**
	 * Return the set of base packages specified, never {@code null}, never empty
	 * post-validation.
	 * @see #doValidate(SimpleProblemReporter)
	 */
	String[] basePackages() {
		return this.basePackages.toArray(new String[this.basePackages.size()]);
	}

	public ComponentScanSpec beanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator = beanNameGenerator;
		return this;
	}

	/**
	 * Set the class name of the BeanNameGenerator to be used and the ClassLoader
	 * to load it.
	 */
	ComponentScanSpec beanNameGenerator(String beanNameGenerator, ClassLoader classLoader) {
		setClassLoader(classLoader);
		if (StringUtils.hasText(beanNameGenerator)) {
			this.beanNameGenerator = beanNameGenerator;
		}
		return this;
	}

	BeanNameGenerator beanNameGenerator() {
		return nullSafeTypedObject(this.beanNameGenerator, BeanNameGenerator.class);
	}

	public ComponentScanSpec scopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
		this.scopeMetadataResolver = scopeMetadataResolver;
		return this;
	}

	ComponentScanSpec scopeMetadataResolver(String scopeMetadataResolver, ClassLoader classLoader) {
		setClassLoader(classLoader);
		if (StringUtils.hasText(scopeMetadataResolver)) {
			this.scopeMetadataResolver = scopeMetadataResolver;
		}
		return this;
	}

	ScopeMetadataResolver scopeMetadataResolver() {
		return nullSafeTypedObject(this.scopeMetadataResolver, ScopeMetadataResolver.class);
	}

	public ComponentScanSpec scopedProxyMode(ScopedProxyMode scopedProxyMode) {
		this.scopedProxyMode = scopedProxyMode;
		return this;
	}

	ComponentScanSpec scopedProxyMode(String scopedProxyMode) {
		if (StringUtils.hasText(scopedProxyMode)) {
			this.scopedProxyMode = scopedProxyMode;
		}
		return this;
	}

	ScopedProxyMode scopedProxyMode() {
		return nullSafeTypedObject(this.scopedProxyMode, ScopedProxyMode.class);
	}

	public ComponentScanSpec useDefaultFilters(Boolean useDefaultFilters) {
		this.useDefaultFilters = useDefaultFilters;
		return this;
	}

	ComponentScanSpec useDefaultFilters(String useDefaultFilters) {
		if (StringUtils.hasText(useDefaultFilters)) {
			this.useDefaultFilters = Boolean.valueOf(useDefaultFilters);
		}
		return this;
	}

	Boolean useDefaultFilters() {
		return this.useDefaultFilters;
	}

	public ComponentScanSpec includeFilters(TypeFilter... includeFilters) {
		this.includeFilters.clear();
		for (TypeFilter filter : includeFilters) {
			addIncludeFilter(filter);
		}
		return this;
	}

	ComponentScanSpec addIncludeFilter(TypeFilter includeFilter) {
		Assert.notNull(includeFilter, "includeFilter must not be null");
		this.includeFilters.add(includeFilter);
		return this;
	}

	ComponentScanSpec addIncludeFilter(String filterType, String expression, ClassLoader classLoader) {
		this.includeFilters.add(new FilterTypeDescriptor(filterType, expression, classLoader));
		return this;
	}

	TypeFilter[] includeFilters() {
		return this.includeFilters.toArray(new TypeFilter[this.includeFilters.size()]);
	}

	public ComponentScanSpec excludeFilters(TypeFilter... excludeFilters) {
		this.excludeFilters.clear();
		for (TypeFilter filter : excludeFilters) {
			addExcludeFilter(filter);
		}
		return this;
	}

	ComponentScanSpec addExcludeFilter(TypeFilter excludeFilter) {
		Assert.notNull(excludeFilter, "excludeFilter must not be null");
		this.excludeFilters.add(excludeFilter);
		return this;
	}

	ComponentScanSpec addExcludeFilter(String filterType, String expression, ClassLoader classLoader) {
		this.excludeFilters.add(new FilterTypeDescriptor(filterType, expression, classLoader));
		return this;
	}

	TypeFilter[] excludeFilters() {
		return this.excludeFilters.toArray(new TypeFilter[this.excludeFilters.size()]);
	}

	ComponentScanSpec beanDefinitionDefaults(BeanDefinitionDefaults beanDefinitionDefaults) {
		this.beanDefinitionDefaults = beanDefinitionDefaults;
		return this;
	}

	BeanDefinitionDefaults beanDefinitionDefaults() {
		return this.beanDefinitionDefaults;
	}

	ComponentScanSpec autowireCandidatePatterns(String[] autowireCandidatePatterns) {
		this.autowireCandidatePatterns = autowireCandidatePatterns;
		return this;
	}

	String[] autowireCandidatePatterns() {
		return this.autowireCandidatePatterns;
	}


	/**
	 * Create a ComponentScanSpec from a single string containing
	 * delimited package names.
	 * @see ConfigurableApplicationContext#CONFIG_LOCATION_DELIMITERS
	 */
	static ComponentScanSpec forDelimitedPackages(String basePackages) {
		Assert.notNull(basePackages, "base packages must not be null");
		return new ComponentScanSpec(
				StringUtils.tokenizeToStringArray(basePackages,
						ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
	}

	public void doValidate(ProblemCollector problems) {
		if(this.basePackages.isEmpty()) {
			problems.error("At least one base package must be specified");
		}

		if(this.beanNameGenerator instanceof String) {
			this.beanNameGenerator = instantiateUserDefinedType("bean name generator", BeanNameGenerator.class, this.beanNameGenerator, this.classLoader, problems);
		}

		if(this.scopeMetadataResolver instanceof String) {
			this.scopeMetadataResolver = instantiateUserDefinedType("scope metadata resolver", ScopeMetadataResolver.class, this.scopeMetadataResolver, this.classLoader, problems);
		}

		if (this.scopedProxyMode instanceof String) {
			if ("targetClass".equalsIgnoreCase((String)this.scopedProxyMode)) {
				this.scopedProxyMode = ScopedProxyMode.TARGET_CLASS;
			}
			else if ("interfaces".equalsIgnoreCase((String)this.scopedProxyMode)) {
				this.scopedProxyMode = ScopedProxyMode.INTERFACES;
			}
			else if ("no".equalsIgnoreCase((String)this.scopedProxyMode)) {
				this.scopedProxyMode = ScopedProxyMode.NO;
			}
			else {
				problems.error("invalid scoped proxy mode [%s] supported modes are " +
						"'no', 'interfaces' and 'targetClass'");
				this.scopedProxyMode = null;
			}
		}

		if (this.scopeMetadataResolver != null && this.scopedProxyMode != null) {
			problems.error("Cannot define both scope metadata resolver and scoped proxy mode");
		}

		for (int i = 0; i < this.includeFilters.size(); i++) {
			if (this.includeFilters.get(i) instanceof FilterTypeDescriptor) {
				this.includeFilters.set(i, ((FilterTypeDescriptor)this.includeFilters.get(i)).createTypeFilter(problems));
			}
		}

		for (int i = 0; i < this.excludeFilters.size(); i++) {
			if (this.excludeFilters.get(i) instanceof FilterTypeDescriptor) {
				this.excludeFilters.set(i, ((FilterTypeDescriptor)this.excludeFilters.get(i)).createTypeFilter(problems));
			}
		}
	}

	private static Object instantiateUserDefinedType(String description, Class<?> targetType, Object className, ClassLoader classLoader, ProblemCollector problems) {
		Assert.isInstanceOf(String.class, className, "userType must be of type String");
		Assert.notNull(classLoader, "classLoader must not be null");
		Assert.notNull(targetType, "targetType must not be null");
		Object instance = null;
		try {
			instance = classLoader.loadClass((String)className).newInstance();
			if (!targetType.isAssignableFrom(instance.getClass())) {
				problems.error(description + " class name must be assignable to " + targetType.getSimpleName());
				instance = null;
			}
		}
		catch (ClassNotFoundException ex) {
			problems.error(String.format(description + " class [%s] not found", className), ex);
		}
		catch (Exception ex) {
			problems.error(String.format("Unable to instantiate %s class [%s] for " +
					"strategy [%s]. Has a no-argument constructor been provided?",
					description, className, targetType.getClass().getSimpleName()), ex);
		}
		return instance;
	}

	private void setClassLoader(ClassLoader classLoader) {
		Assert.notNull(classLoader, "classLoader must not be null");
		if (this.classLoader == null) {
			this.classLoader = classLoader;
		}
		else {
			Assert.isTrue(this.classLoader == classLoader, "A classLoader has already been assigned " +
					"and the supplied classLoader is not the same instance. Use the same classLoader " +
					"for all string-based class properties.");
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T nullSafeTypedObject(Object object, Class<T> type) {
		if (object != null) {
			if (!(type.isAssignableFrom(object.getClass()))) {
				throw new IllegalStateException(
						String.format("field must be of type %s but was actually of type %s", type, object.getClass()));
			}
		}
		return (T)object;
	}

	private static String[] packagesFor(Class<?>[] classes) {
		ArrayList<String> packages = new ArrayList<String>();
		for (Class<?> clazz : classes) {
			packages.add(clazz.getPackage().getName());
		}
		return packages.toArray(new String[packages.size()]);
	}


	private static class FilterTypeDescriptor {
		private String filterType;
		private String expression;
		private ClassLoader classLoader;

		FilterTypeDescriptor(String filterType, String expression, ClassLoader classLoader) {
			Assert.notNull(filterType, "filterType must not be null");
			Assert.notNull(expression, "expression must not be null");
			Assert.notNull(classLoader, "classLoader must not be null");
			this.filterType = filterType;
			this.expression = expression;
			this.classLoader = classLoader;
		}

		@SuppressWarnings("unchecked")
		TypeFilter createTypeFilter(ProblemCollector problems) {
			try {
				if ("annotation".equalsIgnoreCase(this.filterType)) {
					return new AnnotationTypeFilter((Class<Annotation>) this.classLoader.loadClass(this.expression));
				}
				else if ("assignable".equalsIgnoreCase(this.filterType)
						|| "assignable_type".equalsIgnoreCase(this.filterType)) {
					return new AssignableTypeFilter(this.classLoader.loadClass(this.expression));
				}
				else if ("aspectj".equalsIgnoreCase(this.filterType)) {
					return new AspectJTypeFilter(this.expression, this.classLoader);
				}
				else if ("regex".equalsIgnoreCase(this.filterType)) {
					return new RegexPatternTypeFilter(Pattern.compile(this.expression));
				}
				else if ("custom".equalsIgnoreCase(this.filterType)) {
					Class<?> filterClass = this.classLoader.loadClass(this.expression);
					if (!TypeFilter.class.isAssignableFrom(filterClass)) {
						problems.error(String.format("custom type filter class [%s] must be assignable to %s",
								this.expression, TypeFilter.class));
					}
					return (TypeFilter) BeanUtils.instantiateClass(filterClass);
				}
				else {
					problems.error(String.format("Unsupported filter type [%s]; supported types are: " +
							"'annotation', 'assignable[_type]', 'aspectj', 'regex', 'custom'", this.filterType));
				}
			} catch (ClassNotFoundException ex) {
				problems.error("Type filter class not found: " + this.expression, ex);
			} catch (Exception ex) {
				problems.error(ex.getMessage(), ex.getCause());
			}

			return new PlaceholderTypeFilter();
		}


		private class PlaceholderTypeFilter implements TypeFilter {

			public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
					throws IOException {
				throw new UnsupportedOperationException(
						String.format("match() method for placeholder type filter for " +
								"{filterType=%s,expression=%s} should never be invoked",
								filterType, expression));
			}

		}
	}


}
