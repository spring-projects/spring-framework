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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.util.StringUtils.arrayToCommaDelimitedString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Unit tests for {@link ComponentScanSpec}.
 * 
 * @author Chris Beams
 * @since 3.1
 */
public class ComponentScanSpecTests {

	private CollatingProblemReporter problemReporter;
	private ClassLoader classLoader;


	@Before
	public void setUp() {
		problemReporter = new CollatingProblemReporter();
		classLoader = ClassUtils.getDefaultClassLoader();
	}

	@Test
	public void includeAnnotationConfig() {
		ComponentScanSpec spec = new ComponentScanSpec("org.p1");
		assertThat(spec.includeAnnotationConfig(), nullValue());
		spec.includeAnnotationConfig(true);
		assertThat(spec.validate(problemReporter), is(true));
		assertThat(spec.includeAnnotationConfig(), is(true));
		spec.includeAnnotationConfig(false);
		assertThat(spec.validate(problemReporter), is(true));
		assertThat(spec.includeAnnotationConfig(), is(false));
		spec.includeAnnotationConfig("trUE");
		assertThat(spec.validate(problemReporter), is(true));
		assertThat(spec.includeAnnotationConfig(), is(true));
		spec.includeAnnotationConfig("falSE");
		assertThat(spec.validate(problemReporter), is(true));
		assertThat(spec.includeAnnotationConfig(), is(false));
		spec.includeAnnotationConfig("");
		assertThat(spec.validate(problemReporter), is(true));
		assertThat(spec.includeAnnotationConfig(), is(false));
		spec.includeAnnotationConfig((String)null);
		assertThat(spec.validate(problemReporter), is(true));
		assertThat(spec.includeAnnotationConfig(), is(false));
		spec.includeAnnotationConfig((Boolean)null);
		assertThat(spec.validate(problemReporter), is(true));
		assertThat(spec.includeAnnotationConfig(), nullValue());
	}

	@Test
	public void resourcePattern() {
		ComponentScanSpec spec = new ComponentScanSpec("org.p1");
		assertThat(spec.resourcePattern(), nullValue());
		assertThat(spec.validate(problemReporter), is(true));
		spec.resourcePattern("**/Foo*.class");
		assertThat(spec.resourcePattern(), is("**/Foo*.class"));
		assertThat(spec.validate(problemReporter), is(true));
		spec.resourcePattern("");
		assertThat(spec.resourcePattern(), is("**/Foo*.class"));
		assertThat(spec.validate(problemReporter), is(true));
		spec.resourcePattern(null);
		assertThat(spec.resourcePattern(), is("**/Foo*.class"));
		assertThat(spec.validate(problemReporter), is(true));
	}

	@Test
	public void useDefaultFilters() {
		ComponentScanSpec spec = new ComponentScanSpec("org.p1");
		assertThat(spec.useDefaultFilters(), nullValue());
		assertThat(spec.validate(problemReporter), is(true));
		spec.useDefaultFilters((Boolean)null);
		assertThat(spec.useDefaultFilters(), nullValue());
		assertThat(spec.validate(problemReporter), is(true));
		spec.useDefaultFilters(true);
		assertThat(spec.useDefaultFilters(), is(true));
		assertThat(spec.validate(problemReporter), is(true));
		spec.useDefaultFilters(false);
		assertThat(spec.useDefaultFilters(), is(false));
		assertThat(spec.validate(problemReporter), is(true));
		spec.useDefaultFilters("trUE");
		assertThat(spec.useDefaultFilters(), is(true));
		assertThat(spec.validate(problemReporter), is(true));
		spec.useDefaultFilters("falSE");
		assertThat(spec.useDefaultFilters(), is(false));
		assertThat(spec.validate(problemReporter), is(true));
		spec.useDefaultFilters("");
		assertThat(spec.useDefaultFilters(), is(false));
		assertThat(spec.validate(problemReporter), is(true));
		spec.useDefaultFilters((String)null);
		assertThat(spec.useDefaultFilters(), is(false));
		assertThat(spec.validate(problemReporter), is(true));
	}

