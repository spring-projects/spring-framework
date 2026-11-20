/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.oxm.mime;

import java.io.IOException;

import javax.xml.transform.Result;

import org.jspecify.annotations.Nullable;

import org.springframework.oxm.Marshaller;
import org.springframework.oxm.XmlMappingException;

/**
 * Subinterface of {@link Marshaller} that can use MIME attachments to optimize
 * storage of binary data. Attachments can be added as MTOM, XOP, or SwA.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see <a href="https://www.w3.org/TR/2004/WD-soap12-mtom-20040608/">SOAP Message Transmission Optimization Mechanism</a>
 * @see <a href="https://www.w3.org/TR/2005/REC-xop10-20050125/">XML-binary Optimized Packaging</a>
 */
public interface MimeMarshaller extends Marshaller {

	/**
	 * Marshals the object graph with the given root into the provided {@link Result},
	 * writing binary data to a {@link MimeContainer}.
	 * @param graph the root of the object graph to marshal
	 * @param result the result to marshal to
	 * @param mimeContainer the MIME container to write extracted binary content to
	 * @throws XmlMappingException if the given object cannot be marshalled to the result
	 * @throws IOException if an I/O exception occurs
	 */
	void marshal(Object graph, Result result, @Nullable MimeContainer mimeContainer) throws XmlMappingException, IOException;

}
