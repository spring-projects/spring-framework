/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util.xml;

import org.apache.commons.logging.Log;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Simple {@code org.xml.sax.ErrorHandler} implementation:
 * logs warnings using the given Commons Logging logger instance,
 * and rethrows errors to discontinue the XML transformation.
 *
 * @author Juergen Hoeller
 * @since 1.2
 */
public class SimpleSaxErrorHandler implements ErrorHandler {

	private final Log logger;


	/**
	 * Create a new SimpleSaxErrorHandler for the given
	 * Commons Logging logger instance.
	 */
	public SimpleSaxErrorHandler(Log logger) {
		this.logger = logger;
	}


	@Override
	public void warning(SAXParseException ex) throws SAXException {
		logger.warn("Ignored XML validation warning", ex);
	}

	@Override
	public void error(SAXParseException ex) throws SAXException {
		throw ex;
	}

	@Override
	public void fatalError(SAXParseException ex) throws SAXException {
		throw ex;
	}

}
