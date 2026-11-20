/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.transaction.ejb.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Test entity for EJB transaction support in the TestContext framework.
 *
 * @author Xavier Detant
 * @author Sam Brannen
 * @since 4.0.1
 */
@Entity
@Table(name = TestEntity.TABLE_NAME)
public class TestEntity {

	public static final String TABLE_NAME = "TEST_ENTITY";

	@Id
	@Column(name = "TE_NAME", nullable = false)
	private String name;

	@Column(name = "TE_COUNT", nullable = false)
	private int count;


	public TestEntity() {
	}

	public TestEntity(String name, int count) {
		this.name = name;
		this.count = count;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getCount() {
		return this.count;
	}

	public void setCount(int count) {
		this.count = count;
	}
}
