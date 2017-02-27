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

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.ContextLoader;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.springframework.test.context.support.ContextLoaderUtils.GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX;
import static org.springframework.test.context.support.ContextLoaderUtils.buildContextHierarchyMap;
import static org.springframework.test.context.support.ContextLoaderUtils.resolveContextHierarchyAttributes;

/**
 * Unit tests for {@link ContextLoaderUtils} involving context hierarchies parsed from method-level annotations.
 *
 * @author Sergei Ustimenko
 * @since 5.0
 */
public class ContextLoaderUtilsContextHierarchyOnMethodTests extends AbstractContextConfigurationUtilsTests {

	@Test(expected = IllegalStateException.class)
	public void resolveContextHierarchyAttributesForSingleTestClassWithContextConfigurationAndContextHierarchy()
			throws Exception {
		resolveContextHierarchyAttributes(
				SingleTestClassWithContextConfigurationAndContextHierarchy.class.getDeclaredMethod("something"));
	}

	@Test(expected = IllegalStateException.class)
	public void
	resolveContextHierarchyAttributesForSingleTestClassWithContextConfigurationAndContextHierarchyOnSingleMetaAnnotation()
			throws Exception {
		resolveContextHierarchyAttributes(
				SingleTestClassWithContextConfigurationAndContextHierarchyOnSingleMetaAnnotation.class.getDeclaredMethod("something"));
	}

	@Test
	public void resolveContextHierarchyAttributesForSingleTestClassWithImplicitSingleLevelContextHierarchy()
			throws Exception {
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(
				BareMethodAnnotations.class.getDeclaredMethod("something"));
		assertEquals(1, hierarchyAttributes.size());
		List<ContextConfigurationAttributes> configAttributesList = hierarchyAttributes.get(0);
		assertEquals(1, configAttributesList.size());
	}

	@Test
	public void resolveContextHierarchyAttributesForSingleTestClassWithSingleLevelContextHierarchy() throws Exception {
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(
				SingleTestClassWithSingleLevelContextHierarchy.class.getDeclaredMethod("something"));
		assertEquals(1, hierarchyAttributes.size());
		List<ContextConfigurationAttributes> configAttributesList = hierarchyAttributes.get(0);
		assertEquals(1, configAttributesList.size());
	}

	@Test
	public void resolveContextHierarchyAttributesForSingleTestClassWithSingleLevelContextHierarchyFromMetaAnnotation()
			throws Exception {
		Class<SingleTestClassWithSingleLevelContextHierarchyFromMetaAnnotation> testClass
				= SingleTestClassWithSingleLevelContextHierarchyFromMetaAnnotation.class;
		Method something = testClass.getDeclaredMethod("something");
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(something);
		assertEquals(1, hierarchyAttributes.size());

		List<ContextConfigurationAttributes> configAttributesList = hierarchyAttributes.get(0);
		assertNotNull(configAttributesList);
		assertEquals(1, configAttributesList.size());
		assertAttributes(configAttributesList.get(0),
				testClass,
				something,
				new String[] {"A.xml"},
				EMPTY_CLASS_ARRAY,
				ContextLoader.class,
				true);
	}

	@Test
	public void resolveContextHierarchyAttributesForSingleTestClassWithTripleLevelContextHierarchy() throws Exception {
		Class<SingleTestClassWithTripleLevelContextHierarchy> testClass
				= SingleTestClassWithTripleLevelContextHierarchy.class;
		Method something = testClass.getDeclaredMethod("something");
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(something);
		assertEquals(1, hierarchyAttributes.size());

		List<ContextConfigurationAttributes> configAttributesList = hierarchyAttributes.get(0);
		assertNotNull(configAttributesList);
		assertEquals(3, configAttributesList.size());
		assertAttributes(configAttributesList.get(0),
				testClass,
				something,
				new String[] {"A.xml"},
				EMPTY_CLASS_ARRAY,
				ContextLoader.class,
				true);
		assertAttributes(configAttributesList.get(1),
				testClass,
				something,
				new String[] {"B.xml"},
				EMPTY_CLASS_ARRAY,
				ContextLoader.class,
				true);
		assertAttributes(configAttributesList.get(2),
				testClass,
				something,
				new String[] {"C.xml"},
				EMPTY_CLASS_ARRAY,
				ContextLoader.class,
				true);
	}

