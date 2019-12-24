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

package org.springframework.tests.sample.beans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.util.ObjectUtils;

/**
 * Simple immutable test bean used for testing bean factories, the AOP framework etc.
 * @author Fabio Borriello
 * @since 04 December 2019
 */
public class ImmutableTestBean implements BeanNameAware, BeanFactoryAware, ITestBean, IOther, Comparable<Object> {

	private final String beanName;

	private final String country;

	private final BeanFactory beanFactory;

	private final boolean postProcessed;

	private final String name;

	private final String sex;

	private final int age;

	private final boolean jedi;

	private final ITestBean spouse;

	private final String touchy;

	private final String[] stringArray;

	private final Integer[] someIntegerArray;

	private final Integer[][] nestedIntegerArray;

	private final int[] someIntArray;

	private final int[][] nestedIntArray;

	private final Date date = new Date();

	private final Float myFloat = (float) 0.0;

	private final Collection<? super Object> friends = new LinkedList<>();

	private final Set<?> someSet = new HashSet<>();

	private final Map<?, ?> someMap = new HashMap<>();

	private final List<?> someList = new ArrayList<>();

	private final Properties someProperties = new Properties();

	private final INestedTestBean doctor = new NestedTestBean();

	private final INestedTestBean lawyer = new NestedTestBean();

	private final IndexedTestBean nestedIndexedBean;

	private final boolean destroyed;

	private final Number someNumber;

	private final Colour favouriteColour;

	private final Boolean someBoolean;

	private final List<?> otherColours;

	private final List<?> pets;

	public ImmutableTestBean(final String beanName, final String country, final BeanFactory beanFactory, final boolean postProcessed, final String name, final String sex, final int age, final boolean jedi, final ITestBean spouse, final String touchy, final String[] stringArray, final Integer[] someIntegerArray, final Integer[][] nestedIntegerArray, final int[] someIntArray, final int[][] nestedIntArray, final IndexedTestBean nestedIndexedBean, final boolean destroyed, final Number someNumber, final Colour favouriteColour, final Boolean someBoolean, final List<?> otherColours, final List<?> pets) {
		this.beanName = beanName;
		this.country = country;
		this.beanFactory = beanFactory;
		this.postProcessed = postProcessed;
		this.name = name;
		this.sex = sex;
		this.age = age;
		this.jedi = jedi;
		this.spouse = spouse;
		this.touchy = touchy;
		this.stringArray = stringArray;
		this.someIntegerArray = someIntegerArray;
		this.nestedIntegerArray = nestedIntegerArray;
		this.someIntArray = someIntArray;
		this.nestedIntArray = nestedIntArray;
		this.nestedIndexedBean = nestedIndexedBean;
		this.destroyed = destroyed;
		this.someNumber = someNumber;
		this.favouriteColour = favouriteColour;
		this.someBoolean = someBoolean;
		this.otherColours = otherColours;
		this.pets = pets;
	}

	public String getBeanName() {
		return beanName;
	}

	public BeanFactory getBeanFactory() {
		return beanFactory;
	}

	public boolean isPostProcessed() {
		return postProcessed;
	}

	public String getName() {
		return name;
	}

	@Override
	public void setName(final String name) {

	}

	public String getSex() {
		return sex;
	}

	public int getAge() {
		return age;
	}

	@Override
	public void setAge(final int age) {

	}

	public boolean isJedi() {
		return jedi;
	}

	public ITestBean getSpouse() {
		return this.spouse;
	}

	@Override
	public void setSpouse(final ITestBean spouse) {

	}

	@Override
	public ITestBean[] getSpouses() {
		return (spouse != null ? new ITestBean[] {spouse} : null);
	}

	public String getTouchy() {
		return touchy;
	}

	public String getCountry() {
		return country;
	}

	public String[] getStringArray() {
		return stringArray;
	}

	@Override
	public void setStringArray(final String[] stringArray) {

	}

	public Integer[] getSomeIntegerArray() {
		return someIntegerArray;
	}

	@Override
	public void setSomeIntegerArray(final Integer[] someIntegerArray) {

	}

	@Override
	public void setNestedIntegerArray(final Integer[][] nestedIntegerArray) {

	}

	public Integer[][] getNestedIntegerArray() {
		return nestedIntegerArray;
	}

	public int[] getSomeIntArray() {
		return someIntArray;
	}

	@Override
	public void setSomeIntArray(final int[] someIntArray) {

	}

	public int[][] getNestedIntArray() {
		return nestedIntArray;
	}

	@Override
	public void setNestedIntArray(final int[][] someNestedArray) {

	}

	public Date getDate() {
		return date;
	}

	public Float getMyFloat() {
		return myFloat;
	}

	public Collection<? super Object> getFriends() {
		return friends;
	}

	public Set<?> getSomeSet() {
		return someSet;
	}

	public Map<?, ?> getSomeMap() {
		return someMap;
	}

	public List<?> getSomeList() {
		return someList;
	}

	public Properties getSomeProperties() {
		return someProperties;
	}

	public INestedTestBean getDoctor() {
		return doctor;
	}

	public INestedTestBean getLawyer() {
		return lawyer;
	}

	public Number getSomeNumber() {
		return someNumber;
	}

	public Colour getFavouriteColour() {
		return favouriteColour;
	}

	public Boolean getSomeBoolean() {
		return someBoolean;
	}

	public IndexedTestBean getNestedIndexedBean() {
		return nestedIndexedBean;
	}

	@Override
	public int haveBirthday() {
		return 0;
	}

	public List<?> getOtherColours() {
		return otherColours;
	}

	public List<?> getPets() {
		return pets;
	}

	/**
	 * @see org.springframework.tests.sample.beans.ITestBean#exceptional(Throwable)
	 */
	@Override
	public void exceptional(Throwable t) throws Throwable {
		if (t != null) {
			throw t;
		}
	}

	@Override
	public void unreliableFileOperation() throws IOException {
		throw new IOException();
	}

	/**
	 * @see org.springframework.tests.sample.beans.ITestBean#returnsThis()
	 */
	@Override
	public Object returnsThis() {
		return this;
	}

	/**
	 * @see org.springframework.tests.sample.beans.IOther#absquatulate()
	 */
	@Override
	public void absquatulate() {
	}

	public boolean wasDestroyed() {
		return destroyed;
	}

	@Override
	public boolean equals(final Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TestBean)) {
			return false;
		}
		TestBean tb2 = (TestBean) other;
		return (ObjectUtils.nullSafeEquals(this.name, tb2.getName()) && this.age == tb2.getAge());
	}

	@Override
	public int hashCode() {
		return this.age;
	}

	@Override
	public int compareTo(final Object other) {
		if (this.name != null && other instanceof TestBean) {
			return this.name.compareTo(((TestBean) other).getName());
		} else {
			return 1;
		}
	}

	@Override
	public String toString() {
		return this.name;
	}

	@Override
	public void setBeanFactory(final BeanFactory beanFactory) throws BeansException {

	}

	@Override
	public void setBeanName(final String name) {

	}
}
