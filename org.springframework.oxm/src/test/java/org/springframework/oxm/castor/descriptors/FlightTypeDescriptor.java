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

import org.springframework.oxm.castor.FlightType;

/**
 * Class FlightTypeDescriptor.
 *
 * @version $Revision$ $Date$
 */
public class FlightTypeDescriptor extends org.exolab.castor.xml.util.XMLClassDescriptorImpl {

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

	public FlightTypeDescriptor() {
		super();
		_nsURI = "http://samples.springframework.org/flight";
		_xmlName = "flightType";
		_elementDefinition = false;

		//-- set grouping compositor
		setCompositorAsSequence();
		org.exolab.castor.xml.util.XMLFieldDescriptorImpl desc = null;
		org.exolab.castor.mapping.FieldHandler handler = null;
		org.exolab.castor.xml.FieldValidator fieldValidator = null;
		//-- initialize attribute descriptors

		//-- initialize element descriptors

		//-- _number
		desc = new org.exolab.castor.xml.util.XMLFieldDescriptorImpl(java.lang.Long.TYPE, "_number", "number",
				org.exolab.castor.xml.NodeType.Element);
		handler = new org.exolab.castor.xml.XMLFieldHandler() {
			public java.lang.Object getValue(java.lang.Object object) throws IllegalStateException {
				FlightType target = (FlightType) object;
				if (!target.hasNumber()) {
					return null;
				}
				return new java.lang.Long(target.getNumber());
			}

			public void setValue(java.lang.Object object, java.lang.Object value)
					throws IllegalStateException, IllegalArgumentException {
				try {
					FlightType target = (FlightType) object;
					// ignore null values for non optional primitives
					if (value == null) {
						return;
					}

					target.setNumber(((java.lang.Long) value).longValue());
				}
				catch (java.lang.Exception ex) {
					throw new IllegalStateException(ex.toString());
				}
			}

			public java.lang.Object newInstance(java.lang.Object parent) {
				return null;
			}
		};
		desc.setHandler(handler);
		desc.setNameSpaceURI("http://samples.springframework.org/flight");
		desc.setRequired(true);
		desc.setMultivalued(false);
		addFieldDescriptor(desc);

		//-- validation code for: _number
		fieldValidator = new org.exolab.castor.xml.FieldValidator();
		fieldValidator.setMinOccurs(1);
		{ //-- local scope
			org.exolab.castor.xml.validators.LongValidator typeValidator;
			typeValidator = new org.exolab.castor.xml.validators.LongValidator();
			fieldValidator.setValidator(typeValidator);
			typeValidator.setMinInclusive(-9223372036854775808L);
			typeValidator.setMaxInclusive(9223372036854775807L);
		}
		desc.setValidator(fieldValidator);
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
		return _identity;
	}

	/**
	 * Method getJavaClass.
	 *
	 * @return the Java class represented by this descriptor.
	 */
	public java.lang.Class getJavaClass() {
		return org.springframework.oxm.castor.FlightType.class;
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
