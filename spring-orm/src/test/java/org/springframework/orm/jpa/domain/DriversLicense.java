/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="DRIVERS_LICENSE")
public class DriversLicense {

	@Id
	private int id;

	private String serial_number;


	protected DriversLicense() {
	}

	public DriversLicense(String serialNumber) {
		this.serial_number = serialNumber;
	}

	public String getSerialNumber() {
		return serial_number;
	}

}
