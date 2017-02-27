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

package org.springframework.test.context.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.ContextLoader;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.context.support.ContextLoaderUtils.*;

/**
 * Unit tests for {@link ContextLoaderUtils} involving context hierarchies.
 *
 * @author Sam Brannen
 * @since 3.2.2
 */
public class ContextLoaderUtilsContextHierarchyTests extends AbstractContextConfigurationUtilsTests {

	private void debugConfigAttributes(List<ContextConfigurationAttributes> configAttributesList) {
		// for (ContextConfigurationAttributes configAttributes : configAttributesList) {
		// System.err.println(configAttributes);
		// }
	}

	@Test(expected = IllegalStateException.class)
	public void resolveContextHierarchyAttributesForSingleTestClassWithContextConfigurationAndContextHierarchy() {
		resolveContextHierarchyAttributes(SingleTestClassWithContextConfigurationAndContextHierarchy.class);
	}

	@Test(expected = IllegalStateException.class)
	public void resolveContextHierarchyAttributesForSingleTestClassWithContextConfigurationAndContextHierarchyOnSingleMetaAnnotation() {
		resolveContextHierarchyAttributes(SingleTestClassWithContextConfigurationAndContextHierarchyOnSingleMetaAnnotation.class);
	}

	@Test
	public void resolveContextHierarchyAttributesForSingleTestClassWithImplicitSingleLevelContextHierarchy() {
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(BareAnnotations.class);
		assertEquals(1, hierarchyAttributes.size());
		List<ContextConfigurationAttributes> configAttributesList = hierarchyAttributes.get(0);
		assertEquals(1, configAttributesList.size());
		debugConfigAttributes(configAttributesList);
	}

	@Test
	public void resolveContextHierarchyAttributesForSingleTestClassWithSingleLevelContextHierarchy() {
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(SingleTestClassWithSingleLevelContextHierarchy.class);
		assertEquals(1, hierarchyAttributes.size());
		List<ContextConfigurationAttributes> configAttributesList = hierarchyAttributes.get(0);
		assertEquals(1, configAttributesList.size());
		debugConfigAttributes(configAttributesList);
	}

	@Test
	public void resolveContextHierarchyAttributesForSingleTestClassWithSingleLevelContextHierarchyFromMetaAnnotation() {
		Class<SingleTestClassWithSingleLevelContextHierarchyFromMetaAnnotation> testClass = SingleTestClassWithSingleLevelContextHierarchyFromMetaAnnotation.class;
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(testClass);
		assertEquals(1, hierarchyAttributes.size());

		List<ContextConfigurationAttributes> configAttributesList = hierarchyAttributes.get(0);
		assertNotNull(configAttributesList);
		assertEquals(1, configAttributesList.size());
		debugConfigAttributes(configAttributesList);
		assertAttributes(configAttributesList.get(0), testClass, new String[] { "A.xml" }, EMPTY_CLASS_ARRAY,
			ContextLoader.class, true);
	}

	@Test
	public void resolveContextHierarchyAttributesForSingleTestClassWithTripleLevelContextHierarchy() {
		Class<SingleTestClassWithTripleLevelContextHierarchy> testClass = SingleTestClassWithTripleLevelContextHierarchy.class;
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(testClass);
		assertEquals(1, hierarchyAttributes.size());

		List<ContextConfigurationAttributes> configAttributesList = hierarchyAttributes.get(0);
		assertNotNull(configAttributesList);
		assertEquals(3, configAttributesList.size());
		debugConfigAttributes(configAttributesList);
		assertAttributes(configAttributesList.get(0), testClass, new String[] { "A.xml" }, EMPTY_CLASS_ARRAY,
			ContextLoader.class, true);
		assertAttributes(configAttributesList.get(1), testClass, new String[] { "B.xml" }, EMPTY_CLASS_ARRAY,
			ContextLoader.class, true);
		assertAttributes(configAttributesList.get(2), testClass, new String[] { "C.xml" }, EMPTY_CLASS_ARRAY,
			ContextLoader.class, true);
	}

