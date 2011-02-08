/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.beans;

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
@SuppressWarnings("rawtypes")
public class TestBean implements BeanNameAware, BeanFactoryAware, ITestBean, IOther, Comparable {

	private String beanName;

	private String country;

	private BeanFactory beanFactory;

	private boolean postProcessed;

	private String name;

	private String sex;

	private int age;

	private boolean jedi;

	private ITestBean[] spouses;

	private String touchy;

	private String[] stringArray;

	private Integer[] someIntegerArray;

	private Date date = new Date();

	private Float myFloat = new Float(0.0);

	private Collection friends = new LinkedList();

	private Set someSet = new HashSet();

	private Map someMap = new HashMap();

	private List someList = new ArrayList();

	private Properties someProperties = new Properties();

	private INestedTestBean doctor = new NestedTestBean();

	private INestedTestBean lawyer = new NestedTestBean();

	private IndexedTestBean nestedIndexedBean;

	private boolean destroyed;

	private Number someNumber;

	private Colour favouriteColour;

	private Boolean someBoolean;

	private List otherColours;

	private List pets;


	public TestBean() {
	}

	public TestBean(String name) {
		this.name = name;
	}

	public TestBean(ITestBean spouse) {
		this.spouses = new ITestBean[] { spouse };
	}

	public TestBean(String name, int age) {
		this.name = name;
		this.age = age;
	}

	public TestBean(ITestBean spouse, Properties someProperties) {
		this.spouses = new ITestBean[] { spouse };
		this.someProperties = someProperties;
	}

	public TestBean(List someList) {
		this.someList = someList;
	}

	public TestBean(Set someSet) {
		this.someSet = someSet;
	}

	public TestBean(Map someMap) {
		this.someMap = someMap;
	}

	public TestBean(Properties someProperties) {
		this.someProperties = someProperties;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public String getBeanName() {
		return beanName;
	}

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

	public String getName() {
		return name;
	}

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

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public boolean isJedi() {
		return jedi;
	}

	public void setJedi(boolean jedi) {
		this.jedi = jedi;
	}

	public ITestBean getSpouse() {
		return (spouses != null ? spouses[0] : null);
	}

	public void setSpouse(ITestBean spouse) {
		this.spouses = new ITestBean[] { spouse };
	}

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

	public String[] getStringArray() {
		return stringArray;
	}

	public void setStringArray(String[] stringArray) {
		this.stringArray = stringArray;
	}

	public Integer[] getSomeIntegerArray() {
		return someIntegerArray;
	}

	public void setSomeIntegerArray(Integer[] someIntegerArray) {
		this.someIntegerArray = someIntegerArray;
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

	public Collection getFriends() {
		return friends;
	}

	public void setFriends(Collection friends) {
		this.friends = friends;
	}

	public Set getSomeSet() {
		return someSet;
	}

	public void setSomeSet(Set someSet) {
		this.someSet = someSet;
	}

	public Map getSomeMap() {
		return someMap;
	}

	public void setSomeMap(Map someMap) {
		this.someMap = someMap;
	}

	public List getSomeList() {
		return someList;
	}

	public void setSomeList(List someList) {
		this.someList = someList;
	}

	public Properties getSomeProperties() {
		return someProperties;
	}

	public void setSomeProperties(Properties someProperties) {
		this.someProperties = someProperties;
	}

	public INestedTestBean getDoctor() {
		return doctor;
	}

	public void setDoctor(INestedTestBean doctor) {
		this.doctor = doctor;
	}

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

	public IndexedTestBean getNestedIndexedBean() {
		return nestedIndexedBean;
	}

	public void setNestedIndexedBean(IndexedTestBean nestedIndexedBean) {
		this.nestedIndexedBean = nestedIndexedBean;
	}

	public List getOtherColours() {
		return otherColours;
	}

	public void setOtherColours(List otherColours) {
		this.otherColours = otherColours;
	}

	public List getPets() {
		return pets;
	}

	public void setPets(List pets) {
		this.pets = pets;
	}

	/**
	 * @see ITestBean#exceptional(Throwable)
	 */
	public void exceptional(Throwable t) throws Throwable {
		if (t != null) {
			throw t;
		}
	}

	public void unreliableFileOperation() throws IOException {
		throw new IOException();
	}

	/**
	 * @see ITestBean#returnsThis()
	 */
	public Object returnsThis() {
		return this;
	}

	/**
	 * @see IOther#absquatulate()
	 */
	public void absquatulate() {
	}

	public int haveBirthday() {
		return age++;
	}

	public void destroy() {
		this.destroyed = true;
	}

	public boolean wasDestroyed() {
		return destroyed;
	}

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

	public int hashCode() {
		return this.age;
	}

	public int compareTo(Object other) {
		if (this.name != null && other instanceof TestBean) {
			return this.name.compareTo(((TestBean) other).getName());
		}
		else {
			return 1;
		}
	}

	public String toString() {
		return this.name;
	}

}