	@Test
	public void resolveContextHierarchyAttributesForTestClassHierarchyWithSingleLevelContextHierarchies()
			throws Exception {
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(
				TestClass3WithSingleLevelContextHierarchy.class.getDeclaredMethod("something"));
		assertEquals(3, hierarchyAttributes.size());

		List<ContextConfigurationAttributes> configAttributesListClassLevel1 = hierarchyAttributes.get(0);
		assertEquals(1, configAttributesListClassLevel1.size());
		assertThat(configAttributesListClassLevel1.get(0).getLocations()[0], equalTo("one.xml"));

		List<ContextConfigurationAttributes> configAttributesListClassLevel2 = hierarchyAttributes.get(1);
		assertEquals(1, configAttributesListClassLevel2.size());
		assertArrayEquals(new String[] {"two-A.xml", "two-B.xml"},
				configAttributesListClassLevel2.get(0).getLocations());

		List<ContextConfigurationAttributes> configAttributesListClassLevel3 = hierarchyAttributes.get(2);
		assertEquals(1, configAttributesListClassLevel3.size());
		assertThat(configAttributesListClassLevel3.get(0).getLocations()[0], equalTo("three.xml"));
	}

	@Test
	public void
	resolveContextHierarchyAttributesForTestClassHierarchyWithSingleLevelContextHierarchiesAndMetaAnnotations()
			throws Exception {
		Method something = TestClass3WithSingleLevelContextHierarchyFromMetaAnnotation.class.getDeclaredMethod(
				"something");
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(something);
		assertEquals(3, hierarchyAttributes.size());

		List<ContextConfigurationAttributes> configAttributesListClassLevel1 = hierarchyAttributes.get(0);
		assertEquals(1, configAttributesListClassLevel1.size());
		assertThat(configAttributesListClassLevel1.get(0).getLocations()[0], equalTo("A.xml"));
		assertAttributes(configAttributesListClassLevel1.get(0),
				TestClass1WithSingleLevelContextHierarchyFromMetaAnnotation.class,
				TestClass1WithSingleLevelContextHierarchyFromMetaAnnotation.class.getDeclaredMethod("something"),
				new String[] {"A.xml"},
				EMPTY_CLASS_ARRAY,
				ContextLoader.class,
				true);

		List<ContextConfigurationAttributes> configAttributesListClassLevel2 = hierarchyAttributes.get(1);
		assertEquals(1, configAttributesListClassLevel2.size());
		assertArrayEquals(new String[] {"B-one.xml", "B-two.xml"},
				configAttributesListClassLevel2.get(0).getLocations());
		assertAttributes(configAttributesListClassLevel2.get(0),
				TestClass2WithSingleLevelContextHierarchyFromMetaAnnotation.class,
				TestClass2WithSingleLevelContextHierarchyFromMetaAnnotation.class.getDeclaredMethod("something"),
				new String[] {"B-one.xml", "B-two.xml"},
				EMPTY_CLASS_ARRAY,
				ContextLoader.class,
				true);

		List<ContextConfigurationAttributes> configAttributesListClassLevel3 = hierarchyAttributes.get(2);
		assertEquals(1, configAttributesListClassLevel3.size());
		assertThat(configAttributesListClassLevel3.get(0).getLocations()[0], equalTo("C.xml"));
		assertAttributes(configAttributesListClassLevel3.get(0),
				TestClass3WithSingleLevelContextHierarchyFromMetaAnnotation.class,
				something,
				new String[] {"C.xml"},
				EMPTY_CLASS_ARRAY,
				ContextLoader.class,
				true);
	}

	private void assertOneTwo(List<List<ContextConfigurationAttributes>> hierarchyAttributes) {
		assertEquals(2, hierarchyAttributes.size());

		List<ContextConfigurationAttributes> configAttributesListClassLevel1 = hierarchyAttributes.get(0);
		List<ContextConfigurationAttributes> configAttributesListClassLevel2 = hierarchyAttributes.get(1);

		assertEquals(1, configAttributesListClassLevel1.size());
		assertThat(configAttributesListClassLevel1.get(0).getLocations()[0], equalTo("one.xml"));

		assertEquals(1, configAttributesListClassLevel2.size());
		assertThat(configAttributesListClassLevel2.get(0).getLocations()[0], equalTo("two.xml"));
	}