	@Test
	public void resolveContextHierarchyAttributesForTestClassHierarchyWithSingleLevelContextHierarchies() {
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(TestClass3WithSingleLevelContextHierarchy.class);
		assertEquals(3, hierarchyAttributes.size());

		List<ContextConfigurationAttributes> configAttributesListClassLevel1 = hierarchyAttributes.get(0);
		debugConfigAttributes(configAttributesListClassLevel1);
		assertEquals(1, configAttributesListClassLevel1.size());
		assertThat(configAttributesListClassLevel1.get(0).getLocations()[0], equalTo("one.xml"));

		List<ContextConfigurationAttributes> configAttributesListClassLevel2 = hierarchyAttributes.get(1);
		debugConfigAttributes(configAttributesListClassLevel2);
		assertEquals(1, configAttributesListClassLevel2.size());
		assertArrayEquals(new String[] { "two-A.xml", "two-B.xml" },
			configAttributesListClassLevel2.get(0).getLocations());

		List<ContextConfigurationAttributes> configAttributesListClassLevel3 = hierarchyAttributes.get(2);
		debugConfigAttributes(configAttributesListClassLevel3);
		assertEquals(1, configAttributesListClassLevel3.size());
		assertThat(configAttributesListClassLevel3.get(0).getLocations()[0], equalTo("three.xml"));
	}

	@Test
	public void resolveContextHierarchyAttributesForTestClassHierarchyWithSingleLevelContextHierarchiesAndMetaAnnotations() {
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(TestClass3WithSingleLevelContextHierarchyFromMetaAnnotation.class);
		assertEquals(3, hierarchyAttributes.size());

		List<ContextConfigurationAttributes> configAttributesListClassLevel1 = hierarchyAttributes.get(0);
		debugConfigAttributes(configAttributesListClassLevel1);
		assertEquals(1, configAttributesListClassLevel1.size());
		assertThat(configAttributesListClassLevel1.get(0).getLocations()[0], equalTo("A.xml"));
		assertAttributes(configAttributesListClassLevel1.get(0),
			TestClass1WithSingleLevelContextHierarchyFromMetaAnnotation.class, new String[] { "A.xml" },
			EMPTY_CLASS_ARRAY, ContextLoader.class, true);

		List<ContextConfigurationAttributes> configAttributesListClassLevel2 = hierarchyAttributes.get(1);
		debugConfigAttributes(configAttributesListClassLevel2);
		assertEquals(1, configAttributesListClassLevel2.size());
		assertArrayEquals(new String[] { "B-one.xml", "B-two.xml" },
			configAttributesListClassLevel2.get(0).getLocations());
		assertAttributes(configAttributesListClassLevel2.get(0),
			TestClass2WithSingleLevelContextHierarchyFromMetaAnnotation.class,
			new String[] { "B-one.xml",
			"B-two.xml" }, EMPTY_CLASS_ARRAY, ContextLoader.class, true);

		List<ContextConfigurationAttributes> configAttributesListClassLevel3 = hierarchyAttributes.get(2);
		debugConfigAttributes(configAttributesListClassLevel3);
		assertEquals(1, configAttributesListClassLevel3.size());
		assertThat(configAttributesListClassLevel3.get(0).getLocations()[0], equalTo("C.xml"));
		assertAttributes(configAttributesListClassLevel3.get(0),
			TestClass3WithSingleLevelContextHierarchyFromMetaAnnotation.class, new String[] { "C.xml" },
			EMPTY_CLASS_ARRAY, ContextLoader.class, true);
	}

	private void assertOneTwo(List<List<ContextConfigurationAttributes>> hierarchyAttributes) {
		assertEquals(2, hierarchyAttributes.size());

		List<ContextConfigurationAttributes> configAttributesListClassLevel1 = hierarchyAttributes.get(0);
		List<ContextConfigurationAttributes> configAttributesListClassLevel2 = hierarchyAttributes.get(1);
		debugConfigAttributes(configAttributesListClassLevel1);
		debugConfigAttributes(configAttributesListClassLevel2);

		assertEquals(1, configAttributesListClassLevel1.size());
		assertThat(configAttributesListClassLevel1.get(0).getLocations()[0], equalTo("one.xml"));

		assertEquals(1, configAttributesListClassLevel2.size());
		assertThat(configAttributesListClassLevel2.get(0).getLocations()[0], equalTo("two.xml"));
	}

