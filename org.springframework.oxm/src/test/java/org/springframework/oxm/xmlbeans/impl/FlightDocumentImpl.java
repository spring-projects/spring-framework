/*
 * An XML document type.
 * Localname: flight
 * Namespace: http://samples.springframework.org/flight
 * Java type: org.springframework.oxm.xmlbeans.FlightDocument
 *
 * Automatically generated - do not modify.
 */
package org.springframework.oxm.xmlbeans.impl;

/**
 * A document containing one flight(@http://samples.springframework.org/flight) element.
 *
 * This is a complex type.
 */
public class FlightDocumentImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl
		implements org.springframework.oxm.xmlbeans.FlightDocument {

	public FlightDocumentImpl(org.apache.xmlbeans.SchemaType sType) {
		super(sType);
	}

	private static final javax.xml.namespace.QName FLIGHT$0 =
			new javax.xml.namespace.QName("http://samples.springframework.org/flight", "flight");

	/** Gets the "flight" element */
	public org.springframework.oxm.xmlbeans.FlightType getFlight() {
		synchronized (monitor()) {
			check_orphaned();
			org.springframework.oxm.xmlbeans.FlightType target = null;
			target = (org.springframework.oxm.xmlbeans.FlightType) get_store().find_element_user(FLIGHT$0, 0);
			if (target == null) {
				return null;
			}
			return target;
		}
	}

	/** Sets the "flight" element */
	public void setFlight(org.springframework.oxm.xmlbeans.FlightType flight) {
		synchronized (monitor()) {
			check_orphaned();
			org.springframework.oxm.xmlbeans.FlightType target = null;
			target = (org.springframework.oxm.xmlbeans.FlightType) get_store().find_element_user(FLIGHT$0, 0);
			if (target == null) {
				target = (org.springframework.oxm.xmlbeans.FlightType) get_store().add_element_user(FLIGHT$0);
			}
			target.set(flight);
		}
	}

	/** Appends and returns a new empty "flight" element */
	public org.springframework.oxm.xmlbeans.FlightType addNewFlight() {
		synchronized (monitor()) {
			check_orphaned();
			org.springframework.oxm.xmlbeans.FlightType target = null;
			target = (org.springframework.oxm.xmlbeans.FlightType) get_store().add_element_user(FLIGHT$0);
			return target;
		}
	}
}
