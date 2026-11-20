/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

/**
 * Simple bean used to test dependency checking.
 *
 * @author Rod Johnson
 * @since 04.09.2003
 */
public class DependenciesBean implements BeanFactoryAware {

	private int age;

	private String name;

	private TestBean spouse;

	private BeanFactory beanFactory;


	public void setAge(int age) {
		this.age = age;
	}

	public int getAge() {
		return age;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setSpouse(TestBean spouse) {
		this.spouse = spouse;
	}

	public TestBean getSpouse() {
		return spouse;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	public BeanFactory getBeanFactory() {
		return beanFactory;
	}

}
