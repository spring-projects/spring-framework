
package org.springframework.oxm.jaxb.test;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.springframework.oxm.jaxb.test package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Flight_QNAME = new QName("http://samples.springframework.org/flight", "flight");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.springframework.oxm.jaxb.test
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Flights }
     * 
     */
    public Flights createFlights() {
        return new Flights();
    }

    /**
     * Create an instance of {@link FlightType }
     * 
     */
    public FlightType createFlightType() {
        return new FlightType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FlightType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://samples.springframework.org/flight", name = "flight")
    public JAXBElement<FlightType> createFlight(FlightType value) {
        return new JAXBElement<FlightType>(_Flight_QNAME, FlightType.class, null, value);
    }

}
