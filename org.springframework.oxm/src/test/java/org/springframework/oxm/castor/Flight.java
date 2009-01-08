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
 * Class Flight.
 *
 * @version $Revision$ $Date$
 */
public class Flight extends FlightType implements java.io.Serializable {

	//----------------/
	//- Constructors -/
	//----------------/

	public Flight() {
		super();
	}

	//-----------/
	//- Methods -/
	//-----------/

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
	 * Method unmarshal.
	 *
	 * @return the unmarshaled org.springframework.oxm.castor.FlightType
	 * @throws org.exolab.castor.xml.MarshalException
	 *          if object is null or if any SAXException is thrown during marshaling
	 * @throws org.exolab.castor.xml.ValidationException
	 *          if this object is an invalid instance according to the schema
	 */
	public static org.springframework.oxm.castor.FlightType unmarshal(final java.io.Reader reader)
			throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
		return (org.springframework.oxm.castor.FlightType) Unmarshaller
				.unmarshal(org.springframework.oxm.castor.Flight.class, reader);
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
