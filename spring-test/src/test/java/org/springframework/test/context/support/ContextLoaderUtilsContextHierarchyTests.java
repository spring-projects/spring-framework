/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.context.support;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.ContextLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.test.context.support.ContextLoaderUtils.GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX;
import static org.springframework.test.context.support.ContextLoaderUtils.buildContextHierarchyMap;
import static org.springframework.test.context.support.ContextLoaderUtils.resolveContextHierarchyAttributes;

/**
 * Unit tests for {@link ContextLoaderUtils} involving context hierarchies.
 *
 * @author Sam Brannen
 * @since 3.2.2
 */
class ContextLoaderUtilsContextHierarchyTests extends AbstractContextConfigurationUtilsTests {

	private void debugConfigAttributes(List<ContextConfigurationAttributes> configAttributesList) {
		// for (ContextConfigurationAttributes configAttributes : configAttributesList) {
		// System.err.println(configAttributes);
		// }
	}

	@Test
	void resolveContextHierarchyAttributesForSingleTestClassWithContextConfigurationAndContextHierarchy() {
		assertThatIllegalStateException().isThrownBy(() ->
				resolveContextHierarchyAttributes(SingleTestClassWithContextConfigurationAndContextHierarchy.class));
	}

	@Test
	void resolveContextHierarchyAttributesForSingleTestClassWithContextConfigurationAndContextHierarchyOnSingleMetaAnnotation() {
		assertThatIllegalStateException().isThrownBy(() ->
				resolveContextHierarchyAttributes(SingleTestClassWithContextConfigurationAndContextHierarchyOnSingleMetaAnnotation.class));
	}

	@Test
	void resolveContextHierarchyAttributesForSingleTestClassWithImplicitSingleLevelContextHierarchy() {
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(BareAnnotations.class);
		assertThat(hierarchyAttributes.size()).isEqualTo(1);
		List<ContextConfigurationAttributes> configAttributesList = hierarchyAttributes.get(0);
		assertThat(configAttributesList.size()).isEqualTo(1);
		debugConfigAttributes(configAttributesList);
	}

	@Test
	void resolveContextHierarchyAttributesForSingleTestClassWithSingleLevelContextHierarchy() {
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(SingleTestClassWithSingleLevelContextHierarchy.class);
		assertThat(hierarchyAttributes.size()).isEqualTo(1);
		List<ContextConfigurationAttributes> configAttributesList = hierarchyAttributes.get(0);
		assertThat(configAttributesList.size()).isEqualTo(1);
		debugConfigAttributes(configAttributesList);
	}

	@Test
	void resolveContextHierarchyAttributesForSingleTestClassWithSingleLevelContextHierarchyFromMetaAnnotation() {
		Class<SingleTestClassWithSingleLevelContextHierarchyFromMetaAnnotation> testClass = SingleTestClassWithSingleLevelContextHierarchyFromMetaAnnotation.class;
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(testClass);
		assertThat(hierarchyAttributes.size()).isEqualTo(1);

		List<ContextConfigurationAttributes> configAttributesList = hierarchyAttributes.get(0);
		assertThat(configAttributesList).isNotNull();
		assertThat(configAttributesList.size()).isEqualTo(1);
		debugConfigAttributes(configAttributesList);
		assertAttributes(configAttributesList.get(0), testClass, new String[] { "A.xml" }, EMPTY_CLASS_ARRAY,
			ContextLoader.class, true);
	}

	@Test
	void resolveContextHierarchyAttributesForSingleTestClassWithTripleLevelContextHierarchy() {
		Class<SingleTestClassWithTripleLevelContextHierarchy> testClass = SingleTestClassWithTripleLevelContextHierarchy.class;
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(testClass);
		assertThat(hierarchyAttributes.size()).isEqualTo(1);

		List<ContextConfigurationAttributes> configAttributesList = hierarchyAttributes.get(0);
		assertThat(configAttributesList).isNotNull();
		assertThat(configAttributesList.size()).isEqualTo(3);
		debugConfigAttributes(configAttributesList);
		assertAttributes(configAttributesList.get(0), testClass, new String[] { "A.xml" }, EMPTY_CLASS_ARRAY,
			ContextLoader.class, true);
		assertAttributes(configAttributesList.get(1), testClass, new String[] { "B.xml" }, EMPTY_CLASS_ARRAY,
			ContextLoader.class, true);
		assertAttributes(configAttributesList.get(2), testClass, new String[] { "C.xml" }, EMPTY_CLASS_ARRAY,
			ContextLoader.class, true);
	}

