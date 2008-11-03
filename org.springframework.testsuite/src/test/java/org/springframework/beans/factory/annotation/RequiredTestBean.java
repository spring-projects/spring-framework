/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.beans.factory.annotation;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;

/**
 * @author Rob Harrop
 * @since 2.0
 */
public class RequiredTestBean implements BeanNameAware, BeanFactoryAware {

	private String name;

	private int age;

	private String favouriteColour;

	private String jobTitle;


	public int getAge() {
		return age;
	}

	@Required
	public void setAge(int age) {
		this.age = age;
	}

	public String getName() {
		return name;
	}

	@MyRequired
	public void setName(String name) {
		this.name = name;
	}

	public String getFavouriteColour() {
		return favouriteColour;
	}

	@Required
	public void setFavouriteColour(String favouriteColour) {
		this.favouriteColour = favouriteColour;
	}

	public String getJobTitle() {
		return jobTitle;
	}

	@Required
	public void setJobTitle(String jobTitle) {
		this.jobTitle = jobTitle;
	}

	@Required
	public void setBeanName(String name) {
	}

	@Required
	public void setBeanFactory(BeanFactory beanFactory) {
	}

}
