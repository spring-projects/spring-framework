/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * XML Type:  flightType
 * Namespace: http://samples.springframework.org/flight
 * Java type: org.springframework.samples.flight.FlightType
 *
 * Automatically generated - do not modify.
 */
package org.springframework.oxm.xmlbeans;

/**
 * An XML flightType(@http://samples.springframework.org/flight).
 *
 * This is a complex type.
 */
public interface FlightType extends org.apache.xmlbeans.XmlObject {

	public static final org.apache.xmlbeans.SchemaType type =
			(org.apache.xmlbeans.SchemaType) org.apache.xmlbeans.XmlBeans
					.typeSystemForClassLoader(FlightType.class.getClassLoader(),
							"schemaorg_apache_xmlbeans.system.s5EF858A5E57B2761C3670716FC0A909C")
					.resolveHandle("flighttype4702type");

	/** Gets the "number" element */
	long getNumber();

	/** Gets (as xml) the "number" element */
	org.apache.xmlbeans.XmlLong xgetNumber();

	/** Sets the "number" element */
	void setNumber(long number);

	/** Sets (as xml) the "number" element */
	void xsetNumber(org.apache.xmlbeans.XmlLong number);

	/** A factory class with static methods for creating instances of this type. */

	public static final class Factory {

		public static org.springframework.oxm.xmlbeans.FlightType newInstance() {
			return (org.springframework.oxm.xmlbeans.FlightType) org.apache.xmlbeans.XmlBeans.getContextTypeLoader()
					.newInstance(type, null);
		}

		public static org.springframework.oxm.xmlbeans.FlightType newInstance(org.apache.xmlbeans.XmlOptions options) {
			return (org.springframework.oxm.xmlbeans.FlightType) org.apache.xmlbeans.XmlBeans.getContextTypeLoader()
					.newInstance(type, options);
		}

		/** @param xmlAsString the string value to parse */
		public static org.springframework.oxm.xmlbeans.FlightType parse(java.lang.String xmlAsString)
				throws org.apache.xmlbeans.XmlException {
			return (org.springframework.oxm.xmlbeans.FlightType) org.apache.xmlbeans.XmlBeans.getContextTypeLoader()
					.parse(xmlAsString, type, null);
		}

