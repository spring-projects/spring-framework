/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.dataaccess.jdbc.jdbccomplextypes;

import java.util.Date;

class TestItem {

	private Long id;

	private String description;

	private Date expirationDate;

	public TestItem() {
	}

	public TestItem(Long id, String description, Date expirationDate) {
		this.id = id;
		this.description = description;
		this.expirationDate = expirationDate;
	}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getExpirationDate() {
		return this.expirationDate;
	}

	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}
}
