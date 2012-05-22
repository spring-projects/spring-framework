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

package org.springframework.beans.factory.xml;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.DerivedTestBean;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.ITestBean;
import org.springframework.beans.IndexedTestBean;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.ResourceTestBean;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanIsAbstractException;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.DummyFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MethodReplacer;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.SerializationTestUtils;
import org.springframework.util.StopWatch;
import org.xml.sax.InputSource;

/**
 * Miscellaneous tests for XML bean definitions.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Rick Evans
 * @author Chris Beams
 */
@SuppressWarnings("deprecation")
public final class XmlBeanFactoryTests {

	private static final Class<?> CLASS = XmlBeanFactoryTests.class;
	private static final String CLASSNAME = CLASS.getSimpleName();

	private static final ClassPathResource AUTOWIRE_CONTEXT = classPathResource("-autowire.xml");
	private static final ClassPathResource CHILD_CONTEXT = classPathResource("-child.xml");
	private static final ClassPathResource CLASS_NOT_FOUND_CONTEXT = classPathResource("-classNotFound.xml");
	private static final ClassPathResource COMPLEX_FACTORY_CIRCLE_CONTEXT = classPathResource("-complexFactoryCircle.xml");
	private static final ClassPathResource CONSTRUCTOR_ARG_CONTEXT = classPathResource("-constructorArg.xml");
	private static final ClassPathResource CONSTRUCTOR_OVERRIDES_CONTEXT = classPathResource("-constructorOverrides.xml");
	private static final ClassPathResource DELEGATION_OVERRIDES_CONTEXT = classPathResource("-delegationOverrides.xml");
	private static final ClassPathResource DEP_CARG_AUTOWIRE_CONTEXT = classPathResource("-depCargAutowire.xml");
	private static final ClassPathResource DEP_CARG_INNER_CONTEXT = classPathResource("-depCargInner.xml");
	private static final ClassPathResource DEP_CARG_CONTEXT = classPathResource("-depCarg.xml");
	private static final ClassPathResource DEP_DEPENDSON_INNER_CONTEXT = classPathResource("-depDependsOnInner.xml");
	private static final ClassPathResource DEP_DEPENDSON_CONTEXT = classPathResource("-depDependsOn.xml");
	private static final ClassPathResource DEP_PROP = classPathResource("-depProp.xml");
	private static final ClassPathResource DEP_PROP_ABN_CONTEXT = classPathResource("-depPropAutowireByName.xml");
	private static final ClassPathResource DEP_PROP_ABT_CONTEXT = classPathResource("-depPropAutowireByType.xml");
	private static final ClassPathResource DEP_PROP_MIDDLE_CONTEXT = classPathResource("-depPropInTheMiddle.xml");
	private static final ClassPathResource DEP_PROP_INNER_CONTEXT = classPathResource("-depPropInner.xml");
	private static final ClassPathResource DEP_MATERIALIZE_CONTEXT = classPathResource("-depMaterializeThis.xml");
	private static final ClassPathResource FACTORY_CIRCLE_CONTEXT = classPathResource("-factoryCircle.xml");
	private static final ClassPathResource INITIALIZERS_CONTEXT = classPathResource("-initializers.xml");
	private static final ClassPathResource INVALID_CONTEXT = classPathResource("-invalid.xml");
	private static final ClassPathResource INVALID_NO_SUCH_METHOD_CONTEXT = classPathResource("-invalidOverridesNoSuchMethod.xml");
	private static final ClassPathResource COLLECTIONS_XSD_CONTEXT = classPathResource("-localCollectionsUsingXsd.xml");
	private static final ClassPathResource MISSING_CONTEXT = classPathResource("-missing.xml");
	private static final ClassPathResource OVERRIDES_CONTEXT = classPathResource("-overrides.xml");
	private static final ClassPathResource PARENT_CONTEXT = classPathResource("-parent.xml");
	private static final ClassPathResource NO_SUCH_FACTORY_METHOD_CONTEXT = classPathResource("-noSuchFactoryMethod.xml");
	private static final ClassPathResource RECURSIVE_IMPORT_CONTEXT = classPathResource("-recursiveImport.xml");
	private static final ClassPathResource RESOURCE_CONTEXT = classPathResource("-resource.xml");
	private static final ClassPathResource SATISFIED_ALL_DEP_CONTEXT = classPathResource("-satisfiedAllDepCheck.xml");
	private static final ClassPathResource SATISFIED_OBJECT_DEP_CONTEXT = classPathResource("-satisfiedObjectDepCheck.xml");
	private static final ClassPathResource SATISFIED_SIMPLE_DEP_CONTEXT = classPathResource("-satisfiedSimpleDepCheck.xml");
	private static final ClassPathResource TEST_WITH_DUP_NAMES_CONTEXT = classPathResource("-testWithDuplicateNames.xml");
	private static final ClassPathResource TEST_WITH_DUP_NAME_IN_ALIAS_CONTEXT = classPathResource("-testWithDuplicateNameInAlias.xml");
	private static final ClassPathResource UNSATISFIED_ALL_DEP_CONTEXT = classPathResource("-unsatisfiedAllDepCheckMissingObjects.xml");
	private static final ClassPathResource UNSATISFIED_OBJECT_DEP_CONTEXT = classPathResource("-unsatisfiedObjectDepCheck.xml");
	private static final ClassPathResource UNSATISFIED_SIMPLE_DEP_CONTEXT = classPathResource("-unsatisfiedSimpleDepCheck.xml");
	private static final ClassPathResource REFTYPES_CONTEXT = classPathResource("-reftypes.xml");
	private static final ClassPathResource DEFAULT_LAZY_CONTEXT = classPathResource("-defaultLazyInit.xml");
	private static final ClassPathResource DEFAULT_AUTOWIRE_CONTEXT = classPathResource("-defaultAutowire.xml");

	private static ClassPathResource classPathResource(String suffix) {
		return new ClassPathResource(CLASSNAME + suffix, CLASS);
	}

	/* SPR-2368 */
	@Test
	public void testCollectionsReferredToAsRefLocals() throws Exception {
		XmlBeanFactory factory = new XmlBeanFactory(COLLECTIONS_XSD_CONTEXT);
		factory.preInstantiateSingletons();
	}

	public @Test void testRefToSeparatePrototypeInstances() throws Exception {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		reader.loadBeanDefinitions(REFTYPES_CONTEXT);

		TestBean emma = (TestBean) xbf.getBean("emma");
		TestBean georgia = (TestBean) xbf.getBean("georgia");
		ITestBean emmasJenks = emma.getSpouse();
		ITestBean georgiasJenks = georgia.getSpouse();
		assertTrue("Emma and georgia think they have a different boyfriend", emmasJenks != georgiasJenks);
		assertTrue("Emmas jenks has right name", emmasJenks.getName().equals("Andrew"));
		assertTrue("Emmas doesn't equal new ref", emmasJenks != xbf.getBean("jenks"));
		assertTrue("Georgias jenks has right name", emmasJenks.getName().equals("Andrew"));
		assertTrue("They are object equal", emmasJenks.equals(georgiasJenks));
		assertTrue("They object equal direct ref", emmasJenks.equals(xbf.getBean("jenks")));
	}

	public @Test void testRefToSingleton() throws Exception {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		reader.loadBeanDefinitions(new EncodedResource(REFTYPES_CONTEXT, "ISO-8859-1"));

		TestBean jen = (TestBean) xbf.getBean("jenny");
		TestBean dave = (TestBean) xbf.getBean("david");
		TestBean jenks = (TestBean) xbf.getBean("jenks");
		ITestBean davesJen = dave.getSpouse();
		ITestBean jenksJen = jenks.getSpouse();
		assertTrue("1 jen instance", davesJen == jenksJen);
		assertTrue("1 jen instance", davesJen == jen);
	}

	public @Test void testInnerBeans() throws IOException {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);

		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		InputStream inputStream = getClass().getResourceAsStream(REFTYPES_CONTEXT.getPath());
		try {
			reader.loadBeanDefinitions(new InputSource(inputStream));
		}
		finally {
			inputStream.close();
		}

		// Let's create the outer bean named "innerBean",
		// to check whether it doesn't create any conflicts
		// with the actual inner beans named "innerBean".
		xbf.getBean("innerBean");

		TestBean hasInnerBeans = (TestBean) xbf.getBean("hasInnerBeans");
		assertEquals(5, hasInnerBeans.getAge());
		TestBean inner1 = (TestBean) hasInnerBeans.getSpouse();
		assertNotNull(inner1);
		assertEquals("innerBean#1", inner1.getBeanName());
		assertEquals("inner1", inner1.getName());
		assertEquals(6, inner1.getAge());

		assertNotNull(hasInnerBeans.getFriends());
		Object[] friends = hasInnerBeans.getFriends().toArray();
		assertEquals(3, friends.length);
		DerivedTestBean inner2 = (DerivedTestBean) friends[0];
		assertEquals("inner2", inner2.getName());
		assertTrue(inner2.getBeanName().startsWith(DerivedTestBean.class.getName()));
		assertFalse(xbf.containsBean("innerBean#1"));
		assertNotNull(inner2);
		assertEquals(7, inner2.getAge());
		TestBean innerFactory = (TestBean) friends[1];
		assertEquals(DummyFactory.SINGLETON_NAME, innerFactory.getName());
		TestBean inner5 = (TestBean) friends[2];
		assertEquals("innerBean#2", inner5.getBeanName());

