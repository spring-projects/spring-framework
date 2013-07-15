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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.util.ObjectUtils;

/**
 * Simple test bean used for testing bean factories, the AOP framework etc.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 15 April 2001
 */
public class TestBean implements BeanNameAware, BeanFactoryAware, ITestBean, IOther, Comparable<Object> {

	private String beanName;

	private String country;

	private BeanFactory beanFactory;

	private boolean postProcessed;

	private String name;

	private String sex;

	private int age;

	private boolean jedi;

	protected ITestBean[] spouses;

	private String touchy;

	private String[] stringArray;

	private Integer[] someIntegerArray;

	private Integer[][] nestedIntegerArray;

	private int[] someIntArray;

	private int[][] nestedIntArray;

	private Date date = new Date();

	private Float myFloat = new Float(0.0);

	private Collection<? super Object> friends = new LinkedList<>();

	private Set<?> someSet = new HashSet<>();

	private Map<?, ?> someMap = new HashMap<>();

	private List<?> someList = new ArrayList<>();

	private Properties someProperties = new Properties();

	private INestedTestBean doctor = new NestedTestBean();

	private INestedTestBean lawyer = new NestedTestBean();

	private IndexedTestBean nestedIndexedBean;

	private boolean destroyed;

	private Number someNumber;

	private Colour favouriteColour;

	private Boolean someBoolean;

	private List<?> otherColours;

	private List<?> pets;


	public TestBean() {
	}

	public TestBean(String name) {
		this.name = name;
	}

	public TestBean(ITestBean spouse) {
		this.spouses = new ITestBean[] {spouse};
	}

	public TestBean(String name, int age) {
		this.name = name;
		this.age = age;
	}

	public TestBean(ITestBean spouse, Properties someProperties) {
		this.spouses = new ITestBean[] {spouse};
		this.someProperties = someProperties;
	}

	public TestBean(List<?> someList) {
		this.someList = someList;
	}

	public TestBean(Set<?> someSet) {
		this.someSet = someSet;
	}

	public TestBean(Map<?, ?> someMap) {
		this.someMap = someMap;
	}

