/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.oxm.jaxb;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class XmlRegObjectFactory {

	@XmlElementDecl(name = "brand-airplane")
	public JAXBElement<Airplane> createAirplane(Airplane airplane) {
		return new JAXBElement<>(new QName("brand-airplane"), Airplane.class, null, airplane);
	}
}