	@Test
	public void includeFilters() {
		ComponentScanSpec spec = new ComponentScanSpec("org.p1");
		spec.includeFilters(
				new AnnotationTypeFilter(MyAnnotation.class),
				new AssignableTypeFilter(Object.class));
		assertThat(spec.validate(problemReporter), is(true));
		assertThat(spec.includeFilters().length, is(2));
	}

	@Test
	public void stringIncludeExcludeFilters() {
		ComponentScanSpec spec = new ComponentScanSpec("org.p1");
		spec.addIncludeFilter("annotation", MyAnnotation.class.getName(), classLoader);
		spec.addExcludeFilter("assignable", Object.class.getName(), classLoader);
		spec.addExcludeFilter("annotation", Override.class.getName(), classLoader);
		assertThat(spec.validate(problemReporter), is(true));
		assertThat(spec.includeFilters().length, is(1));
		assertThat(spec.excludeFilters().length, is(2));
	}

	@Test
	public void bogusStringIncludeFilter() throws IOException {
		ComponentScanSpec spec = new ComponentScanSpec("org.p1");
		spec.addIncludeFilter("bogus-type", "bogus-expr", classLoader);
		assertThat(spec.validate(problemReporter), is(false));
		assertThat(spec.includeFilters().length, is(1));
		try {
			spec.includeFilters()[0].match(null, null);
			fail("expected placholder TypeFilter to throw exception");
		} catch (UnsupportedOperationException ex) {
			// expected
		}
	}

	@Test
	public void exerciseFilterTypes() throws IOException {
		ComponentScanSpec spec = new ComponentScanSpec("org.p1");
		spec.addIncludeFilter("aspectj", "*..Bogus", classLoader);
		assertThat(spec.validate(problemReporter), is(true));
		spec.addIncludeFilter("regex", ".*Foo", classLoader);
		assertThat(spec.validate(problemReporter), is(true));
		spec.addIncludeFilter("custom", StubTypeFilter.class.getName(), classLoader);
		assertThat(spec.validate(problemReporter), is(true));
		spec.addIncludeFilter("custom", "org.NonExistentTypeFilter", classLoader);
		assertThat(spec.validate(problemReporter), is(false));
		spec.addIncludeFilter("custom", NonNoArgResolver.class.getName(), classLoader);
		assertThat(spec.validate(problemReporter), is(false));
	}

	@Test
	public void missingBasePackages() {
		ComponentScanSpec spec = new ComponentScanSpec();
		assertThat(spec.validate(problemReporter), is(false));
	}

	@Test
	public void withBasePackageViaAdderMethod() {
		ComponentScanSpec spec = new ComponentScanSpec();
		spec.addBasePackage("org.p1");
		spec.addBasePackage("org.p2");
		assertThat(spec.validate(problemReporter), is(true));
		assertExactContents(spec.basePackages(), "org.p1", "org.p2");
	}

	@Test
	public void withBasePackagesViaStringConstructor() {
		ComponentScanSpec spec = new ComponentScanSpec("org.p1", "org.p2");
		assertThat(spec.validate(problemReporter), is(true));
		assertExactContents(spec.basePackages(), "org.p1", "org.p2");
	}

	@Test
	public void withBasePackagesViaClassConstructor() {
		ComponentScanSpec spec = new ComponentScanSpec(java.lang.Object.class, java.io.Closeable.class);
		assertThat(spec.validate(problemReporter), is(true));
		assertExactContents(spec.basePackages(), "java.lang", "java.io");
	}

