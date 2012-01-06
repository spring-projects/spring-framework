package org.springframework.oxm.jaxb.test.annotations;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Flight {

	private String name;

	public void setName(final String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