	@Test
	void resolveContextHierarchyAttributesForTestClassHierarchyWithSingleLevelContextHierarchies() {
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(TestClass3WithSingleLevelContextHierarchy.class);
		assertThat(hierarchyAttributes.size()).isEqualTo(3);

		List<ContextConfigurationAttributes> configAttributesListClassLevel1 = hierarchyAttributes.get(0);
		debugConfigAttributes(configAttributesListClassLevel1);
		assertThat(configAttributesListClassLevel1.size()).isEqualTo(1);
		assertThat(configAttributesListClassLevel1.get(0).getLocations()[0]).isEqualTo("one.xml");

		List<ContextConfigurationAttributes> configAttributesListClassLevel2 = hierarchyAttributes.get(1);
		debugConfigAttributes(configAttributesListClassLevel2);
		assertThat(configAttributesListClassLevel2.size()).isEqualTo(1);
		assertThat(configAttributesListClassLevel2.get(0).getLocations()).isEqualTo(new String[] { "two-A.xml", "two-B.xml" });

		List<ContextConfigurationAttributes> configAttributesListClassLevel3 = hierarchyAttributes.get(2);
		debugConfigAttributes(configAttributesListClassLevel3);
		assertThat(configAttributesListClassLevel3.size()).isEqualTo(1);
		assertThat(configAttributesListClassLevel3.get(0).getLocations()[0]).isEqualTo("three.xml");
	}

	@Test
	void resolveContextHierarchyAttributesForTestClassHierarchyWithSingleLevelContextHierarchiesAndMetaAnnotations() {
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(TestClass3WithSingleLevelContextHierarchyFromMetaAnnotation.class);
		assertThat(hierarchyAttributes.size()).isEqualTo(3);

		List<ContextConfigurationAttributes> configAttributesListClassLevel1 = hierarchyAttributes.get(0);
		debugConfigAttributes(configAttributesListClassLevel1);
		assertThat(configAttributesListClassLevel1.size()).isEqualTo(1);
		assertThat(configAttributesListClassLevel1.get(0).getLocations()[0]).isEqualTo("A.xml");
		assertAttributes(configAttributesListClassLevel1.get(0),
			TestClass1WithSingleLevelContextHierarchyFromMetaAnnotation.class, new String[] { "A.xml" },
			EMPTY_CLASS_ARRAY, ContextLoader.class, true);

		List<ContextConfigurationAttributes> configAttributesListClassLevel2 = hierarchyAttributes.get(1);
		debugConfigAttributes(configAttributesListClassLevel2);
		assertThat(configAttributesListClassLevel2.size()).isEqualTo(1);
		assertThat(configAttributesListClassLevel2.get(0).getLocations()).isEqualTo(new String[] { "B-one.xml", "B-two.xml" });
		assertAttributes(configAttributesListClassLevel2.get(0),
			TestClass2WithSingleLevelContextHierarchyFromMetaAnnotation.class,
			new String[] { "B-one.xml",
			"B-two.xml" }, EMPTY_CLASS_ARRAY, ContextLoader.class, true);

		List<ContextConfigurationAttributes> configAttributesListClassLevel3 = hierarchyAttributes.get(2);
		debugConfigAttributes(configAttributesListClassLevel3);
		assertThat(configAttributesListClassLevel3.size()).isEqualTo(1);
		assertThat(configAttributesListClassLevel3.get(0).getLocations()[0]).isEqualTo("C.xml");
		assertAttributes(configAttributesListClassLevel3.get(0),
			TestClass3WithSingleLevelContextHierarchyFromMetaAnnotation.class, new String[] { "C.xml" },
			EMPTY_CLASS_ARRAY, ContextLoader.class, true);
	}