	@Test
	public void forDelimitedPackages() {
		ComponentScanSpec spec = ComponentScanSpec.forDelimitedPackages("pkg.one,pkg.two");
		assertTrue(ObjectUtils.containsElement(spec.basePackages(), "pkg.one"));
		assertTrue(ObjectUtils.containsElement(spec.basePackages(), "pkg.two"));
		assertThat(spec.basePackages().length, is(2));
	}

	@Test
	public void withSomeEmptyBasePackages() {
		ComponentScanSpec spec = new ComponentScanSpec("org.p1", "", "org.p3");
		assertThat(spec.validate(problemReporter), is(true));
		assertExactContents(spec.basePackages(), "org.p1", "org.p3");
	}

	@Test
	public void withAllEmptyBasePackages() {
		ComponentScanSpec spec = new ComponentScanSpec("", "", "");
		assertThat(spec.validate(problemReporter), is(false));
	}

	@Test
	public void withInstanceBeanNameGenerator() {
		ComponentScanSpec spec = new ComponentScanSpec("org.p1");
		assertThat(spec.beanNameGenerator(), nullValue());
		BeanNameGenerator bng = new DefaultBeanNameGenerator();
		spec.beanNameGenerator(bng);
		assertThat(spec.validate(problemReporter), is(true));
		assertThat(spec.beanNameGenerator(), is(bng));
	}

	@Test
	public void withStringBeanNameGenerator() {
		ComponentScanSpec spec = new ComponentScanSpec("org.p1");
		spec.beanNameGenerator(DefaultBeanNameGenerator.class.getName(), classLoader);
		assertThat(spec.validate(problemReporter), is(true));
		assertThat(spec.beanNameGenerator(), instanceOf(DefaultBeanNameGenerator.class));
	}

	@Test
	public void withInstanceScopeMetadataResolver() {
		ComponentScanSpec spec = new ComponentScanSpec("org.p1");
		assertThat(spec.scopeMetadataResolver(), nullValue());
		ScopeMetadataResolver smr = new AnnotationScopeMetadataResolver();
		spec.scopeMetadataResolver(smr);
		assertThat(spec.validate(problemReporter), is(true));
		assertThat(spec.scopeMetadataResolver(), is(smr));
	}

	@Test
	public void withStringScopeMetadataResolver() {
		ComponentScanSpec spec = new ComponentScanSpec("org.p1");
		spec.scopeMetadataResolver(AnnotationScopeMetadataResolver.class.getName(), classLoader);
		assertThat(spec.validate(problemReporter), is(true));
		assertThat(spec.scopeMetadataResolver(), instanceOf(AnnotationScopeMetadataResolver.class));
	}

	@Test
	public void withNonAssignableStringScopeMetadataResolver() {
		ComponentScanSpec spec = new ComponentScanSpec("org.p1");
		spec.scopeMetadataResolver(Object.class.getName(), classLoader);
		assertThat(spec.validate(problemReporter), is(false));
	}

	@Test
	public void withNonExistentStringScopeMetadataResolver() {
		ComponentScanSpec spec = new ComponentScanSpec("org.p1");
		spec.scopeMetadataResolver("org.Bogus", classLoader);
		assertThat(spec.validate(problemReporter), is(false));
	}

	@Test
	public void withNonNoArgStringScopeMetadataResolver() {
		ComponentScanSpec spec = new ComponentScanSpec("org.p1");
		spec.scopeMetadataResolver(NonNoArgResolver.class.getName(), classLoader);
		assertThat(spec.validate(problemReporter), is(false));
	}

	@Test
	public void withStringScopedProxyMode() {
		ComponentScanSpec spec = new ComponentScanSpec("org.p1");
		spec.scopedProxyMode("targetCLASS");
		assertThat(spec.validate(problemReporter), is(true));
		assertThat(spec.scopedProxyMode(), is(ScopedProxyMode.TARGET_CLASS));
		spec.scopedProxyMode("interFACES");
		assertThat(spec.validate(problemReporter), is(true));
		assertThat(spec.scopedProxyMode(), is(ScopedProxyMode.INTERFACES));
		spec.scopedProxyMode("nO");
		assertThat(spec.validate(problemReporter), is(true));
		assertThat(spec.scopedProxyMode(), is(ScopedProxyMode.NO));
		spec.scopedProxyMode("bogus");
		assertThat(spec.validate(problemReporter), is(false));
		assertThat(spec.scopedProxyMode(), nullValue());
	}

