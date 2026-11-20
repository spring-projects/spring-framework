/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util.xml;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;

/**
 * @author Arjen Poutsma
 */
class StaxStreamHandlerTests extends AbstractStaxHandlerTests {

	@Override
	protected AbstractStaxHandler createStaxHandler(Result result) throws XMLStreamException {
		XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
		XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(result);
		return new StaxStreamHandler(streamWriter);
	}

}