	private void assertOneTwo(List<List<ContextConfigurationAttributes>> hierarchyAttributes) {
		assertThat(hierarchyAttributes.size()).isEqualTo(2);

		List<ContextConfigurationAttributes> configAttributesListClassLevel1 = hierarchyAttributes.get(0);
		List<ContextConfigurationAttributes> configAttributesListClassLevel2 = hierarchyAttributes.get(1);
		debugConfigAttributes(configAttributesListClassLevel1);
		debugConfigAttributes(configAttributesListClassLevel2);

		assertThat(configAttributesListClassLevel1.size()).isEqualTo(1);
		assertThat(configAttributesListClassLevel1.get(0).getLocations()[0]).isEqualTo("one.xml");

		assertThat(configAttributesListClassLevel2.size()).isEqualTo(1);
		assertThat(configAttributesListClassLevel2.get(0).getLocations()[0]).isEqualTo("two.xml");
	}

	@Test
	void resolveContextHierarchyAttributesForTestClassHierarchyWithBareContextConfigurationInSuperclass() {
		assertOneTwo(resolveContextHierarchyAttributes(TestClass2WithBareContextConfigurationInSuperclass.class));
	}

	@Test
	void resolveContextHierarchyAttributesForTestClassHierarchyWithBareContextConfigurationInSubclass() {
		assertOneTwo(resolveContextHierarchyAttributes(TestClass2WithBareContextConfigurationInSubclass.class));
	}

	@Test
	void resolveContextHierarchyAttributesForTestClassHierarchyWithBareMetaContextConfigWithOverridesInSuperclass() {
		assertOneTwo(resolveContextHierarchyAttributes(TestClass2WithBareMetaContextConfigWithOverridesInSuperclass.class));
	}

	@Test
	void resolveContextHierarchyAttributesForTestClassHierarchyWithBareMetaContextConfigWithOverridesInSubclass() {
		assertOneTwo(resolveContextHierarchyAttributes(TestClass2WithBareMetaContextConfigWithOverridesInSubclass.class));
	}

	@Test
	void resolveContextHierarchyAttributesForTestClassHierarchyWithMultiLevelContextHierarchies() {
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(TestClass3WithMultiLevelContextHierarchy.class);
		assertThat(hierarchyAttributes.size()).isEqualTo(3);

		List<ContextConfigurationAttributes> configAttributesListClassLevel1 = hierarchyAttributes.get(0);
		debugConfigAttributes(configAttributesListClassLevel1);
		assertThat(configAttributesListClassLevel1.size()).isEqualTo(2);
		assertThat(configAttributesListClassLevel1.get(0).getLocations()[0]).isEqualTo("1-A.xml");
		assertThat(configAttributesListClassLevel1.get(1).getLocations()[0]).isEqualTo("1-B.xml");

		List<ContextConfigurationAttributes> configAttributesListClassLevel2 = hierarchyAttributes.get(1);
		debugConfigAttributes(configAttributesListClassLevel2);
		assertThat(configAttributesListClassLevel2.size()).isEqualTo(2);
		assertThat(configAttributesListClassLevel2.get(0).getLocations()[0]).isEqualTo("2-A.xml");
		assertThat(configAttributesListClassLevel2.get(1).getLocations()[0]).isEqualTo("2-B.xml");

		List<ContextConfigurationAttributes> configAttributesListClassLevel3 = hierarchyAttributes.get(2);
		debugConfigAttributes(configAttributesListClassLevel3);
		assertThat(configAttributesListClassLevel3.size()).isEqualTo(3);
		assertThat(configAttributesListClassLevel3.get(0).getLocations()[0]).isEqualTo("3-A.xml");
		assertThat(configAttributesListClassLevel3.get(1).getLocations()[0]).isEqualTo("3-B.xml");
		assertThat(configAttributesListClassLevel3.get(2).getLocations()[0]).isEqualTo("3-C.xml");
	}

