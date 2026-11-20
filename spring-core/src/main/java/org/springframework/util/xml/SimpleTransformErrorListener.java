/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util.xml;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;

/**
 * Simple {@code javax.xml.transform.ErrorListener} implementation:
 * logs warnings using the given Commons Logging logger instance,
 * and rethrows errors to discontinue the XML transformation.
 *
 * @author Juergen Hoeller
 * @since 1.2
 */
public class SimpleTransformErrorListener implements ErrorListener {

	private final Log logger;


	/**
	 * Create a new SimpleTransformErrorListener for the given
	 * Commons Logging logger instance.
	 */
	public SimpleTransformErrorListener(Log logger) {
		this.logger = logger;
	}


	@Override
	public void warning(TransformerException ex) throws TransformerException {
		logger.warn("XSLT transformation warning", ex);
	}

	@Override
	public void error(TransformerException ex) throws TransformerException {
		logger.error("XSLT transformation error", ex);
	}

	@Override
	public void fatalError(TransformerException ex) throws TransformerException {
		throw ex;
	}

}
