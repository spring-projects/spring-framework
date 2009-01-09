/*
 * XML Type:  flightType
 * Namespace: http://samples.springframework.org/flight
 * Java type: org.springframework.samples.flight.FlightType
 *
 * Automatically generated - do not modify.
 */
package org.springframework.oxm.xmlbeans.impl;

/**
 * An XML flightType(@http://samples.springframework.org/flight).
 *
 * This is a complex type.
 */
public class FlightTypeImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl
		implements org.springframework.oxm.xmlbeans.FlightType {

	public FlightTypeImpl(org.apache.xmlbeans.SchemaType sType) {
		super(sType);
	}

	private static final javax.xml.namespace.QName NUMBER$0 =
			new javax.xml.namespace.QName("http://samples.springframework.org/flight", "number");

	/** Gets the "number" element */
	public long getNumber() {
		synchronized (monitor()) {
			check_orphaned();
			org.apache.xmlbeans.SimpleValue target = null;
			target = (org.apache.xmlbeans.SimpleValue) get_store().find_element_user(NUMBER$0, 0);
			if (target == null) {
				return 0L;
			}
			return target.getLongValue();
		}
	}

	/** Gets (as xml) the "number" element */
	public org.apache.xmlbeans.XmlLong xgetNumber() {
		synchronized (monitor()) {
			check_orphaned();
			org.apache.xmlbeans.XmlLong target = null;
			target = (org.apache.xmlbeans.XmlLong) get_store().find_element_user(NUMBER$0, 0);
			return target;
		}
	}

	/** Sets the "number" element */
	public void setNumber(long number) {
		synchronized (monitor()) {
			check_orphaned();
			org.apache.xmlbeans.SimpleValue target = null;
			target = (org.apache.xmlbeans.SimpleValue) get_store().find_element_user(NUMBER$0, 0);
			if (target == null) {
				target = (org.apache.xmlbeans.SimpleValue) get_store().add_element_user(NUMBER$0);
			}
			target.setLongValue(number);
		}
	}

	/** Sets (as xml) the "number" element */
	public void xsetNumber(org.apache.xmlbeans.XmlLong number) {
		synchronized (monitor()) {
			check_orphaned();
			org.apache.xmlbeans.XmlLong target = null;
			target = (org.apache.xmlbeans.XmlLong) get_store().find_element_user(NUMBER$0, 0);
			if (target == null) {
				target = (org.apache.xmlbeans.XmlLong) get_store().add_element_user(NUMBER$0);
			}
			target.set(number);
		}
	}
}
