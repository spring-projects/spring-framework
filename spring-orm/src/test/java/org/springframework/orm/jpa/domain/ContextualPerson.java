/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.orm.jpa.domain;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;

import org.springframework.tests.sample.beans.TestBean;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * @author Juergen Hoeller
 */
@Entity
@Configurable
public class ContextualPerson {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Integer id;

	private transient TestBean testBean;

	// Lazy relationship to force use of instrumentation in JPA implementation.
	// TopLink, at least, will not instrument classes unless absolutely necessary.
	@OneToOne(fetch=FetchType.LAZY, cascade=CascadeType.PERSIST)
	@JoinColumn(name="DRIVERS_LICENSE_ID")
	private DriversLicense driversLicense;

	private String first_name;

	@Basic(fetch=FetchType.LAZY)
	private String last_name;

	@PersistenceContext
	public transient EntityManager entityManager;


	public Integer getId() {
		return id;
	}

	public void setTestBean(TestBean testBean) {
		this.testBean = testBean;
	}

	public TestBean getTestBean() {
		return testBean;
	}

	public void setFirstName(String firstName) {
		this.first_name = firstName;
	}

	public String getFirstName() {
		return this.first_name;
	}

	public void setLastName(String lastName) {
		this.last_name = lastName;
	}

	public String getLastName() {
		return this.last_name;
	}

	public void setDriversLicense(DriversLicense driversLicense) {
		this.driversLicense = driversLicense;
	}

	public DriversLicense getDriversLicense() {
		return this.driversLicense;
	}


	@Override
	public String toString() {
		return getClass().getName() + ":(" + hashCode() + ") id=" + id +
				"; firstName=" + first_name + "; lastName=" + last_name + "; testBean=" + testBean;
	}

}