	@Test
	public void resolveContextHierarchyAttributesForTestClassHierarchyWithBareContextConfigurationInSuperclass() {
		assertOneTwo(resolveContextHierarchyAttributes(TestClass2WithBareContextConfigurationInSuperclass.class));
	}

	@Test
	public void resolveContextHierarchyAttributesForTestClassHierarchyWithBareContextConfigurationInSubclass() {
		assertOneTwo(resolveContextHierarchyAttributes(TestClass2WithBareContextConfigurationInSubclass.class));
	}

	@Test
	public void resolveContextHierarchyAttributesForTestClassHierarchyWithBareMetaContextConfigWithOverridesInSuperclass() {
		assertOneTwo(resolveContextHierarchyAttributes(TestClass2WithBareMetaContextConfigWithOverridesInSuperclass.class));
	}

	@Test
	public void resolveContextHierarchyAttributesForTestClassHierarchyWithBareMetaContextConfigWithOverridesInSubclass() {
		assertOneTwo(resolveContextHierarchyAttributes(TestClass2WithBareMetaContextConfigWithOverridesInSubclass.class));
	}

	@Test
	public void resolveContextHierarchyAttributesForTestClassHierarchyWithMultiLevelContextHierarchies() {
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(TestClass3WithMultiLevelContextHierarchy.class);
		assertEquals(3, hierarchyAttributes.size());

		List<ContextConfigurationAttributes> configAttributesListClassLevel1 = hierarchyAttributes.get(0);
		debugConfigAttributes(configAttributesListClassLevel1);
		assertEquals(2, configAttributesListClassLevel1.size());
		assertThat(configAttributesListClassLevel1.get(0).getLocations()[0], equalTo("1-A.xml"));
		assertThat(configAttributesListClassLevel1.get(1).getLocations()[0], equalTo("1-B.xml"));

		List<ContextConfigurationAttributes> configAttributesListClassLevel2 = hierarchyAttributes.get(1);
		debugConfigAttributes(configAttributesListClassLevel2);
		assertEquals(2, configAttributesListClassLevel2.size());
		assertThat(configAttributesListClassLevel2.get(0).getLocations()[0], equalTo("2-A.xml"));
		assertThat(configAttributesListClassLevel2.get(1).getLocations()[0], equalTo("2-B.xml"));

		List<ContextConfigurationAttributes> configAttributesListClassLevel3 = hierarchyAttributes.get(2);
		debugConfigAttributes(configAttributesListClassLevel3);
		assertEquals(3, configAttributesListClassLevel3.size());
		assertThat(configAttributesListClassLevel3.get(0).getLocations()[0], equalTo("3-A.xml"));
		assertThat(configAttributesListClassLevel3.get(1).getLocations()[0], equalTo("3-B.xml"));
		assertThat(configAttributesListClassLevel3.get(2).getLocations()[0], equalTo("3-C.xml"));
	}

	@Test
	public void buildContextHierarchyMapForTestClassHierarchyWithMultiLevelContextHierarchies() {
		Map<String, List<ContextConfigurationAttributes>> map = buildContextHierarchyMap(TestClass3WithMultiLevelContextHierarchy.class);

		assertThat(map.size(), is(3));
		assertThat(map.keySet(), hasItems("alpha", "beta", "gamma"));

		List<ContextConfigurationAttributes> alphaConfig = map.get("alpha");
		assertThat(alphaConfig.size(), is(3));
		assertThat(alphaConfig.get(0).getLocations()[0], is("1-A.xml"));
		assertThat(alphaConfig.get(1).getLocations()[0], is("2-A.xml"));
		assertThat(alphaConfig.get(2).getLocations()[0], is("3-A.xml"));

		List<ContextConfigurationAttributes> betaConfig = map.get("beta");
		assertThat(betaConfig.size(), is(3));
		assertThat(betaConfig.get(0).getLocations()[0], is("1-B.xml"));
		assertThat(betaConfig.get(1).getLocations()[0], is("2-B.xml"));
		assertThat(betaConfig.get(2).getLocations()[0], is("3-B.xml"));

		List<ContextConfigurationAttributes> gammaConfig = map.get("gamma");
		assertThat(gammaConfig.size(), is(1));
		assertThat(gammaConfig.get(0).getLocations()[0], is("3-C.xml"));
	}

