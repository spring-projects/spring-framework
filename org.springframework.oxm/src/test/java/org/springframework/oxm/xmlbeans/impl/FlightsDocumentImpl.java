/*
 * An XML document type.
 * Localname: flights
 * Namespace: http://samples.springframework.org/flight
 * Java type: org.springframework.samples.flight.FlightsDocument
 *
 * Automatically generated - do not modify.
 */
package org.springframework.oxm.xmlbeans.impl;

/**
 * A document containing one flights(@http://samples.springframework.org/flight) element.
 *
 * This is a complex type.
 */
public class FlightsDocumentImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl
		implements org.springframework.oxm.xmlbeans.FlightsDocument {

	public FlightsDocumentImpl(org.apache.xmlbeans.SchemaType sType) {
		super(sType);
	}

	private static final javax.xml.namespace.QName FLIGHTS$0 =
			new javax.xml.namespace.QName("http://samples.springframework.org/flight", "flights");

	/** Gets the "flights" element */
	public org.springframework.oxm.xmlbeans.FlightsDocument.Flights getFlights() {
		synchronized (monitor()) {
			check_orphaned();
			org.springframework.oxm.xmlbeans.FlightsDocument.Flights target = null;
			target = (org.springframework.oxm.xmlbeans.FlightsDocument.Flights) get_store()
					.find_element_user(FLIGHTS$0, 0);
			if (target == null) {
				return null;
			}
			return target;
		}
	}

	/** Sets the "flights" element */
	public void setFlights(org.springframework.oxm.xmlbeans.FlightsDocument.Flights flights) {
		synchronized (monitor()) {
			check_orphaned();
			org.springframework.oxm.xmlbeans.FlightsDocument.Flights target = null;
			target = (org.springframework.oxm.xmlbeans.FlightsDocument.Flights) get_store()
					.find_element_user(FLIGHTS$0, 0);
			if (target == null) {
				target = (org.springframework.oxm.xmlbeans.FlightsDocument.Flights) get_store()
						.add_element_user(FLIGHTS$0);
			}
			target.set(flights);
		}
	}

	/** Appends and returns a new empty "flights" element */
	public org.springframework.oxm.xmlbeans.FlightsDocument.Flights addNewFlights() {
		synchronized (monitor()) {
			check_orphaned();
			org.springframework.oxm.xmlbeans.FlightsDocument.Flights target = null;
			target = (org.springframework.oxm.xmlbeans.FlightsDocument.Flights) get_store().add_element_user(FLIGHTS$0);
			return target;
		}
	}

	/**
	 * An XML flights(@http://samples.springframework.org/flight).
	 *
	 * This is a complex type.
	 */
	public static class FlightsImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl
			implements org.springframework.oxm.xmlbeans.FlightsDocument.Flights {

		public FlightsImpl(org.apache.xmlbeans.SchemaType sType) {
			super(sType);
		}

		private static final javax.xml.namespace.QName FLIGHT$0 =
				new javax.xml.namespace.QName("http://samples.springframework.org/flight", "flight");

		/** Gets a List of "flight" elements */
		public java.util.List<org.springframework.oxm.xmlbeans.FlightType> getFlightList() {
			final class FlightList extends java.util.AbstractList<org.springframework.oxm.xmlbeans.FlightType> {

				public org.springframework.oxm.xmlbeans.FlightType get(int i) {
					return FlightsImpl.this.getFlightArray(i);
				}

				public org.springframework.oxm.xmlbeans.FlightType set(int i,
						org.springframework.oxm.xmlbeans.FlightType o) {
					org.springframework.oxm.xmlbeans.FlightType old = FlightsImpl.this.getFlightArray(i);
					FlightsImpl.this.setFlightArray(i, o);
					return old;
				}

				public void add(int i, org.springframework.oxm.xmlbeans.FlightType o) {
					FlightsImpl.this.insertNewFlight(i).set(o);
				}

				public org.springframework.oxm.xmlbeans.FlightType remove(int i) {
					org.springframework.oxm.xmlbeans.FlightType old = FlightsImpl.this.getFlightArray(i);
					FlightsImpl.this.removeFlight(i);
					return old;
				}

				public int size() {
					return FlightsImpl.this.sizeOfFlightArray();
				}

			}

			synchronized (monitor()) {
				check_orphaned();
				return new FlightList();
			}
		}

		/** Gets array of all "flight" elements */
		public org.springframework.oxm.xmlbeans.FlightType[] getFlightArray() {
			synchronized (monitor()) {
				check_orphaned();
				java.util.List targetList = new java.util.ArrayList();
				get_store().find_all_element_users(FLIGHT$0, targetList);
				org.springframework.oxm.xmlbeans.FlightType[] result =
						new org.springframework.oxm.xmlbeans.FlightType[targetList.size()];
				targetList.toArray(result);
				return result;
			}
		}

		/** Gets ith "flight" element */
		public org.springframework.oxm.xmlbeans.FlightType getFlightArray(int i) {
			synchronized (monitor()) {
				check_orphaned();
				org.springframework.oxm.xmlbeans.FlightType target = null;
				target = (org.springframework.oxm.xmlbeans.FlightType) get_store().find_element_user(FLIGHT$0, i);
				if (target == null) {
					throw new IndexOutOfBoundsException();
				}
				return target;
			}
		}

		/** Returns number of "flight" element */
		public int sizeOfFlightArray() {
			synchronized (monitor()) {
				check_orphaned();
				return get_store().count_elements(FLIGHT$0);
			}
		}

		/** Sets array of all "flight" element */
		public void setFlightArray(org.springframework.oxm.xmlbeans.FlightType[] flightArray) {
			synchronized (monitor()) {
				check_orphaned();
				arraySetterHelper(flightArray, FLIGHT$0);
			}
		}

		/** Sets ith "flight" element */
		public void setFlightArray(int i, org.springframework.oxm.xmlbeans.FlightType flight) {
			synchronized (monitor()) {
				check_orphaned();
				org.springframework.oxm.xmlbeans.FlightType target = null;
				target = (org.springframework.oxm.xmlbeans.FlightType) get_store().find_element_user(FLIGHT$0, i);
				if (target == null) {
					throw new IndexOutOfBoundsException();
				}
				target.set(flight);
			}
		}

		/** Inserts and returns a new empty value (as xml) as the ith "flight" element */
		public org.springframework.oxm.xmlbeans.FlightType insertNewFlight(int i) {
			synchronized (monitor()) {
				check_orphaned();
				org.springframework.oxm.xmlbeans.FlightType target = null;
				target = (org.springframework.oxm.xmlbeans.FlightType) get_store().insert_element_user(FLIGHT$0, i);
				return target;
			}
		}

		/** Appends and returns a new empty value (as xml) as the last "flight" element */
		public org.springframework.oxm.xmlbeans.FlightType addNewFlight() {
			synchronized (monitor()) {
				check_orphaned();
				org.springframework.oxm.xmlbeans.FlightType target = null;
				target = (org.springframework.oxm.xmlbeans.FlightType) get_store().add_element_user(FLIGHT$0);
				return target;
			}
		}

		/** Removes the ith "flight" element */
		public void removeFlight(int i) {
			synchronized (monitor()) {
				check_orphaned();
				get_store().remove_element(FLIGHT$0, i);
			}
		}
	}
}
