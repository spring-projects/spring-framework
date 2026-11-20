/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.xml;

import javax.xml.namespace.QName;

import org.junit.jupiter.api.Test;

import org.springframework.http.codec.xml.jaxb.XmlRootElement;
import org.springframework.http.codec.xml.jaxb.XmlRootElementWithName;
import org.springframework.http.codec.xml.jaxb.XmlRootElementWithNameAndNamespace;
import org.springframework.http.codec.xml.jaxb.XmlType;
import org.springframework.http.codec.xml.jaxb.XmlTypeSeeAlso;
import org.springframework.http.codec.xml.jaxb.XmlTypeWithName;
import org.springframework.http.codec.xml.jaxb.XmlTypeWithNameAndNamespace;
import org.springframework.web.testfixture.xml.Pojo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class Jaxb2HelperTests {

	@Test
	void toExpectedQName() {
		assertThat(Jaxb2Helper.toQNames(Pojo.class)).containsExactly(new QName("pojo"));
		assertThat(Jaxb2Helper.toQNames(TypePojo.class)).containsExactly(new QName("pojo"));

		assertThat(Jaxb2Helper.toQNames(XmlRootElementWithNameAndNamespace.class)).containsExactly(new QName("namespace-type", "name-type"));
		assertThat(Jaxb2Helper.toQNames(XmlRootElementWithName.class)).containsExactly(new QName("namespace-package", "name-type"));
		assertThat(Jaxb2Helper.toQNames(XmlRootElement.class)).containsExactly(new QName("namespace-package", "xmlRootElement"));

		assertThat(Jaxb2Helper.toQNames(XmlTypeWithNameAndNamespace.class)).containsExactly(new QName("namespace-type", "name-type"));
		assertThat(Jaxb2Helper.toQNames(XmlTypeWithName.class)).containsExactly(new QName("namespace-package", "name-type"));
		assertThat(Jaxb2Helper.toQNames(XmlType.class)).containsExactly(new QName("namespace-package", "xmlType"));
		assertThat(Jaxb2Helper.toQNames(XmlTypeSeeAlso.class)).containsExactlyInAnyOrder(new QName("namespace-package", "xmlTypeSeeAlso"),
				new QName("namespace-package", "name-type"), new QName("namespace-type", "name-type"));
	}


}