		assertNotNull(hasInnerBeans.getSomeMap());
		assertEquals(2, hasInnerBeans.getSomeMap().size());
		TestBean inner3 = (TestBean) hasInnerBeans.getSomeMap().get("someKey");
		assertEquals("Jenny", inner3.getName());
		assertEquals(30, inner3.getAge());
		TestBean inner4 = (TestBean) hasInnerBeans.getSomeMap().get("someOtherKey");
		assertEquals("inner4", inner4.getName());
		assertEquals(9, inner4.getAge());

		TestBean hasInnerBeansForConstructor = (TestBean) xbf.getBean("hasInnerBeansForConstructor");
		TestBean innerForConstructor = (TestBean) hasInnerBeansForConstructor.getSpouse();
		assertNotNull(innerForConstructor);
		assertEquals("innerBean#3", innerForConstructor.getBeanName());
		assertEquals("inner1", innerForConstructor.getName());
		assertEquals(6, innerForConstructor.getAge());

		xbf.destroySingletons();
		assertTrue(inner1.wasDestroyed());
		assertTrue(inner2.wasDestroyed());
		assertTrue(innerFactory.getName() == null);
		assertTrue(inner5.wasDestroyed());
	}

	public @Test void testInnerBeansWithoutDestroy() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		reader.loadBeanDefinitions(REFTYPES_CONTEXT);

		// Let's create the outer bean named "innerBean",
		// to check whether it doesn't create any conflicts
		// with the actual inner beans named "innerBean".
		xbf.getBean("innerBean");

		TestBean hasInnerBeans = (TestBean) xbf.getBean("hasInnerBeansWithoutDestroy");
		assertEquals(5, hasInnerBeans.getAge());
		TestBean inner1 = (TestBean) hasInnerBeans.getSpouse();
		assertNotNull(inner1);
		assertEquals("innerBean", inner1.getBeanName());
		assertEquals("inner1", inner1.getName());
		assertEquals(6, inner1.getAge());

		assertNotNull(hasInnerBeans.getFriends());
		Object[] friends = hasInnerBeans.getFriends().toArray();
		assertEquals(3, friends.length);
		DerivedTestBean inner2 = (DerivedTestBean) friends[0];
		assertEquals("inner2", inner2.getName());
		assertTrue(inner2.getBeanName().startsWith(DerivedTestBean.class.getName()));
		assertNotNull(inner2);
		assertEquals(7, inner2.getAge());
		TestBean innerFactory = (TestBean) friends[1];
		assertEquals(DummyFactory.SINGLETON_NAME, innerFactory.getName());
		TestBean inner5 = (TestBean) friends[2];
		assertEquals("innerBean", inner5.getBeanName());
	}

	public @Test void testFailsOnInnerBean() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		reader.loadBeanDefinitions(REFTYPES_CONTEXT);

		try {
			xbf.getBean("failsOnInnerBean");
		}
		catch (BeanCreationException ex) {
			// Check whether message contains outer bean name.
			ex.printStackTrace();
			assertTrue(ex.getMessage().contains("failsOnInnerBean"));
			assertTrue(ex.getMessage().contains("someMap"));
		}

		try {
			xbf.getBean("failsOnInnerBeanForConstructor");
		}
		catch (BeanCreationException ex) {
			// Check whether message contains outer bean name.
			ex.printStackTrace();
			assertTrue(ex.getMessage().contains("failsOnInnerBeanForConstructor"));
			assertTrue(ex.getMessage().contains("constructor argument"));
		}
	}

	public @Test void testInheritanceFromParentFactoryPrototype() throws Exception {
		XmlBeanFactory parent = new XmlBeanFactory(PARENT_CONTEXT);
		XmlBeanFactory child = new XmlBeanFactory(CHILD_CONTEXT, parent);
		assertEquals(TestBean.class, child.getType("inheritsFromParentFactory"));
		TestBean inherits = (TestBean) child.getBean("inheritsFromParentFactory");
		// Name property value is overridden
		assertTrue(inherits.getName().equals("override"));
		// Age property is inherited from bean in parent factory
		assertTrue(inherits.getAge() == 1);
		TestBean inherits2 = (TestBean) child.getBean("inheritsFromParentFactory");
		assertFalse(inherits2 == inherits);
	}

	public @Test void testInheritanceWithDifferentClass() throws Exception {
		XmlBeanFactory parent = new XmlBeanFactory(PARENT_CONTEXT);
		XmlBeanFactory child = new XmlBeanFactory(CHILD_CONTEXT, parent);
		assertEquals(DerivedTestBean.class, child.getType("inheritsWithClass"));
		DerivedTestBean inherits = (DerivedTestBean) child.getBean("inheritsWithDifferentClass");
		// Name property value is overridden
		assertTrue(inherits.getName().equals("override"));
		// Age property is inherited from bean in parent factory
		assertTrue(inherits.getAge() == 1);
		assertTrue(inherits.wasInitialized());
	}

	public @Test void testInheritanceWithClass() throws Exception {
		XmlBeanFactory parent = new XmlBeanFactory(PARENT_CONTEXT);
		XmlBeanFactory child = new XmlBeanFactory(CHILD_CONTEXT, parent);
		assertEquals(DerivedTestBean.class, child.getType("inheritsWithClass"));
		DerivedTestBean inherits = (DerivedTestBean) child.getBean("inheritsWithClass");
		// Name property value is overridden
		assertTrue(inherits.getName().equals("override"));
		// Age property is inherited from bean in parent factory
		assertTrue(inherits.getAge() == 1);
		assertTrue(inherits.wasInitialized());
	}

	public @Test void testPrototypeInheritanceFromParentFactoryPrototype() throws Exception {
		XmlBeanFactory parent = new XmlBeanFactory(PARENT_CONTEXT);
		XmlBeanFactory child = new XmlBeanFactory(CHILD_CONTEXT, parent);
		assertEquals(TestBean.class, child.getType("prototypeInheritsFromParentFactoryPrototype"));
		TestBean inherits = (TestBean) child.getBean("prototypeInheritsFromParentFactoryPrototype");
		// Name property value is overridden
		assertTrue(inherits.getName().equals("prototype-override"));
		// Age property is inherited from bean in parent factory
		assertTrue(inherits.getAge() == 2);
		TestBean inherits2 = (TestBean) child.getBean("prototypeInheritsFromParentFactoryPrototype");
		assertFalse(inherits2 == inherits);
		inherits2.setAge(13);
		assertTrue(inherits2.getAge() == 13);
		// Shouldn't have changed first instance
		assertTrue(inherits.getAge() == 2);
	}

	public @Test void testPrototypeInheritanceFromParentFactorySingleton() throws Exception {
		XmlBeanFactory parent = new XmlBeanFactory(PARENT_CONTEXT);
		XmlBeanFactory child = new XmlBeanFactory(CHILD_CONTEXT, parent);
		TestBean inherits = (TestBean) child.getBean("protoypeInheritsFromParentFactorySingleton");
		// Name property value is overridden
		assertTrue(inherits.getName().equals("prototypeOverridesInheritedSingleton"));
		// Age property is inherited from bean in parent factory
		assertTrue(inherits.getAge() == 1);
		TestBean inherits2 = (TestBean) child.getBean("protoypeInheritsFromParentFactorySingleton");
		assertFalse(inherits2 == inherits);
		inherits2.setAge(13);
		assertTrue(inherits2.getAge() == 13);
		// Shouldn't have changed first instance
		assertTrue(inherits.getAge() == 1);
	}

	public @Test void testAutowireModeNotInherited() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(OVERRIDES_CONTEXT);

		TestBean david = (TestBean)xbf.getBean("magicDavid");
		// the parent bean is autowiring
		assertNotNull(david.getSpouse());

		TestBean derivedDavid = (TestBean)xbf.getBean("magicDavidDerived");
		// this fails while it inherits from the child bean
		assertNull("autowiring not propagated along child relationships", derivedDavid.getSpouse());
	}

	public @Test void testAbstractParentBeans() {
		XmlBeanFactory parent = new XmlBeanFactory(PARENT_CONTEXT);
		parent.preInstantiateSingletons();
		assertTrue(parent.isSingleton("inheritedTestBeanWithoutClass"));

		// abstract beans should not match
		Map<?, ?> tbs = parent.getBeansOfType(TestBean.class);
		assertEquals(2, tbs.size());
		assertTrue(tbs.containsKey("inheritedTestBeanPrototype"));
		assertTrue(tbs.containsKey("inheritedTestBeanSingleton"));

		// abstract bean should throw exception on creation attempt
		try {
			parent.getBean("inheritedTestBeanWithoutClass");
			fail("Should have thrown BeanIsAbstractException");
		}
		catch (BeanIsAbstractException ex) {
			// expected
		}

		// non-abstract bean should work, even if it serves as parent
		assertTrue(parent.getBean("inheritedTestBeanPrototype") instanceof TestBean);
	}

	public @Test void testDependenciesMaterializeThis() throws Exception {
		XmlBeanFactory xbf = new XmlBeanFactory(DEP_MATERIALIZE_CONTEXT);

		assertEquals(2, xbf.getBeansOfType(DummyBo.class, true, false).size());
		assertEquals(3, xbf.getBeansOfType(DummyBo.class, true, true).size());
		assertEquals(3, xbf.getBeansOfType(DummyBo.class, true, false).size());
		assertEquals(3, xbf.getBeansOfType(DummyBo.class).size());
		assertEquals(2, xbf.getBeansOfType(DummyBoImpl.class, true, true).size());
		assertEquals(1, xbf.getBeansOfType(DummyBoImpl.class, false, true).size());
		assertEquals(2, xbf.getBeansOfType(DummyBoImpl.class).size());

		DummyBoImpl bos = (DummyBoImpl) xbf.getBean("boSingleton");
		DummyBoImpl bop = (DummyBoImpl) xbf.getBean("boPrototype");
		assertNotSame(bos, bop);
		assertTrue(bos.dao == bop.dao);
	}

	public @Test void testChildOverridesParentBean() throws Exception {
		XmlBeanFactory parent = new XmlBeanFactory(PARENT_CONTEXT);
		XmlBeanFactory child = new XmlBeanFactory(CHILD_CONTEXT, parent);
		TestBean inherits = (TestBean) child.getBean("inheritedTestBean");
		// Name property value is overridden
		assertTrue(inherits.getName().equals("overrideParentBean"));
		// Age property is inherited from bean in parent factory
		assertTrue(inherits.getAge() == 1);
		TestBean inherits2 = (TestBean) child.getBean("inheritedTestBean");
		assertTrue(inherits2 != inherits);
	}

	/**
	 * Check that a prototype can't inherit from a bogus parent.
	 * If a singleton does this the factory will fail to load.
	 */
	public @Test void testBogusParentageFromParentFactory() throws Exception {
		XmlBeanFactory parent = new XmlBeanFactory(PARENT_CONTEXT);
		XmlBeanFactory child = new XmlBeanFactory(CHILD_CONTEXT, parent);
		try {
			child.getBean("bogusParent", TestBean.class);
			fail();
		}
		catch (BeanDefinitionStoreException ex) {
			// check exception message contains the name
			assertTrue(ex.getMessage().indexOf("bogusParent") != -1);
			assertTrue(ex.getCause() instanceof NoSuchBeanDefinitionException);
		}
	}

	/**
	 * Note that prototype/singleton distinction is <b>not</b> inherited.
	 * It's possible for a subclass singleton not to return independent
	 * instances even if derived from a prototype
	 */
	public @Test void testSingletonInheritsFromParentFactoryPrototype() throws Exception {
		XmlBeanFactory parent = new XmlBeanFactory(PARENT_CONTEXT);
		XmlBeanFactory child = new XmlBeanFactory(CHILD_CONTEXT, parent);
		TestBean inherits = (TestBean) child.getBean("singletonInheritsFromParentFactoryPrototype");
		// Name property value is overriden
		assertTrue(inherits.getName().equals("prototype-override"));
		// Age property is inherited from bean in parent factory
		assertTrue(inherits.getAge() == 2);
		TestBean inherits2 = (TestBean) child.getBean("singletonInheritsFromParentFactoryPrototype");
		assertTrue(inherits2 == inherits);
	}

	public @Test void testSingletonFromParent() {
		XmlBeanFactory parent = new XmlBeanFactory(PARENT_CONTEXT);
		TestBean beanFromParent = (TestBean) parent.getBean("inheritedTestBeanSingleton");
		XmlBeanFactory child = new XmlBeanFactory(CHILD_CONTEXT, parent);
		TestBean beanFromChild = (TestBean) child.getBean("inheritedTestBeanSingleton");
		assertTrue("singleton from parent and child is the same", beanFromParent == beanFromChild);
	}

	public @Test void testNestedPropertyValue() {
		XmlBeanFactory parent = new XmlBeanFactory(PARENT_CONTEXT);
		XmlBeanFactory child = new XmlBeanFactory(CHILD_CONTEXT, parent);
		IndexedTestBean bean = (IndexedTestBean) child.getBean("indexedTestBean");
		assertEquals("name applied correctly", "myname", bean.getArray()[0].getName());
	}

	public @Test void testCircularReferences() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		reader.loadBeanDefinitions(REFTYPES_CONTEXT);
		TestBean jenny = (TestBean) xbf.getBean("jenny");
		TestBean david = (TestBean) xbf.getBean("david");
		TestBean ego = (TestBean) xbf.getBean("ego");
		TestBean complexInnerEgo = (TestBean) xbf.getBean("complexInnerEgo");
		TestBean complexEgo = (TestBean) xbf.getBean("complexEgo");
		assertTrue("Correct circular reference", jenny.getSpouse() == david);
		assertTrue("Correct circular reference", david.getSpouse() == jenny);
		assertTrue("Correct circular reference", ego.getSpouse() == ego);
		assertTrue("Correct circular reference", complexInnerEgo.getSpouse().getSpouse() == complexInnerEgo);
		assertTrue("Correct circular reference", complexEgo.getSpouse().getSpouse() == complexEgo);
	}

	public @Test void testCircularReferenceWithFactoryBeanFirst() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		reader.loadBeanDefinitions(REFTYPES_CONTEXT);
		xbf.getBean("egoBridge");
		TestBean complexEgo = (TestBean) xbf.getBean("complexEgo");
		assertTrue("Correct circular reference", complexEgo.getSpouse().getSpouse() == complexEgo);
	}

	public @Test void testCircularReferenceWithTwoFactoryBeans() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		reader.loadBeanDefinitions(REFTYPES_CONTEXT);
		TestBean ego1 = (TestBean) xbf.getBean("ego1");
		assertTrue("Correct circular reference", ego1.getSpouse().getSpouse() == ego1);
		TestBean ego3 = (TestBean) xbf.getBean("ego3");
		assertTrue("Correct circular reference", ego3.getSpouse().getSpouse() == ego3);
	}

	public @Test void testCircularReferencesWithNotAllowed() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		xbf.setAllowCircularReferences(false);
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		reader.loadBeanDefinitions(REFTYPES_CONTEXT);
		try {
			xbf.getBean("jenny");
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.contains(BeanCurrentlyInCreationException.class));
		}
	}

	public @Test void testCircularReferencesWithWrapping() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		reader.loadBeanDefinitions(REFTYPES_CONTEXT);
		xbf.addBeanPostProcessor(new WrappingPostProcessor());
		try {
			xbf.getBean("jenny");
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.contains(BeanCurrentlyInCreationException.class));
		}
	}

	public @Test void testCircularReferencesWithWrappingAndRawInjectionAllowed() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		xbf.setAllowRawInjectionDespiteWrapping(true);
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		reader.loadBeanDefinitions(REFTYPES_CONTEXT);
		xbf.addBeanPostProcessor(new WrappingPostProcessor());

		ITestBean jenny = (ITestBean) xbf.getBean("jenny");
		ITestBean david = (ITestBean) xbf.getBean("david");
		assertTrue(AopUtils.isAopProxy(jenny));
		assertTrue(AopUtils.isAopProxy(david));
		assertSame(david, jenny.getSpouse());
		assertNotSame(jenny, david.getSpouse());
		assertEquals("Jenny", david.getSpouse().getName());
		assertSame(david, david.getSpouse().getSpouse());
		assertTrue(AopUtils.isAopProxy(jenny.getSpouse()));
		assertTrue(!AopUtils.isAopProxy(david.getSpouse()));
	}

	public @Test void testFactoryReferenceCircle() {
		XmlBeanFactory xbf = new XmlBeanFactory(FACTORY_CIRCLE_CONTEXT);
		TestBean tb = (TestBean) xbf.getBean("singletonFactory");
		DummyFactory db = (DummyFactory) xbf.getBean("&singletonFactory");
		assertTrue(tb == db.getOtherTestBean());
	}

	public @Test void testFactoryReferenceWithDoublePrefix() {
		XmlBeanFactory xbf = new XmlBeanFactory(FACTORY_CIRCLE_CONTEXT);
		assertThat(xbf.getBean("&&singletonFactory"), instanceOf(DummyFactory.class));
	}

	public @Test void testComplexFactoryReferenceCircle() {
		XmlBeanFactory xbf = new XmlBeanFactory(COMPLEX_FACTORY_CIRCLE_CONTEXT);
		xbf.getBean("proxy1");
		// check that unused instances from autowiring got removed
		assertEquals(4, xbf.getSingletonCount());
		// properly create the remaining two instances
		xbf.getBean("proxy2");
		assertEquals(5, xbf.getSingletonCount());
	}

	public @Test void testNoSuchFactoryBeanMethod() {
		try {
			XmlBeanFactory xbf = new XmlBeanFactory(NO_SUCH_FACTORY_METHOD_CONTEXT);
			assertNotNull(xbf.getBean("defaultTestBean"));
			fail("Should not get invalid bean");
		}
		catch (BeanCreationException ex) {
			// Ok
		}
	}

	public @Test void testInitMethodIsInvoked() throws Exception {
		XmlBeanFactory xbf = new XmlBeanFactory(INITIALIZERS_CONTEXT);
		DoubleInitializer in = (DoubleInitializer) xbf.getBean("init-method1");
		// Initializer should have doubled value
		assertEquals(14, in.getNum());
	}

	/**
	 * Test that if a custom initializer throws an exception, it's handled correctly
	 */
	public @Test void testInitMethodThrowsException() {
		XmlBeanFactory xbf = new XmlBeanFactory(INITIALIZERS_CONTEXT);
		try {
			xbf.getBean("init-method2");
			fail();
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.getResourceDescription().indexOf("initializers.xml") != -1);
			assertEquals("init-method2", ex.getBeanName());
			assertTrue(ex.getCause() instanceof IOException);
		}
	}

	public @Test void testNoSuchInitMethod() throws Exception {
		XmlBeanFactory xbf = new XmlBeanFactory(INITIALIZERS_CONTEXT);
		try {
			xbf.getBean("init-method3");
			fail();
		}
		catch (FatalBeanException ex) {
			// check message is helpful
			assertTrue(ex.getMessage().indexOf("initializers.xml") != -1);
			assertTrue(ex.getMessage().indexOf("init-method3") != -1);
			assertTrue(ex.getMessage().indexOf("init") != -1);
		}
	}

	/**
	 * Check that InitializingBean method is called first.
	 */
	public @Test void testInitializingBeanAndInitMethod() throws Exception {
		InitAndIB.constructed = false;
		XmlBeanFactory xbf = new XmlBeanFactory(INITIALIZERS_CONTEXT);
		assertFalse(InitAndIB.constructed);
		xbf.preInstantiateSingletons();
		assertFalse(InitAndIB.constructed);
		InitAndIB iib = (InitAndIB) xbf.getBean("init-and-ib");
		assertTrue(InitAndIB.constructed);
		assertTrue(iib.afterPropertiesSetInvoked && iib.initMethodInvoked);
		assertTrue(!iib.destroyed && !iib.customDestroyed);
		xbf.destroySingletons();
		assertTrue(iib.destroyed && iib.customDestroyed);
		xbf.destroySingletons();
		assertTrue(iib.destroyed && iib.customDestroyed);
	}

	/**
	 * Check that InitializingBean method is not called twice.
	 */
	public @Test void testInitializingBeanAndSameInitMethod() throws Exception {
		InitAndIB.constructed = false;
		XmlBeanFactory xbf = new XmlBeanFactory(INITIALIZERS_CONTEXT);
		assertFalse(InitAndIB.constructed);
		xbf.preInstantiateSingletons();
		assertFalse(InitAndIB.constructed);
		InitAndIB iib = (InitAndIB) xbf.getBean("ib-same-init");
		assertTrue(InitAndIB.constructed);
		assertTrue(iib.afterPropertiesSetInvoked && !iib.initMethodInvoked);
		assertTrue(!iib.destroyed && !iib.customDestroyed);
		xbf.destroySingletons();
		assertTrue(iib.destroyed && !iib.customDestroyed);
		xbf.destroySingletons();
		assertTrue(iib.destroyed && !iib.customDestroyed);
	}

	public @Test void testDefaultLazyInit() throws Exception {
		InitAndIB.constructed = false;
		XmlBeanFactory xbf = new XmlBeanFactory(DEFAULT_LAZY_CONTEXT);
		assertFalse(InitAndIB.constructed);
		xbf.preInstantiateSingletons();
		assertTrue(InitAndIB.constructed);
		try {
			xbf.getBean("lazy-and-bad");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.getCause() instanceof IOException);
		}
	}

	public @Test void testNoSuchXmlFile() throws Exception {
		try {
			new XmlBeanFactory(MISSING_CONTEXT);
			fail("Must not create factory from missing XML");
		}
		catch (BeanDefinitionStoreException expected) {
		}
	}

	public @Test void testInvalidXmlFile() throws Exception {
		try {
			new XmlBeanFactory(INVALID_CONTEXT);
			fail("Must not create factory from invalid XML");
		}
		catch (BeanDefinitionStoreException expected) {
		}
	}

	public @Test void testUnsatisfiedObjectDependencyCheck() throws Exception {
		try {
			XmlBeanFactory xbf = new XmlBeanFactory(UNSATISFIED_OBJECT_DEP_CONTEXT);
			xbf.getBean("a", DependenciesBean.class);
			fail("Must have thrown an UnsatisfiedDependencyException");
		}
		catch (UnsatisfiedDependencyException ex) {
		}
	}

	public @Test void testUnsatisfiedSimpleDependencyCheck() throws Exception {
		try {
			XmlBeanFactory xbf = new XmlBeanFactory(UNSATISFIED_SIMPLE_DEP_CONTEXT);
			xbf.getBean("a", DependenciesBean.class);
			fail("Must have thrown an UnsatisfiedDependencyException");
		}
		catch (UnsatisfiedDependencyException expected) {
		}
	}

	public @Test void testSatisfiedObjectDependencyCheck() throws Exception {
		XmlBeanFactory xbf = new XmlBeanFactory(SATISFIED_OBJECT_DEP_CONTEXT);
		DependenciesBean a = (DependenciesBean) xbf.getBean("a");
		assertNotNull(a.getSpouse());
		assertEquals(xbf, a.getBeanFactory());
	}

	public @Test void testSatisfiedSimpleDependencyCheck() throws Exception {
		XmlBeanFactory xbf = new XmlBeanFactory(SATISFIED_SIMPLE_DEP_CONTEXT);
		DependenciesBean a = (DependenciesBean) xbf.getBean("a");
		assertEquals(a.getAge(), 33);
	}

	public @Test void testUnsatisfiedAllDependencyCheck() throws Exception {
		try {
			XmlBeanFactory xbf = new XmlBeanFactory(UNSATISFIED_ALL_DEP_CONTEXT);
			xbf.getBean("a", DependenciesBean.class);
			fail("Must have thrown an UnsatisfiedDependencyException");
		}
		catch (UnsatisfiedDependencyException expected) {
		}
	}

	public @Test void testSatisfiedAllDependencyCheck() throws Exception {
		XmlBeanFactory xbf = new XmlBeanFactory(SATISFIED_ALL_DEP_CONTEXT);
		DependenciesBean a = (DependenciesBean) xbf.getBean("a");
		assertEquals(a.getAge(), 33);
		assertNotNull(a.getName());
		assertNotNull(a.getSpouse());
	}

	public @Test void testAutowire() throws Exception {
		XmlBeanFactory xbf = new XmlBeanFactory(AUTOWIRE_CONTEXT);
		TestBean spouse = new TestBean("kerry", 0);
		xbf.registerSingleton("spouse", spouse);
		doTestAutowire(xbf);
	}

	public @Test void testAutowireWithParent() throws Exception {
		XmlBeanFactory xbf = new XmlBeanFactory(AUTOWIRE_CONTEXT);
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "kerry");
		lbf.registerBeanDefinition("spouse", new RootBeanDefinition(TestBean.class, pvs));
		xbf.setParentBeanFactory(lbf);
		doTestAutowire(xbf);
	}

	private void doTestAutowire(XmlBeanFactory xbf) throws Exception {
		DependenciesBean rod1 = (DependenciesBean) xbf.getBean("rod1");
		TestBean kerry = (TestBean) xbf.getBean("spouse");
		// should have been autowired
		assertEquals(kerry, rod1.getSpouse());

		DependenciesBean rod1a = (DependenciesBean) xbf.getBean("rod1a");
		// should have been autowired
		assertEquals(kerry, rod1a.getSpouse());

		DependenciesBean rod2 = (DependenciesBean) xbf.getBean("rod2");
		// should have been autowired
		assertEquals(kerry, rod2.getSpouse());

		DependenciesBean rod2a = (DependenciesBean) xbf.getBean("rod2a");
		// should have been set explicitly
		assertEquals(kerry, rod2a.getSpouse());

		ConstructorDependenciesBean rod3 = (ConstructorDependenciesBean) xbf.getBean("rod3");
		IndexedTestBean other = (IndexedTestBean) xbf.getBean("other");
		// should have been autowired
		assertEquals(kerry, rod3.getSpouse1());
		assertEquals(kerry, rod3.getSpouse2());
		assertEquals(other, rod3.getOther());

		ConstructorDependenciesBean rod3a = (ConstructorDependenciesBean) xbf.getBean("rod3a");
		// should have been autowired
		assertEquals(kerry, rod3a.getSpouse1());
		assertEquals(kerry, rod3a.getSpouse2());
		assertEquals(other, rod3a.getOther());

		try {
			xbf.getBean("rod4", ConstructorDependenciesBean.class);
			fail("Must have thrown a FatalBeanException");
		}
		catch (FatalBeanException expected) {
			// expected
		}

		DependenciesBean rod5 = (DependenciesBean) xbf.getBean("rod5");
		// Should not have been autowired
		assertNull(rod5.getSpouse());

		BeanFactory appCtx = (BeanFactory) xbf.getBean("childAppCtx");
		assertTrue(appCtx.containsBean("rod1"));
		assertTrue(appCtx.containsBean("jenny"));
	}

	public @Test void testAutowireWithDefault() throws Exception {
		XmlBeanFactory xbf = new XmlBeanFactory(DEFAULT_AUTOWIRE_CONTEXT);

		DependenciesBean rod1 = (DependenciesBean) xbf.getBean("rod1");
		// should have been autowired
		assertNotNull(rod1.getSpouse());
		assertTrue(rod1.getSpouse().getName().equals("Kerry"));

		DependenciesBean rod2 = (DependenciesBean) xbf.getBean("rod2");
		// should have been autowired
		assertNotNull(rod2.getSpouse());
		assertTrue(rod2.getSpouse().getName().equals("Kerry"));

		try {
			xbf.getBean("rod3", DependenciesBean.class);
			fail("Must have thrown UnsatisfiedDependencyException");
		}
		catch (UnsatisfiedDependencyException expected) {
		}
	}

	public @Test void testAutowireByConstructor() throws Exception {
		XmlBeanFactory xbf = new XmlBeanFactory(CONSTRUCTOR_ARG_CONTEXT);
		ConstructorDependenciesBean rod1 = (ConstructorDependenciesBean) xbf.getBean("rod1");
		TestBean kerry = (TestBean) xbf.getBean("kerry2");
		// should have been autowired
		assertEquals(kerry, rod1.getSpouse1());
		assertEquals(0, rod1.getAge());
		assertEquals(null, rod1.getName());

		ConstructorDependenciesBean rod2 = (ConstructorDependenciesBean) xbf.getBean("rod2");
		TestBean kerry1 = (TestBean) xbf.getBean("kerry1");
		TestBean kerry2 = (TestBean) xbf.getBean("kerry2");
		// should have been autowired
		assertEquals(kerry2, rod2.getSpouse1());
		assertEquals(kerry1, rod2.getSpouse2());
		assertEquals(0, rod2.getAge());
		assertEquals(null, rod2.getName());

		ConstructorDependenciesBean rod = (ConstructorDependenciesBean) xbf.getBean("rod3");
		IndexedTestBean other = (IndexedTestBean) xbf.getBean("other");
		// should have been autowired
		assertEquals(kerry, rod.getSpouse1());
		assertEquals(kerry, rod.getSpouse2());
		assertEquals(other, rod.getOther());
		assertEquals(0, rod.getAge());
		assertEquals(null, rod.getName());

		xbf.getBean("rod4", ConstructorDependenciesBean.class);
		// should have been autowired
		assertEquals(kerry, rod.getSpouse1());
		assertEquals(kerry, rod.getSpouse2());
		assertEquals(other, rod.getOther());
		assertEquals(0, rod.getAge());
		assertEquals(null, rod.getName());
	}

	public @Test void testAutowireByConstructorWithSimpleValues() throws Exception {
		XmlBeanFactory xbf = new XmlBeanFactory(CONSTRUCTOR_ARG_CONTEXT);

		ConstructorDependenciesBean rod5 = (ConstructorDependenciesBean) xbf.getBean("rod5");
		TestBean kerry1 = (TestBean) xbf.getBean("kerry1");
		TestBean kerry2 = (TestBean) xbf.getBean("kerry2");
		IndexedTestBean other = (IndexedTestBean) xbf.getBean("other");
		// should have been autowired
		assertEquals(kerry2, rod5.getSpouse1());
		assertEquals(kerry1, rod5.getSpouse2());
		assertEquals(other, rod5.getOther());
		assertEquals(99, rod5.getAge());
		assertEquals("myname", rod5.getName());

		DerivedConstructorDependenciesBean rod6 = (DerivedConstructorDependenciesBean) xbf.getBean("rod6");
		// should have been autowired
		assertTrue(rod6.initialized);
		assertTrue(!rod6.destroyed);
		assertEquals(kerry2, rod6.getSpouse1());
		assertEquals(kerry1, rod6.getSpouse2());
		assertEquals(other, rod6.getOther());
		assertEquals(0, rod6.getAge());
		assertEquals(null, rod6.getName());

		xbf.destroySingletons();
		assertTrue(rod6.destroyed);
	}

	public @Test void testRelatedCausesFromConstructorResolution() {
		XmlBeanFactory xbf = new XmlBeanFactory(CONSTRUCTOR_ARG_CONTEXT);

		try {
			xbf.getBean("rod2Accessor");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.toString().indexOf("touchy") != -1);
			ex.printStackTrace();
			assertNull(ex.getRelatedCauses());
		}
	}

	public @Test void testConstructorArgResolution() {
		XmlBeanFactory xbf = new XmlBeanFactory(CONSTRUCTOR_ARG_CONTEXT);
		TestBean kerry1 = (TestBean) xbf.getBean("kerry1");
		TestBean kerry2 = (TestBean) xbf.getBean("kerry2");

		ConstructorDependenciesBean rod9 = (ConstructorDependenciesBean) xbf.getBean("rod9");
		assertEquals(99, rod9.getAge());
		ConstructorDependenciesBean rod9a = (ConstructorDependenciesBean) xbf.getBean("rod9", 98);
		assertEquals(98, rod9a.getAge());
		ConstructorDependenciesBean rod9b = (ConstructorDependenciesBean) xbf.getBean("rod9", "myName");
		assertEquals("myName", rod9b.getName());
		ConstructorDependenciesBean rod9c = (ConstructorDependenciesBean) xbf.getBean("rod9", 97);
		assertEquals(97, rod9c.getAge());

		ConstructorDependenciesBean rod10 = (ConstructorDependenciesBean) xbf.getBean("rod10");
		assertEquals(null, rod10.getName());

		ConstructorDependenciesBean rod11 = (ConstructorDependenciesBean) xbf.getBean("rod11");
		assertEquals(kerry2, rod11.getSpouse1());

		ConstructorDependenciesBean rod12 = (ConstructorDependenciesBean) xbf.getBean("rod12");
		assertEquals(kerry1, rod12.getSpouse1());
		assertNull(rod12.getSpouse2());

		ConstructorDependenciesBean rod13 = (ConstructorDependenciesBean) xbf.getBean("rod13");
		assertEquals(kerry1, rod13.getSpouse1());
		assertEquals(kerry2, rod13.getSpouse2());

		ConstructorDependenciesBean rod14 = (ConstructorDependenciesBean) xbf.getBean("rod14");
		assertEquals(kerry1, rod14.getSpouse1());
		assertEquals(kerry2, rod14.getSpouse2());

		ConstructorDependenciesBean rod15 = (ConstructorDependenciesBean) xbf.getBean("rod15");
		assertEquals(kerry2, rod15.getSpouse1());
		assertEquals(kerry1, rod15.getSpouse2());

		ConstructorDependenciesBean rod16 = (ConstructorDependenciesBean) xbf.getBean("rod16");
		assertEquals(kerry2, rod16.getSpouse1());
		assertEquals(kerry1, rod16.getSpouse2());
		assertEquals(29, rod16.getAge());

		ConstructorDependenciesBean rod17 = (ConstructorDependenciesBean) xbf.getBean("rod17");
		assertEquals(kerry1, rod17.getSpouse1());
		assertEquals(kerry2, rod17.getSpouse2());
		assertEquals(29, rod17.getAge());
	}

	public @Test void testPrototypeWithExplicitArguments() {
		XmlBeanFactory xbf = new XmlBeanFactory(CONSTRUCTOR_ARG_CONTEXT);
		SimpleConstructorArgBean cd1 = (SimpleConstructorArgBean) xbf.getBean("rod18");
		assertEquals(0, cd1.getAge());
		SimpleConstructorArgBean cd2 = (SimpleConstructorArgBean) xbf.getBean("rod18", 98);
		assertEquals(98, cd2.getAge());
		SimpleConstructorArgBean cd3 = (SimpleConstructorArgBean) xbf.getBean("rod18", "myName");
		assertEquals("myName", cd3.getName());
		SimpleConstructorArgBean cd4 = (SimpleConstructorArgBean) xbf.getBean("rod18");
		assertEquals(0, cd4.getAge());
		SimpleConstructorArgBean cd5 = (SimpleConstructorArgBean) xbf.getBean("rod18", 97);
		assertEquals(97, cd5.getAge());
	}

	public @Test void testConstructorArgWithSingleMatch() {
		XmlBeanFactory xbf = new XmlBeanFactory(CONSTRUCTOR_ARG_CONTEXT);
		File file = (File) xbf.getBean("file");
		assertEquals(File.separator + "test", file.getPath());
	}

	public @Test void testThrowsExceptionOnTooManyArguments() throws Exception {
		XmlBeanFactory xbf = new XmlBeanFactory(CONSTRUCTOR_ARG_CONTEXT);
		try {
			xbf.getBean("rod7", ConstructorDependenciesBean.class);
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException expected) {
		}
	}

	public @Test void testThrowsExceptionOnAmbiguousResolution() throws Exception {
		XmlBeanFactory xbf = new XmlBeanFactory(CONSTRUCTOR_ARG_CONTEXT);
		try {
			xbf.getBean("rod8", ConstructorDependenciesBean.class);
			fail("Must have thrown UnsatisfiedDependencyException");
		}
		catch (UnsatisfiedDependencyException expected) {
		}
	}

	public @Test void testDependsOn() {
		doTestDependencies(DEP_DEPENDSON_CONTEXT, 1);
	}

	public @Test void testDependsOnInInnerBean() {
		doTestDependencies(DEP_DEPENDSON_INNER_CONTEXT, 4);
	}

	public @Test void testDependenciesThroughConstructorArguments() {
		doTestDependencies(DEP_CARG_CONTEXT, 1);
	}

	public @Test void testDependenciesThroughConstructorArgumentAutowiring() {
		doTestDependencies(DEP_CARG_AUTOWIRE_CONTEXT, 1);
	}

	public @Test void testDependenciesThroughConstructorArgumentsInInnerBean() {
		doTestDependencies(DEP_CARG_INNER_CONTEXT, 1);
	}

	public @Test void testDependenciesThroughProperties() {
		doTestDependencies(DEP_PROP, 1);
	}

	public @Test void testDependenciesThroughPropertiesWithInTheMiddle() {
		doTestDependencies(DEP_PROP_MIDDLE_CONTEXT, 1);
	}

	public @Test void testDependenciesThroughPropertyAutowiringByName() {
		doTestDependencies(DEP_PROP_ABN_CONTEXT, 1);
	}

	public @Test void testDependenciesThroughPropertyAutowiringByType() {
		doTestDependencies(DEP_PROP_ABT_CONTEXT, 1);
	}

	public @Test void testDependenciesThroughPropertiesInInnerBean() {
		doTestDependencies(DEP_PROP_INNER_CONTEXT, 1);
	}

	private void doTestDependencies(ClassPathResource resource, int nrOfHoldingBeans) {
		PreparingBean1.prepared = false;
		PreparingBean1.destroyed = false;
		PreparingBean2.prepared = false;
		PreparingBean2.destroyed = false;
		DependingBean.destroyCount = 0;
		HoldingBean.destroyCount = 0;
		XmlBeanFactory xbf = new XmlBeanFactory(resource);
		xbf.preInstantiateSingletons();
		xbf.destroySingletons();
		assertTrue(PreparingBean1.prepared);
		assertTrue(PreparingBean1.destroyed);
		assertTrue(PreparingBean2.prepared);
		assertTrue(PreparingBean2.destroyed);
		assertEquals(nrOfHoldingBeans, DependingBean.destroyCount);
		if (!xbf.getBeansOfType(HoldingBean.class, false, false).isEmpty()) {
			assertEquals(nrOfHoldingBeans, HoldingBean.destroyCount);
		}
	}

	/**
	 * When using a BeanFactory. singletons are of course not pre-instantiated.
	 * So rubbish class names in bean defs must now not be 'resolved' when the
	 * bean def is being parsed, 'cos everything on a bean def is now lazy, but
	 * must rather only be picked up when the bean is instantiated.
	 */
	public @Test void testClassNotFoundWithDefaultBeanClassLoader() {
		BeanFactory factory = new XmlBeanFactory(CLASS_NOT_FOUND_CONTEXT);
		// cool, no errors, so the rubbish class name in the bean def was not resolved
		try {
			// let's resolve the bean definition; must blow up
			factory.getBean("classNotFound");
			fail("Must have thrown a CannotLoadBeanClassException");
		}
		catch (CannotLoadBeanClassException ex) {
			assertTrue(ex.getResourceDescription().indexOf("classNotFound.xml") != -1);
			assertTrue(ex.getCause() instanceof ClassNotFoundException);
		}
	}

	public @Test void testClassNotFoundWithNoBeanClassLoader() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(bf);
		reader.setBeanClassLoader(null);
		reader.loadBeanDefinitions(CLASS_NOT_FOUND_CONTEXT);
		assertEquals("WhatALotOfRubbish", bf.getBeanDefinition("classNotFound").getBeanClassName());
	}

	public @Test void testResourceAndInputStream() throws IOException {
		XmlBeanFactory xbf = new XmlBeanFactory(RESOURCE_CONTEXT);
		// comes from "resourceImport.xml"
		ResourceTestBean resource1 = (ResourceTestBean) xbf.getBean("resource1");
		// comes from "resource.xml"
		ResourceTestBean resource2 = (ResourceTestBean) xbf.getBean("resource2");

		assertTrue(resource1.getResource() instanceof ClassPathResource);
		StringWriter writer = new StringWriter();
		FileCopyUtils.copy(new InputStreamReader(resource1.getResource().getInputStream()), writer);
		assertEquals("test", writer.toString());
		writer = new StringWriter();
		FileCopyUtils.copy(new InputStreamReader(resource1.getInputStream()), writer);
		assertEquals("test", writer.toString());
		writer = new StringWriter();
		FileCopyUtils.copy(new InputStreamReader(resource2.getResource().getInputStream()), writer);
		assertEquals("test", writer.toString());
		writer = new StringWriter();
		FileCopyUtils.copy(new InputStreamReader(resource2.getInputStream()), writer);
		assertEquals("test", writer.toString());
	}

	public @Test void testClassPathResourceWithImport() {
		XmlBeanFactory xbf = new XmlBeanFactory(RESOURCE_CONTEXT);
		// comes from "resourceImport.xml"
		xbf.getBean("resource1", ResourceTestBean.class);
		// comes from "resource.xml"
		xbf.getBean("resource2", ResourceTestBean.class);
	}

	public @Test void testUrlResourceWithImport() {
		URL url = getClass().getResource(RESOURCE_CONTEXT.getPath());
		XmlBeanFactory xbf = new XmlBeanFactory(new UrlResource(url));
		// comes from "resourceImport.xml"
		xbf.getBean("resource1", ResourceTestBean.class);
		// comes from "resource.xml"
		xbf.getBean("resource2", ResourceTestBean.class);
	}

	public @Test void testFileSystemResourceWithImport() throws URISyntaxException {
		String file = getClass().getResource(RESOURCE_CONTEXT.getPath()).toURI().getPath();
		XmlBeanFactory xbf = new XmlBeanFactory(new FileSystemResource(file));
		// comes from "resourceImport.xml"
		xbf.getBean("resource1", ResourceTestBean.class);
		// comes from "resource.xml"
		xbf.getBean("resource2", ResourceTestBean.class);
	}

	public @Test void testRecursiveImport() {
		try {
			new XmlBeanFactory(RECURSIVE_IMPORT_CONTEXT);
			fail("Should have thrown BeanDefinitionStoreException");
		}
		catch (BeanDefinitionStoreException ex) {
			// expected
			ex.printStackTrace();
		}
	}


	public @Test void testLookupOverrideMethodsWithSetterInjection() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(OVERRIDES_CONTEXT);

		testLookupOverrideMethodsWithSetterInjection(xbf, "overrideOneMethod", true);
		// Should work identically on subclass definition, in which lookup
		// methods are inherited
		testLookupOverrideMethodsWithSetterInjection(xbf, "overrideInheritedMethod", true);

		// Check cost of repeated construction of beans with method overrides
		// Will pick up misuse of CGLIB
		int howMany = 100;
		StopWatch sw = new StopWatch();
		sw.start("Look up " + howMany + " prototype bean instances with method overrides");
		for (int i = 0; i < howMany; i++) {
			testLookupOverrideMethodsWithSetterInjection(xbf, "overrideOnPrototype", false);
		}
		sw.stop();
		System.out.println(sw);
		if (!LogFactory.getLog(DefaultListableBeanFactory.class).isDebugEnabled()) {
			assertTrue(sw.getTotalTimeMillis() < 2000);
		}

		// Now test distinct bean with swapped value in factory, to ensure the two are independent
		OverrideOneMethod swappedOom = (OverrideOneMethod) xbf.getBean("overrideOneMethodSwappedReturnValues");

		TestBean tb = swappedOom.getPrototypeDependency();
		assertEquals("David", tb.getName());
		tb = swappedOom.protectedOverrideSingleton();
		assertEquals("Jenny", tb.getName());
	}

	private void testLookupOverrideMethodsWithSetterInjection(BeanFactory xbf, String beanName, boolean singleton) {
		OverrideOneMethod oom = (OverrideOneMethod) xbf.getBean(beanName);

		if (singleton) {
			assertSame(oom, xbf.getBean(beanName));
		}
		else {
			assertNotSame(oom, xbf.getBean(beanName));
		}

		TestBean jenny1 = oom.getPrototypeDependency();
		assertEquals("Jenny", jenny1.getName());
		TestBean jenny2 = oom.getPrototypeDependency();
		assertEquals("Jenny", jenny2.getName());
		assertNotSame(jenny1, jenny2);

		// Check that the bean can invoke the overridden method on itself
		// This differs from Spring's AOP support, which has a distinct notion
		// of a "target" object, meaning that the target needs explicit knowledge
		// of AOP proxying to invoke an advised method on itself.
		TestBean jenny3 = oom.invokesOverridenMethodOnSelf();
		assertEquals("Jenny", jenny3.getName());
		assertNotSame(jenny1, jenny3);

		// Now try protected method, and singleton
		TestBean dave1 = oom.protectedOverrideSingleton();
		assertEquals("David", dave1.getName());
		TestBean dave2 = oom.protectedOverrideSingleton();
		assertEquals("David", dave2.getName());
		assertSame(dave1, dave2);
	}

	public @Test void testReplaceMethodOverrideWithSetterInjection() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(DELEGATION_OVERRIDES_CONTEXT);

		OverrideOneMethod oom = (OverrideOneMethod) xbf.getBean("overrideOneMethod");

		// Same contract as for overrides.xml
		TestBean jenny1 = oom.getPrototypeDependency();
		assertEquals("Jenny", jenny1.getName());
		TestBean jenny2 = oom.getPrototypeDependency();
		assertEquals("Jenny", jenny2.getName());
		assertNotSame(jenny1, jenny2);

		TestBean notJenny = oom.getPrototypeDependency("someParam");
		assertTrue(!"Jenny".equals(notJenny.getName()));

		// Now try protected method, and singleton
		TestBean dave1 = oom.protectedOverrideSingleton();
		assertEquals("David", dave1.getName());
		TestBean dave2 = oom.protectedOverrideSingleton();
		assertEquals("David", dave2.getName());
		assertSame(dave1, dave2);

		// Check unadvised behaviour
		String str = "woierowijeiowiej";
		assertEquals(str, oom.echo(str));

		// Now test replace
		String s = "this is not a palindrome";
		String reverse = new StringBuffer(s).reverse().toString();
		assertEquals("Should have overridden to reverse, not echo", reverse, oom.replaceMe(s));

		assertEquals("Should have overridden no-arg overloaded replaceMe method to return fixed value",
				FixedMethodReplacer.VALUE, oom.replaceMe());

		OverrideOneMethodSubclass ooms = (OverrideOneMethodSubclass) xbf.getBean("replaceVoidMethod");
		DoSomethingReplacer dos = (DoSomethingReplacer) xbf.getBean("doSomethingReplacer");
		assertEquals(null, dos.lastArg);
		String s1 = "";
		String s2 = "foo bar black sheep";
		ooms.doSomething(s1);
		assertEquals(s1, dos.lastArg);
		ooms.doSomething(s2);
		assertEquals(s2, dos.lastArg);
	}

	public @Test void testLookupOverrideOneMethodWithConstructorInjection() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(CONSTRUCTOR_OVERRIDES_CONTEXT);

		ConstructorInjectedOverrides cio = (ConstructorInjectedOverrides) xbf.getBean("constructorOverrides");

		// Check that the setter was invoked...
		// We should be able to combine Constructor and
		// Setter Injection
		assertEquals("Setter string was set", "from property element", cio.getSetterString());

		// Jenny is a singleton
		TestBean jenny = (TestBean) xbf.getBean("jenny");
		assertSame(jenny, cio.getTestBean());
		assertSame(jenny, cio.getTestBean());
		FactoryMethods fm1 = cio.createFactoryMethods();
		FactoryMethods fm2 = cio.createFactoryMethods();
		assertNotSame("FactoryMethods reference is to a prototype", fm1, fm2);
		assertSame("The two prototypes hold the same singleton reference",
				fm1.getTestBean(), fm2.getTestBean());
	}

	public @Test void testRejectsOverrideOfBogusMethodName() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		try {
			reader.loadBeanDefinitions(INVALID_NO_SUCH_METHOD_CONTEXT);
			xbf.getBean("constructorOverrides");
			fail("Shouldn't allow override of bogus method");
		}
		catch (BeanDefinitionStoreException ex) {
			// Check that the bogus method name was included in the error message
			assertTrue("Bogus method name correctly reported", ex.getMessage().indexOf("bogusMethod") != -1);
		}
	}

	/**
	 * Assert the presence of this bug until we resolve it.
	 */
	public @Test void testSerializabilityOfMethodReplacer() throws Exception {
		try {
			BUGtestSerializableMethodReplacerAndSuperclass();
			fail();
		}
		catch (AssertionError ex) {
			System.err.println("****** SPR-356: Objects with MethodReplace overrides are not serializable");
		}
	}

	public void BUGtestSerializableMethodReplacerAndSuperclass() throws IOException, ClassNotFoundException {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(DELEGATION_OVERRIDES_CONTEXT);
		SerializableMethodReplacerCandidate s = (SerializableMethodReplacerCandidate) xbf.getBean("serializableReplacer");
		String forwards = "this is forwards";
		String backwards = new StringBuffer(forwards).reverse().toString();
		assertEquals(backwards, s.replaceMe(forwards));
		assertTrue(SerializationTestUtils.isSerializable(s));
		s = (SerializableMethodReplacerCandidate) SerializationTestUtils.serializeAndDeserialize(s);
		assertEquals("Method replace still works after serialization and deserialization", backwards, s.replaceMe(forwards));
	}

	public @Test void testInnerBeanInheritsScopeFromConcreteChildDefinition() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(OVERRIDES_CONTEXT);
		TestBean jenny = (TestBean) xbf.getBean("jennyChild");
		assertEquals(1, jenny.getFriends().size());
		assertTrue(jenny.getFriends().iterator().next() instanceof TestBean);
	}

	public @Test void testConstructorArgWithSingleSimpleTypeMatch() {
		XmlBeanFactory xbf = new XmlBeanFactory(CONSTRUCTOR_ARG_CONTEXT);

		SingleSimpleTypeConstructorBean bean = (SingleSimpleTypeConstructorBean) xbf.getBean("beanWithBoolean");
		assertTrue(bean.isSingleBoolean());

		SingleSimpleTypeConstructorBean bean2 = (SingleSimpleTypeConstructorBean) xbf.getBean("beanWithBoolean2");
		assertTrue(bean2.isSingleBoolean());
	}

	public @Test void testConstructorArgWithDoubleSimpleTypeMatch() {
		XmlBeanFactory xbf = new XmlBeanFactory(CONSTRUCTOR_ARG_CONTEXT);

		SingleSimpleTypeConstructorBean bean = (SingleSimpleTypeConstructorBean) xbf.getBean("beanWithBooleanAndString");
		assertTrue(bean.isSecondBoolean());
		assertEquals("A String", bean.getTestString());

		SingleSimpleTypeConstructorBean bean2 = (SingleSimpleTypeConstructorBean) xbf.getBean("beanWithBooleanAndString2");
		assertTrue(bean2.isSecondBoolean());
		assertEquals("A String", bean2.getTestString());
	}

	public @Test void testDoubleBooleanAutowire() {
		XmlBeanFactory xbf = new XmlBeanFactory(CONSTRUCTOR_ARG_CONTEXT);
		DoubleBooleanConstructorBean bean = (DoubleBooleanConstructorBean) xbf.getBean("beanWithDoubleBoolean");
		assertEquals(Boolean.TRUE, bean.boolean1);
		assertEquals(Boolean.FALSE, bean.boolean2);
	}

	public @Test void testDoubleBooleanAutowireWithIndex() {
		XmlBeanFactory xbf = new XmlBeanFactory(CONSTRUCTOR_ARG_CONTEXT);
		DoubleBooleanConstructorBean bean = (DoubleBooleanConstructorBean) xbf.getBean("beanWithDoubleBooleanAndIndex");
		assertEquals(Boolean.FALSE, bean.boolean1);
		assertEquals(Boolean.TRUE, bean.boolean2);
	}

	public @Test void testLenientDependencyMatching() {
		XmlBeanFactory xbf = new XmlBeanFactory(CONSTRUCTOR_ARG_CONTEXT);
		LenientDependencyTestBean bean = (LenientDependencyTestBean) xbf.getBean("lenientDependencyTestBean");
		assertTrue(bean.tb instanceof DerivedTestBean);
	}

	public @Test void testLenientDependencyMatchingFactoryMethod() {
		XmlBeanFactory xbf = new XmlBeanFactory(CONSTRUCTOR_ARG_CONTEXT);
		LenientDependencyTestBean bean = (LenientDependencyTestBean) xbf.getBean("lenientDependencyTestBeanFactoryMethod");
		assertTrue(bean.tb instanceof DerivedTestBean);
	}

	public @Test void testNonLenientDependencyMatching() {
		XmlBeanFactory xbf = new XmlBeanFactory(CONSTRUCTOR_ARG_CONTEXT);
		AbstractBeanDefinition bd = (AbstractBeanDefinition) xbf.getBeanDefinition("lenientDependencyTestBean");
		bd.setLenientConstructorResolution(false);
		try {
			xbf.getBean("lenientDependencyTestBean");
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			// expected
			ex.printStackTrace();
			assertTrue(ex.getMostSpecificCause().getMessage().contains("Ambiguous"));
		}
	}

	public @Test void testNonLenientDependencyMatchingFactoryMethod() {
		XmlBeanFactory xbf = new XmlBeanFactory(CONSTRUCTOR_ARG_CONTEXT);
		AbstractBeanDefinition bd = (AbstractBeanDefinition) xbf.getBeanDefinition("lenientDependencyTestBeanFactoryMethod");
		bd.setLenientConstructorResolution(false);
		try {
			xbf.getBean("lenientDependencyTestBeanFactoryMethod");
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			// expected
			ex.printStackTrace();
			assertTrue(ex.getMostSpecificCause().getMessage().contains("Ambiguous"));
		}
	}

	public @Test void testJavaLangStringConstructor() {
		XmlBeanFactory xbf = new XmlBeanFactory(CONSTRUCTOR_ARG_CONTEXT);
		AbstractBeanDefinition bd = (AbstractBeanDefinition) xbf.getBeanDefinition("string");
		bd.setLenientConstructorResolution(false);
		String str = (String) xbf.getBean("string");
		assertEquals("test", str);
	}

	public @Test void testCustomStringConstructor() {
		XmlBeanFactory xbf = new XmlBeanFactory(CONSTRUCTOR_ARG_CONTEXT);
		AbstractBeanDefinition bd = (AbstractBeanDefinition) xbf.getBeanDefinition("stringConstructor");
		bd.setLenientConstructorResolution(false);
		StringConstructorTestBean tb = (StringConstructorTestBean) xbf.getBean("stringConstructor");
		assertEquals("test", tb.name);
	}

	public @Test void testPrimitiveConstructorArray() {
		XmlBeanFactory xbf = new XmlBeanFactory(CONSTRUCTOR_ARG_CONTEXT);
		ConstructorArrayTestBean bean = (ConstructorArrayTestBean) xbf.getBean("constructorArray");
		assertTrue(bean.array instanceof int[]);
		assertEquals(1, ((int[]) bean.array).length);
		assertEquals(1, ((int[]) bean.array)[0]);
	}

	public @Test void testIndexedPrimitiveConstructorArray() {
		XmlBeanFactory xbf = new XmlBeanFactory(CONSTRUCTOR_ARG_CONTEXT);
		ConstructorArrayTestBean bean = (ConstructorArrayTestBean) xbf.getBean("indexedConstructorArray");
		assertTrue(bean.array instanceof int[]);
		assertEquals(1, ((int[]) bean.array).length);
		assertEquals(1, ((int[]) bean.array)[0]);
	}

	public @Test void testStringConstructorArrayNoType() {
		XmlBeanFactory xbf = new XmlBeanFactory(CONSTRUCTOR_ARG_CONTEXT);
		ConstructorArrayTestBean bean = (ConstructorArrayTestBean) xbf.getBean("constructorArrayNoType");
		assertTrue(bean.array instanceof String[]);
		assertEquals(0, ((String[]) bean.array).length);
	}

	public @Test void testStringConstructorArrayNoTypeNonLenient() {
		XmlBeanFactory xbf = new XmlBeanFactory(CONSTRUCTOR_ARG_CONTEXT);
		AbstractBeanDefinition bd = (AbstractBeanDefinition) xbf.getBeanDefinition("constructorArrayNoType");
		bd.setLenientConstructorResolution(false);
		ConstructorArrayTestBean bean = (ConstructorArrayTestBean) xbf.getBean("constructorArrayNoType");
		assertTrue(bean.array instanceof String[]);
		assertEquals(0, ((String[]) bean.array).length);
	}

	public @Test void testWithDuplicateName() throws Exception {
		try {
			new XmlBeanFactory(TEST_WITH_DUP_NAMES_CONTEXT);
			fail("Duplicate name not detected");
		}
		catch (BeansException ex) {
			assertTrue(ex.getMessage().contains("Bean name 'foo'"));
		}
	}

	public @Test void testWithDuplicateNameInAlias() throws Exception {
		try {
			new XmlBeanFactory(TEST_WITH_DUP_NAME_IN_ALIAS_CONTEXT);
			fail("Duplicate name not detected");
		}
		catch (BeansException e) {
			assertTrue(e.getMessage().contains("Bean name 'foo'"));
		}
	}

	public @Test void testOverrideMethodByArgTypeAttribute() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(DELEGATION_OVERRIDES_CONTEXT);
		OverrideOneMethod oom = (OverrideOneMethod) xbf.getBean("overrideOneMethodByAttribute");
		assertEquals("should not replace", "replaceMe:1", oom.replaceMe(1));
		assertEquals("should replace", "cba", oom.replaceMe("abc"));
	}

	public @Test void testOverrideMethodByArgTypeElement() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(DELEGATION_OVERRIDES_CONTEXT);
		OverrideOneMethod oom = (OverrideOneMethod) xbf.getBean("overrideOneMethodByElement");
		assertEquals("should not replace", "replaceMe:1", oom.replaceMe(1));
		assertEquals("should replace", "cba", oom.replaceMe("abc"));
	}

	public static class DoSomethingReplacer implements MethodReplacer {

		public Object lastArg;

		public Object reimplement(Object obj, Method method, Object[] args) throws Throwable {
			assertEquals(1, args.length);
			assertEquals("doSomething", method.getName());
			lastArg = args[0];
			return null;
		}
	}


	public static class BadInitializer {

		/** Init method */
		public void init2() throws IOException {
			throw new IOException();
		}
	}


	public static class DoubleInitializer {

		private int num;

		public int getNum() {
			return num;
		}

		public void setNum(int i) {
			num = i;
		}

		/** Init method */
		public void init() {
			this.num *= 2;
		}
	}


	public static class InitAndIB implements InitializingBean, DisposableBean {

		public static boolean constructed;

		public boolean afterPropertiesSetInvoked, initMethodInvoked, destroyed, customDestroyed;

		public InitAndIB() {
			constructed = true;
		}

		public void afterPropertiesSet() {
			if (this.initMethodInvoked) {
				fail();
			}
			if (this.afterPropertiesSetInvoked) {
				throw new IllegalStateException("Already initialized");
			}
			this.afterPropertiesSetInvoked = true;
		}

		/** Init method */
		public void customInit() throws IOException {
			if (!this.afterPropertiesSetInvoked) {
				fail();
			}
			if (this.initMethodInvoked) {
				throw new IllegalStateException("Already customInitialized");
			}
			this.initMethodInvoked = true;
		}

		public void destroy() {
			if (this.customDestroyed) {
				fail();
			}
			if (this.destroyed) {
				throw new IllegalStateException("Already destroyed");
			}
			this.destroyed = true;
		}

		public void customDestroy() {
			if (!this.destroyed) {
				fail();
			}
			if (this.customDestroyed) {
				throw new IllegalStateException("Already customDestroyed");
			}
			this.customDestroyed = true;
		}
	}


	public static class PreparingBean1 implements DisposableBean {

		public static boolean prepared = false;

		public static boolean destroyed = false;

		public PreparingBean1() {
			prepared = true;
		}

		public void destroy() {
			destroyed = true;
		}
	}


	public static class PreparingBean2 implements DisposableBean {

		public static boolean prepared = false;

		public static boolean destroyed = false;

		public PreparingBean2() {
			prepared = true;
		}

		public void destroy() {
			destroyed = true;
		}
	}


	public static class DependingBean implements InitializingBean, DisposableBean {

		public static int destroyCount = 0;

		public boolean destroyed = false;

		public DependingBean() {
		}

		public DependingBean(PreparingBean1 bean1, PreparingBean2 bean2) {
		}

		public void setBean1(PreparingBean1 bean1) {
		}

		public void setBean2(PreparingBean2 bean2) {
		}

		public void setInTheMiddleBean(InTheMiddleBean bean) {
		}

		public void afterPropertiesSet() {
			if (!(PreparingBean1.prepared && PreparingBean2.prepared)) {
				throw new IllegalStateException("Need prepared PreparingBeans!");
			}
		}

		public void destroy() {
			if (PreparingBean1.destroyed || PreparingBean2.destroyed) {
				throw new IllegalStateException("Should not be destroyed after PreparingBeans");
			}
			destroyed = true;
			destroyCount++;
		}
	}


	public static class InTheMiddleBean {

		public void setBean1(PreparingBean1 bean1) {
		}

		public void setBean2(PreparingBean2 bean2) {
		}
	}


	public static class HoldingBean implements DisposableBean {

		public static int destroyCount = 0;

		private DependingBean dependingBean;

		public boolean destroyed = false;

		public void setDependingBean(DependingBean dependingBean) {
			this.dependingBean = dependingBean;
		}

		public void destroy() {
			if (this.dependingBean.destroyed) {
				throw new IllegalStateException("Should not be destroyed after DependingBean");
			}
			this.destroyed = true;
			destroyCount++;
		}
	}


	public static class DoubleBooleanConstructorBean {

		private Boolean boolean1;
		private Boolean boolean2;

		public DoubleBooleanConstructorBean(Boolean b1, Boolean b2) {
			this.boolean1 = b1;
			this.boolean2 = b2;
		}

		public DoubleBooleanConstructorBean(String s1, String s2) {
			throw new IllegalStateException("Don't pick this constructor");
		}

		public static DoubleBooleanConstructorBean create(Boolean b1, Boolean b2) {
			return new DoubleBooleanConstructorBean(b1, b2);
		}

		public static DoubleBooleanConstructorBean create(String s1, String s2) {
			return new DoubleBooleanConstructorBean(s1, s2);
		}
	}


	public static class LenientDependencyTestBean {

		public final ITestBean tb;

		public LenientDependencyTestBean(ITestBean tb) {
			this.tb = tb;
		}

		public LenientDependencyTestBean(TestBean tb) {
			this.tb = tb;
		}

		public LenientDependencyTestBean(DerivedTestBean tb) {
			this.tb = tb;
		}

		public LenientDependencyTestBean(Map<Object, Object>[] m) {
			throw new IllegalStateException("Don't pick this constructor");
		}

		public static LenientDependencyTestBean create(ITestBean tb) {
			return new LenientDependencyTestBean(tb);
		}

		public static LenientDependencyTestBean create(TestBean tb) {
			return new LenientDependencyTestBean(tb);
		}

		public static LenientDependencyTestBean create(DerivedTestBean tb) {
			return new LenientDependencyTestBean(tb);
		}
	}


	public static class ConstructorArrayTestBean {

		public final Object array;

		public ConstructorArrayTestBean(int[] array) {
			this.array = array;
		}

		public ConstructorArrayTestBean(float[] array) {
			this.array = array;
		}

		public ConstructorArrayTestBean(short[] array) {
			this.array = array;
		}

		public ConstructorArrayTestBean(String[] array) {
			this.array = array;
		}
	}


	public static class StringConstructorTestBean {

		public final String name;

		public StringConstructorTestBean(String name) {
			this.name = name;
		}
	}


	public static class WrappingPostProcessor implements BeanPostProcessor {

		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
			return bean;
		}

		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			ProxyFactory pf = new ProxyFactory(bean);
			return pf.getProxy();
		}
	}

}