	@Test
	public void buildContextHierarchyMapForTestClassHierarchyWithMultiLevelContextHierarchiesAndUnnamedConfig() {
		Map<String, List<ContextConfigurationAttributes>> map = buildContextHierarchyMap(TestClass3WithMultiLevelContextHierarchyAndUnnamedConfig.class);

		String level1 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 1;
		String level2 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 2;
		String level3 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 3;
		String level4 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 4;
		String level5 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 5;
		String level6 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 6;
		String level7 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 7;

		assertThat(map.size(), is(7));
		assertThat(map.keySet(), hasItems(level1, level2, level3, level4, level5, level6, level7));

		List<ContextConfigurationAttributes> level1Config = map.get(level1);
		assertThat(level1Config.size(), is(1));
		assertThat(level1Config.get(0).getLocations()[0], is("1-A.xml"));

		List<ContextConfigurationAttributes> level2Config = map.get(level2);
		assertThat(level2Config.size(), is(1));
		assertThat(level2Config.get(0).getLocations()[0], is("1-B.xml"));

		List<ContextConfigurationAttributes> level3Config = map.get(level3);
		assertThat(level3Config.size(), is(1));
		assertThat(level3Config.get(0).getLocations()[0], is("2-A.xml"));

		// ...

		List<ContextConfigurationAttributes> level7Config = map.get(level7);
		assertThat(level7Config.size(), is(1));
		assertThat(level7Config.get(0).getLocations()[0], is("3-C.xml"));
	}

	@Test
	public void buildContextHierarchyMapForTestClassHierarchyWithMultiLevelContextHierarchiesAndPartiallyNamedConfig() {
		Map<String, List<ContextConfigurationAttributes>> map = buildContextHierarchyMap(TestClass2WithMultiLevelContextHierarchyAndPartiallyNamedConfig.class);

		String level1 = "parent";
		String level2 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 2;
		String level3 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 3;

		assertThat(map.size(), is(3));
		assertThat(map.keySet(), hasItems(level1, level2, level3));
		Iterator<String> levels = map.keySet().iterator();
		assertThat(levels.next(), is(level1));
		assertThat(levels.next(), is(level2));
		assertThat(levels.next(), is(level3));

		List<ContextConfigurationAttributes> level1Config = map.get(level1);
		assertThat(level1Config.size(), is(2));
		assertThat(level1Config.get(0).getLocations()[0], is("1-A.xml"));
		assertThat(level1Config.get(1).getLocations()[0], is("2-A.xml"));

		List<ContextConfigurationAttributes> level2Config = map.get(level2);
		assertThat(level2Config.size(), is(1));
		assertThat(level2Config.get(0).getLocations()[0], is("1-B.xml"));

		List<ContextConfigurationAttributes> level3Config = map.get(level3);
		assertThat(level3Config.size(), is(1));
		assertThat(level3Config.get(0).getLocations()[0], is("2-C.xml"));
	}

	private void assertContextConfigEntriesAreNotUnique(Class<?> testClass) {
		try {
			buildContextHierarchyMap(testClass);
			fail("Should throw an IllegalStateException");
		}
		catch (IllegalStateException e) {
			String msg = String.format(
				"The @ContextConfiguration elements configured via @ContextHierarchy in test class [%s] and its superclasses must define unique contexts per hierarchy level.",
				testClass.getName());
			assertEquals(msg, e.getMessage());
		}
	}

