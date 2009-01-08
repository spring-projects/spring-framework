/*
 * This class was automatically generated with 
 * <a href="http://www.castor.org">Castor 1.1</a>, using an XML
 * Schema.
 * $Id$
 */

package org.springframework.oxm.castor.descriptors;

//---------------------------------/
//- Imported classes and packages -/
//---------------------------------/

/**
 * Class FlightDescriptor.
 *
 * @version $Revision$ $Date$
 */
public class FlightDescriptor extends org.springframework.oxm.castor.descriptors.FlightTypeDescriptor {

	//--------------------------/
	//- Class/Member Variables -/
	//--------------------------/

	/** Field _elementDefinition. */
	private boolean _elementDefinition;

	/** Field _nsPrefix. */
	private java.lang.String _nsPrefix;

	/** Field _nsURI. */
	private java.lang.String _nsURI;

	/** Field _xmlName. */
	private java.lang.String _xmlName;

	/** Field _identity. */
	private org.exolab.castor.xml.XMLFieldDescriptor _identity;

	//----------------/
	//- Constructors -/
	//----------------/

	public FlightDescriptor() {
		super();
		setExtendsWithoutFlatten(new org.springframework.oxm.castor.descriptors.FlightTypeDescriptor());
		_nsURI = "http://samples.springframework.org/flight";
		_xmlName = "flight";
		_elementDefinition = true;
	}

	//-----------/
	//- Methods -/
	//-----------/

	/**
	 * Method getAccessMode.
	 *
	 * @return the access mode specified for this class.
	 */
	public org.exolab.castor.mapping.AccessMode getAccessMode() {
		return null;
	}

	/**
	 * Method getIdentity.
	 *
	 * @return the identity field, null if this class has no identity.
	 */
	public org.exolab.castor.mapping.FieldDescriptor getIdentity() {
		if (_identity == null) {
			return super.getIdentity();
		}
		return _identity;
	}

	/**
	 * Method getJavaClass.
	 *
	 * @return the Java class represented by this descriptor.
	 */
	public java.lang.Class getJavaClass() {
		return org.springframework.oxm.castor.Flight.class;
	}

	/**
	 * Method getNameSpacePrefix.
	 *
	 * @return the namespace prefix to use when marshaling as XML.
	 */
	public java.lang.String getNameSpacePrefix() {
		return _nsPrefix;
	}

	/**
	 * Method getNameSpaceURI.
	 *
	 * @return the namespace URI used when marshaling and unmarshaling as XML.
	 */
	public java.lang.String getNameSpaceURI() {
		return _nsURI;
	}

	/**
	 * Method getValidator.
	 *
	 * @return a specific validator for the class described by this ClassDescriptor.
	 */
	public org.exolab.castor.xml.TypeValidator getValidator() {
		return this;
	}

	/**
	 * Method getXMLName.
	 *
	 * @return the XML Name for the Class being described.
	 */
	public java.lang.String getXMLName() {
		return _xmlName;
	}

	/**
	 * Method isElementDefinition.
	 *
	 * @return true if XML schema definition of this Class is that of a global element or element with anonymous type
	 *         definition.
	 */
	public boolean isElementDefinition() {
		return _elementDefinition;
	}

}
