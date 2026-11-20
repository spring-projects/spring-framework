/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.orm.hibernate.domain;

/**
 * Person POJO.
 *
 * @author Sam Brannen
 * @since 3.0
 */
public class Person {

	private Long id;
	private String name;
	private DriversLicense driversLicense;


	public Person() {
	}

	public Person(Long id) {
		this(id, null, null);
	}

	public Person(String name) {
		this(name, null);
	}

	public Person(String name, DriversLicense driversLicense) {
		this(null, name, driversLicense);
	}

	public Person(Long id, String name, DriversLicense driversLicense) {
		this.id = id;
		this.name = name;
		this.driversLicense = driversLicense;
	}

	public Long getId() {
		return this.id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public DriversLicense getDriversLicense() {
		return this.driversLicense;
	}

	public void setDriversLicense(DriversLicense driversLicense) {
		this.driversLicense = driversLicense;
	}
}
