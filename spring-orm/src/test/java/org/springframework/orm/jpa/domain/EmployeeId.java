/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.domain;

import java.io.Serial;
import java.io.Serializable;

public class EmployeeId implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private String name;

	@SuppressWarnings("unused")
	private String department;

}
