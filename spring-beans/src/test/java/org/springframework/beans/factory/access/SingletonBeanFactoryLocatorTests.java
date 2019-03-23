/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory.access;

import java.util.List;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.ClassUtils;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link SingletonBeanFactoryLocator}.
 *
 * @author Colin Sampaleanu
 * @author Chris Beams
 */
public class SingletonBeanFactoryLocatorTests {
	private static final Class<?> CLASS = SingletonBeanFactoryLocatorTests.class;
	private static final String REF1_XML = CLASS.getSimpleName() + "-ref1.xml";

	@Test
	public void testBasicFunctionality() {
		SingletonBeanFactoryLocator facLoc = new SingletonBeanFactoryLocator(
				"classpath*:" + ClassUtils.addResourcePathToPackagePath(CLASS, REF1_XML));

		basicFunctionalityTest(facLoc);
	}

	/**
	 * Worker method so subclass can use it too.
	 */
	protected void basicFunctionalityTest(SingletonBeanFactoryLocator facLoc) {
		BeanFactoryReference bfr = facLoc.useBeanFactory("a.qualified.name.of.some.sort");
		BeanFactory fac = bfr.getFactory();
		BeanFactoryReference bfr2 = facLoc.useBeanFactory("another.qualified.name");
		fac = bfr2.getFactory();
		// verify that the same instance is returned
		TestBean tb = (TestBean) fac.getBean("beans1.bean1");
		assertTrue(tb.getName().equals("beans1.bean1"));
		tb.setName("was beans1.bean1");
		BeanFactoryReference bfr3 = facLoc.useBeanFactory("another.qualified.name");
		fac = bfr3.getFactory();
		tb = (TestBean) fac.getBean("beans1.bean1");
		assertTrue(tb.getName().equals("was beans1.bean1"));
		BeanFactoryReference bfr4 = facLoc.useBeanFactory("a.qualified.name.which.is.an.alias");
		fac = bfr4.getFactory();
		tb = (TestBean) fac.getBean("beans1.bean1");
		assertTrue(tb.getName().equals("was beans1.bean1"));
		// Now verify that we can call release in any order.
		// Unfortunately this doesn't validate complete release after the last one.
		bfr2.release();
		bfr3.release();
		bfr.release();
		bfr4.release();
	}

	/**
	 * This test can run multiple times, but due to static keyed lookup of the locators,
	 * 2nd and subsequent calls will actuall get back same locator instance. This is not
	 * an issue really, since the contained beanfactories will still be loaded and released.
	 */
	@Test
	public void testGetInstance() {
		// Try with and without 'classpath*:' prefix, and with 'classpath:' prefix.
		BeanFactoryLocator facLoc = SingletonBeanFactoryLocator.getInstance(
				ClassUtils.addResourcePathToPackagePath(CLASS, REF1_XML));
		getInstanceTest1(facLoc);

		facLoc = SingletonBeanFactoryLocator.getInstance(
				"classpath*:/" + ClassUtils.addResourcePathToPackagePath(CLASS, REF1_XML));
		getInstanceTest2(facLoc);

		// This will actually get another locator instance, as the key is the resource name.
		facLoc = SingletonBeanFactoryLocator.getInstance(
				"classpath:" + ClassUtils.addResourcePathToPackagePath(CLASS, REF1_XML));
		getInstanceTest3(facLoc);

	}