		public static org.springframework.oxm.xmlbeans.FlightType parse(java.lang.String xmlAsString,
				org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException {
			return (org.springframework.oxm.xmlbeans.FlightType) org.apache.xmlbeans.XmlBeans.getContextTypeLoader()
					.parse(xmlAsString, type, options);
		}

		/** @param file the file from which to load an xml document */
		public static org.springframework.oxm.xmlbeans.FlightType parse(java.io.File file)
				throws org.apache.xmlbeans.XmlException, java.io.IOException {
			return (org.springframework.oxm.xmlbeans.FlightType) org.apache.xmlbeans.XmlBeans.getContextTypeLoader()
					.parse(file, type, null);
		}

		public static org.springframework.oxm.xmlbeans.FlightType parse(java.io.File file,
				org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
			return (org.springframework.oxm.xmlbeans.FlightType) org.apache.xmlbeans.XmlBeans.getContextTypeLoader()
					.parse(file, type, options);
		}

		public static org.springframework.oxm.xmlbeans.FlightType parse(java.net.URL u)
				throws org.apache.xmlbeans.XmlException, java.io.IOException {
			return (org.springframework.oxm.xmlbeans.FlightType) org.apache.xmlbeans.XmlBeans.getContextTypeLoader()
					.parse(u, type, null);
		}

		public static org.springframework.oxm.xmlbeans.FlightType parse(java.net.URL u,
				org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
			return (org.springframework.oxm.xmlbeans.FlightType) org.apache.xmlbeans.XmlBeans.getContextTypeLoader()
					.parse(u, type, options);
		}

		public static org.springframework.oxm.xmlbeans.FlightType parse(java.io.InputStream is)
				throws org.apache.xmlbeans.XmlException, java.io.IOException {
			return (org.springframework.oxm.xmlbeans.FlightType) org.apache.xmlbeans.XmlBeans.getContextTypeLoader()
					.parse(is, type, null);
		}

		public static org.springframework.oxm.xmlbeans.FlightType parse(java.io.InputStream is,
				org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
			return (org.springframework.oxm.xmlbeans.FlightType) org.apache.xmlbeans.XmlBeans.getContextTypeLoader()
					.parse(is, type, options);
		}

		public static org.springframework.oxm.xmlbeans.FlightType parse(java.io.Reader r)
				throws org.apache.xmlbeans.XmlException, java.io.IOException {
			return (org.springframework.oxm.xmlbeans.FlightType) org.apache.xmlbeans.XmlBeans.getContextTypeLoader()
					.parse(r, type, null);
		}

		public static org.springframework.oxm.xmlbeans.FlightType parse(java.io.Reader r,
				org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
			return (org.springframework.oxm.xmlbeans.FlightType) org.apache.xmlbeans.XmlBeans.getContextTypeLoader()
					.parse(r, type, options);
		}

		public static org.springframework.oxm.xmlbeans.FlightType parse(javax.xml.stream.XMLStreamReader sr)
				throws org.apache.xmlbeans.XmlException {
			return (org.springframework.oxm.xmlbeans.FlightType) org.apache.xmlbeans.XmlBeans.getContextTypeLoader()
					.parse(sr, type, null);
		}

		public static org.springframework.oxm.xmlbeans.FlightType parse(javax.xml.stream.XMLStreamReader sr,
				org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException {
			return (org.springframework.oxm.xmlbeans.FlightType) org.apache.xmlbeans.XmlBeans.getContextTypeLoader()
					.parse(sr, type, options);
		}

		public static org.springframework.oxm.xmlbeans.FlightType parse(org.w3c.dom.Node node)
				throws org.apache.xmlbeans.XmlException {
			return (org.springframework.oxm.xmlbeans.FlightType) org.apache.xmlbeans.XmlBeans.getContextTypeLoader()
					.parse(node, type, null);
		}

		public static org.springframework.oxm.xmlbeans.FlightType parse(org.w3c.dom.Node node,
				org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException {
			return (org.springframework.oxm.xmlbeans.FlightType) org.apache.xmlbeans.XmlBeans.getContextTypeLoader()
					.parse(node, type, options);
		}

		/** @deprecated {@link XMLInputStream} */
		public static org.springframework.oxm.xmlbeans.FlightType parse(org.apache.xmlbeans.xml.stream.XMLInputStream xis)
				throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
			return (org.springframework.oxm.xmlbeans.FlightType) org.apache.xmlbeans.XmlBeans.getContextTypeLoader()
					.parse(xis, type, null);
		}

		/** @deprecated {@link XMLInputStream} */
		public static org.springframework.oxm.xmlbeans.FlightType parse(org.apache.xmlbeans.xml.stream.XMLInputStream xis,
				org.apache.xmlbeans.XmlOptions options)
				throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
			return (org.springframework.oxm.xmlbeans.FlightType) org.apache.xmlbeans.XmlBeans.getContextTypeLoader()
					.parse(xis, type, options);
		}

		/** @deprecated {@link XMLInputStream} */
		public static org.apache.xmlbeans.xml.stream.XMLInputStream newValidatingXMLInputStream(org.apache.xmlbeans.xml.stream.XMLInputStream xis)
				throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
			return org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newValidatingXMLInputStream(xis, type, null);
		}

		/** @deprecated {@link XMLInputStream} */
		public static org.apache.xmlbeans.xml.stream.XMLInputStream newValidatingXMLInputStream(org.apache.xmlbeans.xml.stream.XMLInputStream xis,
				org.apache.xmlbeans.XmlOptions options)
				throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
			return org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newValidatingXMLInputStream(xis, type, options);
		}

		private Factory() {
		} // No instance of this class allowed
	}
}