	@Test
	public void buildContextHierarchyMapForSingleTestClassWithMultiLevelContextHierarchyWithEmptyContextConfig() {
		assertContextConfigEntriesAreNotUnique(SingleTestClassWithMultiLevelContextHierarchyWithEmptyContextConfig.class);
	}

	@Test
	public void buildContextHierarchyMapForSingleTestClassWithMultiLevelContextHierarchyWithDuplicatedContextConfig() {
		assertContextConfigEntriesAreNotUnique(SingleTestClassWithMultiLevelContextHierarchyWithDuplicatedContextConfig.class);
	}

	/**
	 * Used to reproduce bug reported in https://jira.spring.io/browse/SPR-10997
	 */
	@Test
	public void buildContextHierarchyMapForTestClassHierarchyWithMultiLevelContextHierarchiesAndOverriddenInitializers() {
		Map<String, List<ContextConfigurationAttributes>> map = buildContextHierarchyMap(TestClass2WithMultiLevelContextHierarchyWithOverriddenInitializers.class);

		assertThat(map.size(), is(2));
		assertThat(map.keySet(), hasItems("alpha", "beta"));

		List<ContextConfigurationAttributes> alphaConfig = map.get("alpha");
		assertThat(alphaConfig.size(), is(2));
		assertThat(alphaConfig.get(0).getLocations().length, is(1));
		assertThat(alphaConfig.get(0).getLocations()[0], is("1-A.xml"));
		assertThat(alphaConfig.get(0).getInitializers().length, is(0));
		assertThat(alphaConfig.get(1).getLocations().length, is(0));
		assertThat(alphaConfig.get(1).getInitializers().length, is(1));
		assertEquals(DummyApplicationContextInitializer.class, alphaConfig.get(1).getInitializers()[0]);

		List<ContextConfigurationAttributes> betaConfig = map.get("beta");
		assertThat(betaConfig.size(), is(2));
		assertThat(betaConfig.get(0).getLocations().length, is(1));
		assertThat(betaConfig.get(0).getLocations()[0], is("1-B.xml"));
		assertThat(betaConfig.get(0).getInitializers().length, is(0));
		assertThat(betaConfig.get(1).getLocations().length, is(0));
		assertThat(betaConfig.get(1).getInitializers().length, is(1));
		assertEquals(DummyApplicationContextInitializer.class, betaConfig.get(1).getInitializers()[0]);
	}


	// -------------------------------------------------------------------------

	@ContextConfiguration("foo.xml")
	@ContextHierarchy(@ContextConfiguration("bar.xml"))
	private static class SingleTestClassWithContextConfigurationAndContextHierarchy {
	}

    @Target({ElementType.TYPE, ElementType.METHOD})
	@ContextConfiguration("foo.xml")
	@ContextHierarchy(@ContextConfiguration("bar.xml"))
	@Retention(RetentionPolicy.RUNTIME)
	@interface ContextConfigurationAndContextHierarchyOnSingleMeta {
	}

	@ContextConfigurationAndContextHierarchyOnSingleMeta
	private static class SingleTestClassWithContextConfigurationAndContextHierarchyOnSingleMetaAnnotation {
	}

	@ContextHierarchy(@ContextConfiguration("A.xml"))
	private static class SingleTestClassWithSingleLevelContextHierarchy {
	}

	@ContextHierarchy({//
	//
		@ContextConfiguration("A.xml"),//
		@ContextConfiguration("B.xml"),//
		@ContextConfiguration("C.xml") //
	})
	private static class SingleTestClassWithTripleLevelContextHierarchy {
	}

	@ContextHierarchy(@ContextConfiguration("one.xml"))
	private static class TestClass1WithSingleLevelContextHierarchy {
	}

	@ContextHierarchy(@ContextConfiguration({ "two-A.xml", "two-B.xml" }))
	private static class TestClass2WithSingleLevelContextHierarchy extends TestClass1WithSingleLevelContextHierarchy {
	}