	@Test
	public void resolveContextHierarchyAttributesForTestClassHierarchyWithBareContextConfigurationInSuperclass()
			throws Exception {
		assertOneTwo(resolveContextHierarchyAttributes(
				TestClass2WithBareContextConfigurationInSuperclass.class.getDeclaredMethod("something")));
	}

	@Test
	public void resolveContextHierarchyAttributesForTestClassHierarchyWithBareContextConfigurationInSubclass()
			throws Exception {
		assertOneTwo(resolveContextHierarchyAttributes(
				TestClass2WithBareContextConfigurationInSubclass.class.getDeclaredMethod("something")));
	}

	@Test
	public void
	resolveContextHierarchyAttributesForTestClassHierarchyWithBareMetaContextConfigWithOverridesInSuperclass()
			throws Exception {
		assertOneTwo(resolveContextHierarchyAttributes(
				TestClass2WithBareMetaContextConfigWithOverridesInSuperclass.class.getDeclaredMethod("something")));
	}

	@Test
	public void resolveContextHierarchyAttributesForTestClassHierarchyWithBareMetaContextConfigWithOverridesInSubclass()
			throws Exception {
		assertOneTwo(resolveContextHierarchyAttributes(
				TestClass2WithBareMetaContextConfigWithOverridesInSubclass.class.getDeclaredMethod("something")));
	}

	@Test
	public void resolveContextHierarchyAttributesForTestClassHierarchyWithMultiLevelContextHierarchies()
			throws Exception {
		List<List<ContextConfigurationAttributes>> hierarchyAttributes = resolveContextHierarchyAttributes(
				TestClass3WithMultiLevelContextHierarchy.class.getDeclaredMethod("something"));
		assertEquals(3, hierarchyAttributes.size());

		List<ContextConfigurationAttributes> configAttributesListClassLevel1 = hierarchyAttributes.get(0);
		assertEquals(2, configAttributesListClassLevel1.size());
		assertThat(configAttributesListClassLevel1.get(0).getLocations()[0], equalTo("1-A.xml"));
		assertThat(configAttributesListClassLevel1.get(1).getLocations()[0], equalTo("1-B.xml"));

		List<ContextConfigurationAttributes> configAttributesListClassLevel2 = hierarchyAttributes.get(1);
		assertEquals(2, configAttributesListClassLevel2.size());
		assertThat(configAttributesListClassLevel2.get(0).getLocations()[0], equalTo("2-A.xml"));
		assertThat(configAttributesListClassLevel2.get(1).getLocations()[0], equalTo("2-B.xml"));

		List<ContextConfigurationAttributes> configAttributesListClassLevel3 = hierarchyAttributes.get(2);
		assertEquals(3, configAttributesListClassLevel3.size());
		assertThat(configAttributesListClassLevel3.get(0).getLocations()[0], equalTo("3-A.xml"));
		assertThat(configAttributesListClassLevel3.get(1).getLocations()[0], equalTo("3-B.xml"));
		assertThat(configAttributesListClassLevel3.get(2).getLocations()[0], equalTo("3-C.xml"));
	}

