/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.oxm.jaxb;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Airplane {

	private String name;


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