	@ContextHierarchy(@ContextConfiguration("three.xml"))
	private static class TestClass3WithSingleLevelContextHierarchy extends TestClass2WithSingleLevelContextHierarchy {
	}

	@ContextConfiguration("one.xml")
	private static class TestClass1WithBareContextConfigurationInSuperclass {
	}

	@ContextHierarchy(@ContextConfiguration("two.xml"))
	private static class TestClass2WithBareContextConfigurationInSuperclass extends
			TestClass1WithBareContextConfigurationInSuperclass {
	}

	@ContextHierarchy(@ContextConfiguration("one.xml"))
	private static class TestClass1WithBareContextConfigurationInSubclass {
	}

	@ContextConfiguration("two.xml")
	private static class TestClass2WithBareContextConfigurationInSubclass extends
			TestClass1WithBareContextConfigurationInSubclass {
	}

	@ContextHierarchy({//
	//
		@ContextConfiguration(locations = "1-A.xml", name = "alpha"),//
		@ContextConfiguration(locations = "1-B.xml", name = "beta") //
	})
	private static class TestClass1WithMultiLevelContextHierarchy {
	}

	@ContextHierarchy({//
	//
		@ContextConfiguration(locations = "2-A.xml", name = "alpha"),//
		@ContextConfiguration(locations = "2-B.xml", name = "beta") //
	})
	private static class TestClass2WithMultiLevelContextHierarchy extends TestClass1WithMultiLevelContextHierarchy {
	}

	@ContextHierarchy({//
	//
		@ContextConfiguration(locations = "3-A.xml", name = "alpha"),//
		@ContextConfiguration(locations = "3-B.xml", name = "beta"),//
		@ContextConfiguration(locations = "3-C.xml", name = "gamma") //
	})
	private static class TestClass3WithMultiLevelContextHierarchy extends TestClass2WithMultiLevelContextHierarchy {
	}

	@ContextHierarchy({//
	//
		@ContextConfiguration(locations = "1-A.xml"),//
		@ContextConfiguration(locations = "1-B.xml") //
	})
	private static class TestClass1WithMultiLevelContextHierarchyAndUnnamedConfig {
	}

	@ContextHierarchy({//
	//
		@ContextConfiguration(locations = "2-A.xml"),//
		@ContextConfiguration(locations = "2-B.xml") //
	})
	private static class TestClass2WithMultiLevelContextHierarchyAndUnnamedConfig extends
			TestClass1WithMultiLevelContextHierarchyAndUnnamedConfig {
	}

	@ContextHierarchy({//
	//
		@ContextConfiguration(locations = "3-A.xml"),//
		@ContextConfiguration(locations = "3-B.xml"),//
		@ContextConfiguration(locations = "3-C.xml") //
	})
	private static class TestClass3WithMultiLevelContextHierarchyAndUnnamedConfig extends
			TestClass2WithMultiLevelContextHierarchyAndUnnamedConfig {
	}

	@ContextHierarchy({//
	//
		@ContextConfiguration(locations = "1-A.xml", name = "parent"),//
		@ContextConfiguration(locations = "1-B.xml") //
	})
	private static class TestClass1WithMultiLevelContextHierarchyAndPartiallyNamedConfig {
	}

	@ContextHierarchy({//
	//
		@ContextConfiguration(locations = "2-A.xml", name = "parent"),//
		@ContextConfiguration(locations = "2-C.xml") //
	})
	private static class TestClass2WithMultiLevelContextHierarchyAndPartiallyNamedConfig extends
			TestClass1WithMultiLevelContextHierarchyAndPartiallyNamedConfig {
	}

	@ContextHierarchy({
		//
		@ContextConfiguration,//
		@ContextConfiguration //
	})
	private static class SingleTestClassWithMultiLevelContextHierarchyWithEmptyContextConfig {
	}