	@Test
	void buildContextHierarchyMapForTestClassHierarchyWithMultiLevelContextHierarchies() {
		Map<String, List<ContextConfigurationAttributes>> map = buildContextHierarchyMap(TestClass3WithMultiLevelContextHierarchy.class);

		assertThat(map).hasSize(3).containsKeys("alpha", "beta", "gamma");

		List<ContextConfigurationAttributes> alphaConfig = map.get("alpha");
		assertThat(alphaConfig).hasSize(3);
		assertThat(alphaConfig.get(0).getLocations()[0]).isEqualTo("1-A.xml");
		assertThat(alphaConfig.get(1).getLocations()[0]).isEqualTo("2-A.xml");
		assertThat(alphaConfig.get(2).getLocations()[0]).isEqualTo("3-A.xml");

		List<ContextConfigurationAttributes> betaConfig = map.get("beta");
		assertThat(betaConfig).hasSize(3);
		assertThat(betaConfig.get(0).getLocations()[0]).isEqualTo("1-B.xml");
		assertThat(betaConfig.get(1).getLocations()[0]).isEqualTo("2-B.xml");
		assertThat(betaConfig.get(2).getLocations()[0]).isEqualTo("3-B.xml");

		List<ContextConfigurationAttributes> gammaConfig = map.get("gamma");
		assertThat(gammaConfig).hasSize(1);
		assertThat(gammaConfig.get(0).getLocations()[0]).isEqualTo("3-C.xml");
	}

	@Test
	void buildContextHierarchyMapForTestClassHierarchyWithMultiLevelContextHierarchiesAndUnnamedConfig() {
		Map<String, List<ContextConfigurationAttributes>> map = buildContextHierarchyMap(TestClass3WithMultiLevelContextHierarchyAndUnnamedConfig.class);

		String level1 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 1;
		String level2 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 2;
		String level3 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 3;
		String level4 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 4;
		String level5 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 5;
		String level6 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 6;
		String level7 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 7;

		assertThat(map).hasSize(7).containsKeys(level1, level2, level3, level4, level5, level6, level7);

		List<ContextConfigurationAttributes> level1Config = map.get(level1);
		assertThat(level1Config).hasSize(1);
		assertThat(level1Config.get(0).getLocations()[0]).isEqualTo("1-A.xml");

		List<ContextConfigurationAttributes> level2Config = map.get(level2);
		assertThat(level2Config).hasSize(1);
		assertThat(level2Config.get(0).getLocations()[0]).isEqualTo("1-B.xml");

		List<ContextConfigurationAttributes> level3Config = map.get(level3);
		assertThat(level3Config).hasSize(1);
		assertThat(level3Config.get(0).getLocations()[0]).isEqualTo("2-A.xml");

		// ...

		List<ContextConfigurationAttributes> level7Config = map.get(level7);
		assertThat(level7Config).hasSize(1);
		assertThat(level7Config.get(0).getLocations()[0]).isEqualTo("3-C.xml");
	}

	@Test
	void buildContextHierarchyMapForTestClassHierarchyWithMultiLevelContextHierarchiesAndPartiallyNamedConfig() {
		Map<String, List<ContextConfigurationAttributes>> map = buildContextHierarchyMap(TestClass2WithMultiLevelContextHierarchyAndPartiallyNamedConfig.class);

		String level1 = "parent";
		String level2 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 2;
		String level3 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 3;

		assertThat(map).hasSize(3).containsKeys(level1, level2, level3);
		Iterator<String> levels = map.keySet().iterator();
		assertThat(levels.next()).isEqualTo(level1);
		assertThat(levels.next()).isEqualTo(level2);
		assertThat(levels.next()).isEqualTo(level3);

		List<ContextConfigurationAttributes> level1Config = map.get(level1);
		assertThat(level1Config).hasSize(2);
		assertThat(level1Config.get(0).getLocations()[0]).isEqualTo("1-A.xml");
		assertThat(level1Config.get(1).getLocations()[0]).isEqualTo("2-A.xml");

		List<ContextConfigurationAttributes> level2Config = map.get(level2);
		assertThat(level2Config).hasSize(1);
		assertThat(level2Config.get(0).getLocations()[0]).isEqualTo("1-B.xml");

		List<ContextConfigurationAttributes> level3Config = map.get(level3);
		assertThat(level3Config).hasSize(1);
		assertThat(level3Config.get(0).getLocations()[0]).isEqualTo("2-C.xml");
	}

