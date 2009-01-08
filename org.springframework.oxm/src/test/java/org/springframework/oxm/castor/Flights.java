/*
 * This class was automatically generated with 
 * <a href="http://www.castor.org">Castor 1.1</a>, using an XML
 * Schema.
 * $Id$
 */

package org.springframework.oxm.castor;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/

import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;

/**
 * Class Flights.
 *
 * @version $Revision$ $Date$
 */
public class Flights implements java.io.Serializable {

	//--------------------------/
	//- Class/Member Variables -/
	//--------------------------/

	/** Field _flightList. */
	private java.util.List _flightList;

	//----------------/
	//- Constructors -/
	//----------------/

	public Flights() {
		super();
		this._flightList = new java.util.ArrayList();
	}

	//-----------/
	//- Methods -/
	//-----------/

	/**
	 * @throws java.lang.IndexOutOfBoundsException
	 *          if the index given is outside the bounds of the collection
	 */
	public void addFlight(final org.springframework.oxm.castor.Flight vFlight)
			throws java.lang.IndexOutOfBoundsException {
		this._flightList.add(vFlight);
	}

	/**
	 * @throws java.lang.IndexOutOfBoundsException
	 *          if the index given is outside the bounds of the collection
	 */
	public void addFlight(final int index, final org.springframework.oxm.castor.Flight vFlight)
			throws java.lang.IndexOutOfBoundsException {
		this._flightList.add(index, vFlight);
	}

	/**
	 * Method enumerateFlight.
	 *
	 * @return an Enumeration over all possible elements of this collection
	 */
	public java.util.Enumeration enumerateFlight() {
		return java.util.Collections.enumeration(this._flightList);
	}

	/**
	 * Method getFlight.
	 *
	 * @return the value of the org.springframework.oxm.castor.Flight at the given index
	 * @throws java.lang.IndexOutOfBoundsException
	 *          if the index given is outside the bounds of the collection
	 */
	public org.springframework.oxm.castor.Flight getFlight(final int index) throws java.lang.IndexOutOfBoundsException {
		// check bounds for index
		if (index < 0 || index >= this._flightList.size()) {
			throw new IndexOutOfBoundsException(
					"getFlight: Index value '" + index + "' not in range [0.." + (this._flightList.size() - 1) + "]");
		}

		return (org.springframework.oxm.castor.Flight) _flightList.get(index);
	}

	/**
	 * Method getFlight.Returns the contents of the collection in an Array.  <p>Note:  Just in case the collection
	 * contents are changing in another thread, we pass a 0-length Array of the correct type into the API call.  This
	 * way we <i>know</i> that the Array returned is of exactly the correct length.
	 *
	 * @return this collection as an Array
	 */
	public org.springframework.oxm.castor.Flight[] getFlight() {
		org.springframework.oxm.castor.Flight[] array = new org.springframework.oxm.castor.Flight[0];
		return (org.springframework.oxm.castor.Flight[]) this._flightList.toArray(array);
	}

	/**
	 * Method getFlightCount.
	 *
	 * @return the size of this collection
	 */
	public int getFlightCount() {
		return this._flightList.size();
	}

	/**
	 * Method isValid.
	 *
	 * @return true if this object is valid according to the schema
	 */
	public boolean isValid() {
		try {
			validate();
		}
		catch (org.exolab.castor.xml.ValidationException vex) {
			return false;
		}
		return true;
	}

	/**
	 * Method iterateFlight.
	 *
	 * @return an Iterator over all possible elements in this collection
	 */
	public java.util.Iterator iterateFlight() {
		return this._flightList.iterator();
	}

	/**
	 * @throws org.exolab.castor.xml.MarshalException
	 *          if object is null or if any SAXException is thrown during marshaling
	 * @throws org.exolab.castor.xml.ValidationException
	 *          if this object is an invalid instance according to the schema
	 */
	public void marshal(final java.io.Writer out)
			throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
		Marshaller.marshal(this, out);
	}

	/**
	 * @throws java.io.IOException if an IOException occurs during marshaling
	 * @throws org.exolab.castor.xml.ValidationException
	 *                             if this object is an invalid instance according to the schema
	 * @throws org.exolab.castor.xml.MarshalException
	 *                             if object is null or if any SAXException is thrown during marshaling
	 */
	public void marshal(final org.xml.sax.ContentHandler handler)
			throws java.io.IOException, org.exolab.castor.xml.MarshalException,
			org.exolab.castor.xml.ValidationException {
		Marshaller.marshal(this, handler);
	}

	/**
	 */
	public void removeAllFlight() {
		this._flightList.clear();
	}

	/**
	 * Method removeFlight.
	 *
	 * @return true if the object was removed from the collection.
	 */
	public boolean removeFlight(final org.springframework.oxm.castor.Flight vFlight) {
		boolean removed = _flightList.remove(vFlight);
		return removed;
	}

	/**
	 * Method removeFlightAt.
	 *
	 * @return the element removed from the collection
	 */
	public org.springframework.oxm.castor.Flight removeFlightAt(final int index) {
		java.lang.Object obj = this._flightList.remove(index);
		return (org.springframework.oxm.castor.Flight) obj;
	}

	/**
	 * @throws java.lang.IndexOutOfBoundsException
	 *          if the index given is outside the bounds of the collection
	 */
	public void setFlight(final int index, final org.springframework.oxm.castor.Flight vFlight)
			throws java.lang.IndexOutOfBoundsException {
		// check bounds for index
		if (index < 0 || index >= this._flightList.size()) {
			throw new IndexOutOfBoundsException(
					"setFlight: Index value '" + index + "' not in range [0.." + (this._flightList.size() - 1) + "]");
		}

		this._flightList.set(index, vFlight);
	}

	/**
	 *
	 *
	 * @param vFlightArray
	 */
	public void setFlight(final org.springframework.oxm.castor.Flight[] vFlightArray) {
		//-- copy array
		_flightList.clear();

		for (int i = 0; i < vFlightArray.length; i++) {
			this._flightList.add(vFlightArray[i]);
		}
	}

	/**
	 * Method unmarshal.
	 *
	 * @return the unmarshaled org.springframework.oxm.castor.Flight
	 * @throws org.exolab.castor.xml.MarshalException
	 *          if object is null or if any SAXException is thrown during marshaling
	 * @throws org.exolab.castor.xml.ValidationException
	 *          if this object is an invalid instance according to the schema
	 */
	public static org.springframework.oxm.castor.Flights unmarshal(final java.io.Reader reader)
			throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
		return (org.springframework.oxm.castor.Flights) Unmarshaller
				.unmarshal(org.springframework.oxm.castor.Flights.class, reader);
	}

	/**
	 * @throws org.exolab.castor.xml.ValidationException
	 *          if this object is an invalid instance according to the schema
	 */
	public void validate() throws org.exolab.castor.xml.ValidationException {
		org.exolab.castor.xml.Validator validator = new org.exolab.castor.xml.Validator();
		validator.validate(this);
	}

}