	public TestBean(Properties someProperties) {
		this.someProperties = someProperties;
	}


	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public String getBeanName() {
		return beanName;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	public BeanFactory getBeanFactory() {
		return beanFactory;
	}

	public void setPostProcessed(boolean postProcessed) {
		this.postProcessed = postProcessed;
	}

	public boolean isPostProcessed() {
		return postProcessed;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	public String getSex() {
		return sex;
	}

	public void setSex(String sex) {
		this.sex = sex;
		if (this.name == null) {
			this.name = sex;
		}
	}

	@Override
	public int getAge() {
		return age;
	}

	@Override
	public void setAge(int age) {
		this.age = age;
	}

	public boolean isJedi() {
		return jedi;
	}

	public void setJedi(boolean jedi) {
		this.jedi = jedi;
	}

	@Override
	public ITestBean getSpouse() {
		return (spouses != null ? spouses[0] : null);
	}

	@Override
	public void setSpouse(ITestBean spouse) {
		this.spouses = new ITestBean[] {spouse};
	}

	@Override
	public ITestBean[] getSpouses() {
		return spouses;
	}

	public String getTouchy() {
		return touchy;
	}

	public void setTouchy(String touchy) throws Exception {
		if (touchy.indexOf('.') != -1) {
			throw new Exception("Can't contain a .");
		}
		if (touchy.indexOf(',') != -1) {
			throw new NumberFormatException("Number format exception: contains a ,");
		}
		this.touchy = touchy;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	@Override
	public String[] getStringArray() {
		return stringArray;
	}

	@Override
	public void setStringArray(String[] stringArray) {
		this.stringArray = stringArray;
	}

	@Override
	public Integer[] getSomeIntegerArray() {
		return someIntegerArray;
	}

	@Override
	public void setSomeIntegerArray(Integer[] someIntegerArray) {
		this.someIntegerArray = someIntegerArray;
	}

	@Override
	public Integer[][] getNestedIntegerArray() {
		return nestedIntegerArray;
	}

	@Override
	public void setNestedIntegerArray(Integer[][] nestedIntegerArray) {
		this.nestedIntegerArray = nestedIntegerArray;
	}

	@Override
	public int[] getSomeIntArray() {
		return someIntArray;
	}

	@Override
	public void setSomeIntArray(int[] someIntArray) {
		this.someIntArray = someIntArray;
	}

	@Override
	public int[][] getNestedIntArray() {
		return nestedIntArray;
	}

	@Override
	public void setNestedIntArray(int[][] nestedIntArray) {
		this.nestedIntArray = nestedIntArray;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Float getMyFloat() {
		return myFloat;
	}

	public void setMyFloat(Float myFloat) {
		this.myFloat = myFloat;
	}

	public Collection<? super Object> getFriends() {
		return friends;
	}

	public void setFriends(Collection<? super Object> friends) {
		this.friends = friends;
	}

	public Set<?> getSomeSet() {
		return someSet;
	}

	public void setSomeSet(Set<?> someSet) {
		this.someSet = someSet;
	}

	public Map<?, ?> getSomeMap() {
		return someMap;
	}

	public void setSomeMap(Map<?, ?> someMap) {
		this.someMap = someMap;
	}

	public List<?> getSomeList() {
		return someList;
	}

	public void setSomeList(List<?> someList) {
		this.someList = someList;
	}

	public Properties getSomeProperties() {
		return someProperties;
	}

	public void setSomeProperties(Properties someProperties) {
		this.someProperties = someProperties;
	}

	@Override
	public INestedTestBean getDoctor() {
		return doctor;
	}

	public void setDoctor(INestedTestBean doctor) {
		this.doctor = doctor;
	}

	@Override
	public INestedTestBean getLawyer() {
		return lawyer;
	}

	public void setLawyer(INestedTestBean lawyer) {
		this.lawyer = lawyer;
	}

	public Number getSomeNumber() {
		return someNumber;
	}

	public void setSomeNumber(Number someNumber) {
		this.someNumber = someNumber;
	}

	public Colour getFavouriteColour() {
		return favouriteColour;
	}

	public void setFavouriteColour(Colour favouriteColour) {
		this.favouriteColour = favouriteColour;
	}

	public Boolean getSomeBoolean() {
		return someBoolean;
	}

	public void setSomeBoolean(Boolean someBoolean) {
		this.someBoolean = someBoolean;
	}

	@Override
	public IndexedTestBean getNestedIndexedBean() {
		return nestedIndexedBean;
	}

	public void setNestedIndexedBean(IndexedTestBean nestedIndexedBean) {
		this.nestedIndexedBean = nestedIndexedBean;
	}

	public List<?> getOtherColours() {
		return otherColours;
	}

	public void setOtherColours(List<?> otherColours) {
		this.otherColours = otherColours;
	}

	public List<?> getPets() {
		return pets;
	}

	public void setPets(List<?> pets) {
		this.pets = pets;
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

	@Override
	public int haveBirthday() {
		return age++;
	}


	public void destroy() {
		this.destroyed = true;
	}

	public boolean wasDestroyed() {
		return destroyed;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || !(other instanceof TestBean)) {
			return false;
		}
		TestBean tb2 = (TestBean) other;
		return (ObjectUtils.nullSafeEquals(this.name, tb2.name) && this.age == tb2.age);
	}

	@Override
	public int hashCode() {
		return this.age;
	}

	@Override
	public int compareTo(Object other) {
		if (this.name != null && other instanceof TestBean) {
			return this.name.compareTo(((TestBean) other).getName());
		}
		else {
			return 1;
		}
	}

	@Override
	public String toString() {
		return this.name;
	}

}