	private void assertContextConfigEntriesAreNotUnique(Class<?> testClass) {
		assertThatIllegalStateException().isThrownBy(() ->
				buildContextHierarchyMap(testClass))
			.withMessage(String.format(
				"The @ContextConfiguration elements configured via @ContextHierarchy in test class [%s] and its superclasses must define unique contexts per hierarchy level.", testClass.getName()));
	}

	@Test
	void buildContextHierarchyMapForSingleTestClassWithMultiLevelContextHierarchyWithEmptyContextConfig() {
		assertContextConfigEntriesAreNotUnique(SingleTestClassWithMultiLevelContextHierarchyWithEmptyContextConfig.class);
	}

	@Test
	void buildContextHierarchyMapForSingleTestClassWithMultiLevelContextHierarchyWithDuplicatedContextConfig() {
		assertContextConfigEntriesAreNotUnique(SingleTestClassWithMultiLevelContextHierarchyWithDuplicatedContextConfig.class);
	}

	/**
	 * Used to reproduce bug reported in https://jira.spring.io/browse/SPR-10997
	 */
	@Test
	void buildContextHierarchyMapForTestClassHierarchyWithMultiLevelContextHierarchiesAndOverriddenInitializers() {
		Map<String, List<ContextConfigurationAttributes>> map = buildContextHierarchyMap(TestClass2WithMultiLevelContextHierarchyWithOverriddenInitializers.class);

		assertThat(map).hasSize(2).containsKeys("alpha", "beta");

		List<ContextConfigurationAttributes> alphaConfig = map.get("alpha");
		assertThat(alphaConfig).hasSize(2);
		assertThat(alphaConfig.get(0).getLocations().length).isEqualTo(1);
		assertThat(alphaConfig.get(0).getLocations()[0]).isEqualTo("1-A.xml");
		assertThat(alphaConfig.get(0).getInitializers().length).isEqualTo(0);
		assertThat(alphaConfig.get(1).getLocations().length).isEqualTo(0);
		assertThat(alphaConfig.get(1).getInitializers().length).isEqualTo(1);
		assertThat(alphaConfig.get(1).getInitializers()[0]).isEqualTo(DummyApplicationContextInitializer.class);

		List<ContextConfigurationAttributes> betaConfig = map.get("beta");
		assertThat(betaConfig).hasSize(2);
		assertThat(betaConfig.get(0).getLocations().length).isEqualTo(1);
		assertThat(betaConfig.get(0).getLocations()[0]).isEqualTo("1-B.xml");
		assertThat(betaConfig.get(0).getInitializers().length).isEqualTo(0);
		assertThat(betaConfig.get(1).getLocations().length).isEqualTo(0);
		assertThat(betaConfig.get(1).getInitializers().length).isEqualTo(1);
		assertThat(betaConfig.get(1).getInitializers()[0]).isEqualTo(DummyApplicationContextInitializer.class);
	}


	// -------------------------------------------------------------------------

	@ContextConfiguration("foo.xml")
	@ContextHierarchy(@ContextConfiguration("bar.xml"))
	private static class SingleTestClassWithContextConfigurationAndContextHierarchy {
	}

	@ContextConfiguration("foo.xml")
	@ContextHierarchy(@ContextConfiguration("bar.xml"))
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface ContextConfigurationAndContextHierarchyOnSingleMeta {
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
	private static @interface ContextHierarchyA {
	}

	@ContextHierarchy(@ContextConfiguration({ "B-one.xml", "B-two.xml" }))
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface ContextHierarchyB {
	}

	@ContextHierarchy(@ContextConfiguration("C.xml"))
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface ContextHierarchyC {
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

	@ContextConfiguration
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface ContextConfigWithOverrides {

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
