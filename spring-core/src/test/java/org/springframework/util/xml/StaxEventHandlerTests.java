/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util.xml;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;

/**
 * @author Arjen Poutsma
 */
class StaxEventHandlerTests extends AbstractStaxHandlerTests {

	@Override
	protected AbstractStaxHandler createStaxHandler(Result result) throws XMLStreamException {
		XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
		XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(result);
		return new StaxEventHandler(eventWriter);
	}

}