	@Test
	public void buildContextHierarchyMapForTestClassHierarchyWithMultiLevelContextHierarchies() throws Exception {
		Map<String, List<ContextConfigurationAttributes>> map = buildContextHierarchyMap(
				TestClass3WithMultiLevelContextHierarchy.class.getDeclaredMethod("something"));

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
	public void buildContextHierarchyMapForTestClassHierarchyWithMultiLevelContextHierarchiesAndUnnamedConfig()
			throws Exception {
		Map<String, List<ContextConfigurationAttributes>> map = buildContextHierarchyMap(
				TestClass3WithMultiLevelContextHierarchyAndUnnamedConfig.class.getDeclaredMethod("something"));

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

		List<ContextConfigurationAttributes> level7Config = map.get(level7);
		assertThat(level7Config.size(), is(1));
		assertThat(level7Config.get(0).getLocations()[0], is("3-C.xml"));
	}

	@Test
	public void buildContextHierarchyMapForTestClassHierarchyWithMultiLevelContextHierarchiesAndPartiallyNamedConfig()
			throws Exception {
		Map<String, List<ContextConfigurationAttributes>> map = buildContextHierarchyMap(
				TestClass2WithMultiLevelContextHierarchyAndPartiallyNamedConfig.class.getDeclaredMethod("something"));

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

	private void assertContextConfigEntriesAreNotUnique(Method testMethod) {
		try {
			buildContextHierarchyMap(testMethod);
			fail("Should throw an IllegalStateException");
		} catch (IllegalStateException e) {
			String msg = String.format(
					"The @ContextConfiguration elements configured via @ContextHierarchy on test method [%s] and its "
							+ "supermethods must define unique contexts per hierarchy level.",
					testMethod.getName());
			assertEquals(msg, e.getMessage());
		}
	}

	@Test
	public void buildContextHierarchyMapForSingleTestClassWithMultiLevelContextHierarchyWithEmptyContextConfig()
			throws Exception {
		assertContextConfigEntriesAreNotUnique(
				SingleTestClassWithMultiLevelContextHierarchyWithEmptyContextConfig.class.getDeclaredMethod("something"));
	}

	@Test
	public void buildContextHierarchyMapForSingleTestClassWithMultiLevelContextHierarchyWithDuplicatedContextConfig()
			throws Exception {
		assertContextConfigEntriesAreNotUnique
				(SingleTestClassWithMultiLevelContextHierarchyWithDuplicatedContextConfig.class.getDeclaredMethod("something"));
	}

	@Test
	public void buildContextHierarchyForInterfaceHierarchy() throws Exception {
		Map<String, List<ContextConfigurationAttributes>> map = buildContextHierarchyMap(
				TestClassWithMethodAnnotatedInInterfaces.class.getDeclaredMethod("something"));
		String level1 = "superInterface";
		String level2 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 2;
		String level3 = "parent";
		String level4 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 4;
		String level5 = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + 5;

		assertThat(map.size(), is(5));
		assertThat(map.keySet(), hasItems(level1, level2, level3, level4, level5));
		Iterator<String> levels = map.keySet().iterator();
		assertThat(levels.next(), is(level1));
		assertThat(levels.next(), is(level2));
		assertThat(levels.next(), is(level3));
		assertThat(levels.next(), is(level4));
		assertThat(levels.next(), is(level5));

		List<ContextConfigurationAttributes> level1Config = map.get(level1);
		assertThat(level1Config.size(), is(1));
		assertThat(level1Config.get(0).getLocations()[0], is("boo.xml"));

		List<ContextConfigurationAttributes> level2Config = map.get(level2);
		assertThat(level2Config.size(), is(1));
		assertThat(level2Config.get(0).getLocations()[0], is("bak.xml"));

		List<ContextConfigurationAttributes> level3Config = map.get(level3);
		assertThat(level3Config.size(), is(1));
		assertThat(level3Config.get(0).getLocations()[0], is("baz.xml"));

		List<ContextConfigurationAttributes> level4Config = map.get(level4);
		assertThat(level4Config.size(), is(1));
		assertThat(level4Config.get(0).getLocations()[0], is("bar.xml"));

		List<ContextConfigurationAttributes> level5Config = map.get(level5);
		assertThat(level5Config.size(), is(1));
		assertThat(level5Config.get(0).getLocations()[0], is("foo.xml"));
	}


	//-------------------------------------------------------------

	private static class SingleTestClassWithContextConfigurationAndContextHierarchy {
		@ContextConfiguration("foo.xml")
		@ContextHierarchy(@ContextConfiguration("bar.xml"))
		public void something() {
			// for test purposes
		}

	}

	private static class SingleTestClassWithContextConfigurationAndContextHierarchyOnSingleMetaAnnotation {
		@ContextLoaderUtilsContextHierarchyTests.ContextConfigurationAndContextHierarchyOnSingleMeta
		public void something() {
			// for test purposes
		}
	}

	private static class SingleTestClassWithSingleLevelContextHierarchy {
		@ContextHierarchy(@ContextConfiguration("A.xml"))
		public void something() {
			// for test purposes
		}
	}

	private static class SingleTestClassWithTripleLevelContextHierarchy {
		@ContextHierarchy({//
				//
				@ContextConfiguration("A.xml"),//
				@ContextConfiguration("B.xml"),//
				@ContextConfiguration("C.xml") //
		})
		public void something() {
			// for test purposes
		}
	}

	private static class TestClass1WithSingleLevelContextHierarchy {
		@ContextHierarchy(@ContextConfiguration("one.xml"))
		public void something() {
			// for test purposes
		}
	}

	private static class TestClass2WithSingleLevelContextHierarchy extends TestClass1WithSingleLevelContextHierarchy {
		@ContextHierarchy(@ContextConfiguration({"two-A.xml", "two-B.xml"}))
		public void something() {
			// for test purposes
		}
	}

	private static class TestClass3WithSingleLevelContextHierarchy extends TestClass2WithSingleLevelContextHierarchy {
		@ContextHierarchy(@ContextConfiguration("three.xml"))
		public void something() {
			// for test purposes
		}
	}

	private static class TestClass1WithBareContextConfigurationInSuperclass {
		@ContextConfiguration("one.xml")
		public void something() {
			// for test purposes
		}
	}

	private static class TestClass2WithBareContextConfigurationInSuperclass extends TestClass1WithBareContextConfigurationInSuperclass {
		@ContextHierarchy(@ContextConfiguration("two.xml"))
		public void something() {
			// for test purposes
		}
	}

	private static class TestClass1WithBareContextConfigurationInSubclass {
		@ContextHierarchy(@ContextConfiguration("one.xml"))
		public void something() {
			// for test purposes
		}
	}

	private static class TestClass2WithBareContextConfigurationInSubclass extends TestClass1WithBareContextConfigurationInSubclass {
		@ContextConfiguration("two.xml")
		public void something() {
			// for test purposes
		}
	}

	private static class TestClass1WithBareMetaContextConfigWithOverridesInSuperclass {
		@ContextLoaderUtilsContextHierarchyTests.ContextConfigWithOverrides(locations = "one.xml")
		public void something() {
			// for test purposes
		}
	}

	private static class TestClass2WithBareMetaContextConfigWithOverridesInSuperclass extends TestClass1WithBareMetaContextConfigWithOverridesInSuperclass {
		@ContextHierarchy(@ContextConfiguration(locations = "two.xml"))
		public void something() {
			// for test purposes
		}
	}

	private static class TestClass1WithBareMetaContextConfigWithOverridesInSubclass {
		@ContextHierarchy(@ContextConfiguration(locations = "one.xml"))
		public void something() {
			// for test purposes
		}
	}

	private static class TestClass2WithBareMetaContextConfigWithOverridesInSubclass extends TestClass1WithBareMetaContextConfigWithOverridesInSubclass {
		@ContextLoaderUtilsContextHierarchyTests.ContextConfigWithOverrides(locations = "two.xml")
		public void something() {
			// for test purposes
		}
	}


	private static class SingleTestClassWithSingleLevelContextHierarchyFromMetaAnnotation {
		@ContextLoaderUtilsContextHierarchyTests.ContextHierarchyA
		public void something() {
			// for test purposes
		}
	}

	private static class TestClass1WithSingleLevelContextHierarchyFromMetaAnnotation {
		@ContextLoaderUtilsContextHierarchyTests.ContextHierarchyA
		public void something() {
			// for test purposes
		}
	}

	private static class TestClass2WithSingleLevelContextHierarchyFromMetaAnnotation extends TestClass1WithSingleLevelContextHierarchyFromMetaAnnotation {
		@ContextLoaderUtilsContextHierarchyTests.ContextHierarchyB
		public void something() {
			// for test purposes
		}
	}

	private static class TestClass3WithSingleLevelContextHierarchyFromMetaAnnotation extends TestClass2WithSingleLevelContextHierarchyFromMetaAnnotation {
		@ContextLoaderUtilsContextHierarchyTests.ContextHierarchyC
		public void something() {
			// for test purposes
		}
	}


	private static class TestClass1WithMultiLevelContextHierarchy {
		@ContextHierarchy({//
				//
				@ContextConfiguration(locations = "1-A.xml", name = "alpha"),//
				@ContextConfiguration(locations = "1-B.xml", name = "beta") //
		})
		public void something() {
			// for test purposes
		}
	}

	private static class TestClass2WithMultiLevelContextHierarchy extends TestClass1WithMultiLevelContextHierarchy {
		@Override
		@ContextHierarchy({//
				//
				@ContextConfiguration(locations = "2-A.xml", name = "alpha"),//
				@ContextConfiguration(locations = "2-B.xml", name = "beta") //
		})
		public void something() {
			// for test purposes
		}
	}

	private static class TestClass3WithMultiLevelContextHierarchy extends TestClass2WithMultiLevelContextHierarchy {
		@Override
		@ContextHierarchy({//
				//
				@ContextConfiguration(locations = "3-A.xml", name = "alpha"),//
				@ContextConfiguration(locations = "3-B.xml", name = "beta"),//
				@ContextConfiguration(locations = "3-C.xml", name = "gamma") //
		})
		public void something() {
			// for test purposes
		}
	}


	private static class TestClass1WithMultiLevelContextHierarchyAndUnnamedConfig {
		@ContextHierarchy({//
				//
				@ContextConfiguration(locations = "1-A.xml"),//
				@ContextConfiguration(locations = "1-B.xml") //
		})
		public void something() {
			// for test purposes
		}
	}


	private static class TestClass2WithMultiLevelContextHierarchyAndUnnamedConfig extends TestClass1WithMultiLevelContextHierarchyAndUnnamedConfig {
		@Override
		@ContextHierarchy({//
				//
				@ContextConfiguration(locations = "2-A.xml"),//
				@ContextConfiguration(locations = "2-B.xml") //
		})
		public void something() {
			// for test purposes
		}
	}


	private static class TestClass3WithMultiLevelContextHierarchyAndUnnamedConfig extends TestClass2WithMultiLevelContextHierarchyAndUnnamedConfig {
		@Override
		@ContextHierarchy({//
				//
				@ContextConfiguration(locations = "3-A.xml"),//
				@ContextConfiguration(locations = "3-B.xml"),//
				@ContextConfiguration(locations = "3-C.xml") //
		})
		public void something() {
			// for test purposes
		}
	}


	private static class TestClass1WithMultiLevelContextHierarchyAndPartiallyNamedConfig {
		@ContextHierarchy({//
				//
				@ContextConfiguration(locations = "1-A.xml", name = "parent"),//
				@ContextConfiguration(locations = "1-B.xml") //
		})
		public void something() {
			// for test purposes
		}
	}


	private static class TestClass2WithMultiLevelContextHierarchyAndPartiallyNamedConfig extends TestClass1WithMultiLevelContextHierarchyAndPartiallyNamedConfig {
		@Override
		@ContextHierarchy({//
				//
				@ContextConfiguration(locations = "2-A.xml", name = "parent"),//
				@ContextConfiguration(locations = "2-C.xml") //
		})
		public void something() {
			// for test purposes
		}
	}


	private static class SingleTestClassWithMultiLevelContextHierarchyWithEmptyContextConfig {
		@ContextHierarchy({
				//
				@ContextConfiguration,//
				@ContextConfiguration //
		})
		public void something() {
			// for test purposes
		}
	}

	private static class SingleTestClassWithMultiLevelContextHierarchyWithDuplicatedContextConfig {
		@ContextHierarchy({
				//
				@ContextConfiguration("foo.xml"),//
				@ContextConfiguration(classes = AbstractContextConfigurationUtilsTests.BarConfig.class),// duplicate!
				@ContextConfiguration("baz.xml"),//
				@ContextConfiguration(classes = AbstractContextConfigurationUtilsTests.BarConfig.class),// duplicate!
				@ContextConfiguration(loader = AnnotationConfigContextLoader.class) //
		})
		public void something() {
			// for test purposes
		}
	}

	private static class TestClassWithMethodAnnotatedInInterfaces extends TestClassWithMethodAnnotatedInInterfacesParent
			implements FirstLevelAnnotation {
		@Override
		public void something() {
			//for test purposes
		}
	}

	private interface FirstLevelAnnotation extends SecondLevelAnnotation {
		@Override
		@ContextHierarchy({@ContextConfiguration(value = "foo.xml")})
		void something();
	}

	private interface SecondLevelAnnotation extends ThirdLevelAnnotation {
		@Override
		@ContextHierarchy({@ContextConfiguration("bar.xml")})
		void something();
	}

	private interface ThirdLevelAnnotation {
		@ContextHierarchy({@ContextConfiguration(value = "baz.xml", name = "parent")})
		void something();
	}

	private static class TestClassWithMethodAnnotatedInInterfacesParent implements FirstLevelOnSuperClassInterface {
		@Override
		@ContextHierarchy({@ContextConfiguration(value = "bak.xml")})
		public void something() {
			//for test purposes
		}
	}

	private interface FirstLevelOnSuperClassInterface {
		@ContextHierarchy({@ContextConfiguration(value = "boo.xml", name = "superInterface")})
		void something();
	}

}