	@Test
	public void withScopeMetadataResolverAndScopedProxyMode() {
		ComponentScanSpec spec = new ComponentScanSpec("org.p1");
		spec.scopeMetadataResolver(new AnnotationScopeMetadataResolver());
		assertThat(spec.validate(problemReporter), is(true));
		spec.scopedProxyMode(ScopedProxyMode.INTERFACES);
		assertThat(spec.validate(problemReporter), is(false));
	}

	@Test
	public void addBasePackage() {
		ComponentScanSpec spec = new ComponentScanSpec();
		spec.addBasePackage("foo.bar");
		assertThat(spec.validate(problemReporter), is(true));
		assertThat(spec.basePackages().length, is(1));
	}

	@Test
	public void addBasePackageWithConstructor() {
		ComponentScanSpec spec = new ComponentScanSpec("my.pkg");
		spec.addBasePackage("foo.bar");
		assertThat(spec.validate(problemReporter), is(true));
		assertThat(spec.basePackages().length, is(2));
	}

	@Test
	public void addExcludeFilterString() {
		ComponentScanSpec spec = new ComponentScanSpec("my.pkg");
		spec.addExcludeFilter("annotation", MyAnnotation.class.getName(), ClassUtils.getDefaultClassLoader());
		assertThat(spec.validate(problemReporter), is(true));
		assertThat(spec.excludeFilters().length, is(1));
		assertThat(spec.excludeFilters()[0], instanceOf(AnnotationTypeFilter.class));
	}

	@Test(expected=BeanDefinitionParsingException.class)
	public void withFailFastProblemReporter() {
		new ComponentScanSpec().validate(new FailFastProblemReporter());
	}

	private <T> void assertExactContents(T[] actual, T... expected) {
		if (actual.length >= expected.length) {
			for (int i = 0; i < expected.length; i++) {
				assertThat(
						String.format("element number %d in actual is incorrect. actual: [%s], expected: [%s]",
								i, arrayToCommaDelimitedString(actual), arrayToCommaDelimitedString(expected)),
						actual[i], equalTo(expected[i]));
			}
		}
		assertThat(String.format("actual contains incorrect number of arguments. actual: [%s], expected: [%s]",
					arrayToCommaDelimitedString(actual), arrayToCommaDelimitedString(expected)),
				actual.length, equalTo(expected.length));
	}


	private static class CollatingProblemReporter implements ProblemReporter {

		private List<Problem> errors = new ArrayList<Problem>();

		private List<Problem> warnings = new ArrayList<Problem>();


		public void fatal(Problem problem) {
			throw new BeanDefinitionParsingException(problem);
		}

		public void error(Problem problem) {
			this.errors.add(problem);
		}

		@SuppressWarnings("unused")
		public Problem[] getErrors() {
			return this.errors.toArray(new Problem[this.errors.size()]);
		}

		public void warning(Problem problem) {
			System.out.println(problem);
			this.warnings.add(problem);
		}

		@SuppressWarnings("unused")
		public Problem[] getWarnings() {
			return this.warnings.toArray(new Problem[this.warnings.size()]);
		}
	}


	private static class NonNoArgResolver implements ScopeMetadataResolver {
		public ScopeMetadata resolveScopeMetadata(BeanDefinition definition) {
			throw new UnsupportedOperationException();
		}
	}

	private static class StubTypeFilter implements TypeFilter {
		public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
				throws IOException {
			throw new UnsupportedOperationException();
		}
	}

}