	/**
	 * Worker method so subclass can use it too
	 */
	protected void getInstanceTest1(BeanFactoryLocator facLoc) {
		BeanFactoryReference bfr = facLoc.useBeanFactory("a.qualified.name.of.some.sort");
		BeanFactory fac = bfr.getFactory();
		BeanFactoryReference bfr2 = facLoc.useBeanFactory("another.qualified.name");
		fac = bfr2.getFactory();
		// verify that the same instance is returned
		TestBean tb = (TestBean) fac.getBean("beans1.bean1");
		assertTrue(tb.getName().equals("beans1.bean1"));
		tb.setName("was beans1.bean1");
		BeanFactoryReference bfr3 = facLoc.useBeanFactory("another.qualified.name");
		fac = bfr3.getFactory();
		tb = (TestBean) fac.getBean("beans1.bean1");
		assertTrue(tb.getName().equals("was beans1.bean1"));

		BeanFactoryReference bfr4 = facLoc.useBeanFactory("a.qualified.name.which.is.an.alias");
		fac = bfr4.getFactory();
		tb = (TestBean) fac.getBean("beans1.bean1");
		assertTrue(tb.getName().equals("was beans1.bean1"));

		bfr.release();
		bfr3.release();
		bfr2.release();
		bfr4.release();
	}

	/**
	 * Worker method so subclass can use it too
	 */
	protected void getInstanceTest2(BeanFactoryLocator facLoc) {
		BeanFactoryReference bfr;
		BeanFactory fac;
		BeanFactoryReference bfr2;
		TestBean tb;
		BeanFactoryReference bfr3;
		BeanFactoryReference bfr4;
		bfr = facLoc.useBeanFactory("a.qualified.name.of.some.sort");
		fac = bfr.getFactory();
		bfr2 = facLoc.useBeanFactory("another.qualified.name");
		fac = bfr2.getFactory();
		// verify that the same instance is returned
		tb = (TestBean) fac.getBean("beans1.bean1");
		assertTrue(tb.getName().equals("beans1.bean1"));
		tb.setName("was beans1.bean1");
		bfr3 = facLoc.useBeanFactory("another.qualified.name");
		fac = bfr3.getFactory();
		tb = (TestBean) fac.getBean("beans1.bean1");
		assertTrue(tb.getName().equals("was beans1.bean1"));
		bfr4 = facLoc.useBeanFactory("a.qualified.name.which.is.an.alias");
		fac = bfr4.getFactory();
		tb = (TestBean) fac.getBean("beans1.bean1");
		assertTrue(tb.getName().equals("was beans1.bean1"));
		bfr.release();
		bfr2.release();
		bfr4.release();
		bfr3.release();
	}

	/**
	 * Worker method so subclass can use it too
	 */
	protected void getInstanceTest3(BeanFactoryLocator facLoc) {
		BeanFactoryReference bfr;
		BeanFactory fac;
		BeanFactoryReference bfr2;
		TestBean tb;
		BeanFactoryReference bfr3;
		BeanFactoryReference bfr4;
		bfr = facLoc.useBeanFactory("a.qualified.name.of.some.sort");
		fac = bfr.getFactory();
		bfr2 = facLoc.useBeanFactory("another.qualified.name");
		fac = bfr2.getFactory();
		// verify that the same instance is returned
		tb = (TestBean) fac.getBean("beans1.bean1");
		assertTrue(tb.getName().equals("beans1.bean1"));
		tb.setName("was beans1.bean1");
		bfr3 = facLoc.useBeanFactory("another.qualified.name");
		fac = bfr3.getFactory();
		tb = (TestBean) fac.getBean("beans1.bean1");
		assertTrue(tb.getName().equals("was beans1.bean1"));
		bfr4 = facLoc.useBeanFactory("a.qualified.name.which.is.an.alias");
		fac = bfr4.getFactory();
		tb = (TestBean) fac.getBean("beans1.bean1");
		assertTrue(tb.getName().equals("was beans1.bean1"));
		bfr4.release();
		bfr3.release();
		bfr2.release();
		bfr.release();
	}

}


class TestBean {

	private String name;

	private List<?> list;

	private Object objRef;

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return Returns the list.
	 */
	public List<?> getList() {
		return list;
	}

	/**
	 * @param list The list to set.
	 */
	public void setList(List<?> list) {
		this.list = list;
	}

	/**
	 * @return Returns the object.
	 */
	public Object getObjRef() {
		return objRef;
	}

	/**
	 * @param object The object to set.
	 */
	public void setObjRef(Object object) {
		this.objRef = object;
	}
}
