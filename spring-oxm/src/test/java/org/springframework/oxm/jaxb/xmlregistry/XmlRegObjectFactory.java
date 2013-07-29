
package org.springframework.oxm.jaxb.xmlregistry;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

@XmlRegistry
public class XmlRegObjectFactory {

	@XmlElementDecl(name = "brand-airplane")
	public JAXBElement<Airplane> createAirplane(Airplane airplane) {
		return new JAXBElement<Airplane>(new QName("brand-airplane"), Airplane.class, null, airplane);
	}
}