	@ContextHierarchy({
		//
		@ContextConfiguration("foo.xml"),//
		@ContextConfiguration(classes = BarConfig.class),// duplicate!
		@ContextConfiguration("baz.xml"),//
		@ContextConfiguration(classes = BarConfig.class),// duplicate!
		@ContextConfiguration(loader = AnnotationConfigContextLoader.class) //
	})
	private static class SingleTestClassWithMultiLevelContextHierarchyWithDuplicatedContextConfig {
	}

	/**
	 * Used to reproduce bug reported in https://jira.spring.io/browse/SPR-10997
	 */
	@ContextHierarchy({//
	//
		@ContextConfiguration(name = "alpha", locations = "1-A.xml"),//
		@ContextConfiguration(name = "beta", locations = "1-B.xml") //
	})
	private static class TestClass1WithMultiLevelContextHierarchyWithUniqueContextConfig {
	}

	/**
	 * Used to reproduce bug reported in https://jira.spring.io/browse/SPR-10997
	 */
	@ContextHierarchy({//
	//
		@ContextConfiguration(name = "alpha", initializers = DummyApplicationContextInitializer.class),//
		@ContextConfiguration(name = "beta", initializers = DummyApplicationContextInitializer.class) //
	})
	private static class TestClass2WithMultiLevelContextHierarchyWithOverriddenInitializers extends
			TestClass1WithMultiLevelContextHierarchyWithUniqueContextConfig {
	}

	/**
	 * Used to reproduce bug reported in https://jira.spring.io/browse/SPR-10997
	 */
	private static class DummyApplicationContextInitializer implements
			ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			/* no-op */
		}
	}

	// -------------------------------------------------------------------------

	@ContextHierarchy(@ContextConfiguration("A.xml"))
	@Retention(RetentionPolicy.RUNTIME)
	@interface ContextHierarchyA {
	}

    @Target({ElementType.TYPE, ElementType.METHOD})
	@ContextHierarchy(@ContextConfiguration({ "B-one.xml", "B-two.xml" }))
	@Retention(RetentionPolicy.RUNTIME)
	@interface ContextHierarchyB {
	}

    @Target({ElementType.TYPE, ElementType.METHOD})
	@ContextHierarchy(@ContextConfiguration("C.xml"))
	@Retention(RetentionPolicy.RUNTIME)
	@interface ContextHierarchyC {
	}

	@ContextHierarchyA
	private static class SingleTestClassWithSingleLevelContextHierarchyFromMetaAnnotation {
	}

	@ContextHierarchyA
	private static class TestClass1WithSingleLevelContextHierarchyFromMetaAnnotation {
	}

	@ContextHierarchyB
	private static class TestClass2WithSingleLevelContextHierarchyFromMetaAnnotation extends
			TestClass1WithSingleLevelContextHierarchyFromMetaAnnotation {
	}

	@ContextHierarchyC
	private static class TestClass3WithSingleLevelContextHierarchyFromMetaAnnotation extends
			TestClass2WithSingleLevelContextHierarchyFromMetaAnnotation {
	}

	// -------------------------------------------------------------------------

    @Target({ElementType.TYPE, ElementType.METHOD})
	@ContextConfiguration
	@Retention(RetentionPolicy.RUNTIME)
	@interface ContextConfigWithOverrides {

		String[] locations() default "A.xml";
	}

	@ContextConfigWithOverrides(locations = "one.xml")
	private static class TestClass1WithBareMetaContextConfigWithOverridesInSuperclass {
	}

	@ContextHierarchy(@ContextConfiguration(locations = "two.xml"))
	private static class TestClass2WithBareMetaContextConfigWithOverridesInSuperclass extends
			TestClass1WithBareMetaContextConfigWithOverridesInSuperclass {
	}

	@ContextHierarchy(@ContextConfiguration(locations = "one.xml"))
	private static class TestClass1WithBareMetaContextConfigWithOverridesInSubclass {
	}

	@ContextConfigWithOverrides(locations = "two.xml")
	private static class TestClass2WithBareMetaContextConfigWithOverridesInSubclass extends
			TestClass1WithBareMetaContextConfigWithOverridesInSubclass {
	}

}
