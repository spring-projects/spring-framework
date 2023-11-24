/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
	public void toExpectedQName() {
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
