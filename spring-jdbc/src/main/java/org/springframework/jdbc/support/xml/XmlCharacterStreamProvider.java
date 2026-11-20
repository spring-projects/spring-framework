/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.support.xml;

import java.io.IOException;
import java.io.Writer;

/**
 * Interface defining handling involved with providing {@code Writer}
 * data for XML input.
 *
 * @author Thomas Risberg
 * @since 2.5.5
 * @see java.io.Writer
 * @deprecated as of 6.2, in favor of direct {@link java.sql.SQLXML} usage
 */
@Deprecated(since = "6.2")
public interface XmlCharacterStreamProvider {

	/**
	 * Implementations must implement this method to provide the XML content
	 * for the {@code Writer}.
	 * @param writer the {@code Writer} object being used to provide the XML input
	 * @throws IOException if an I/O error occurs while providing the XML
	 */
	void provideXml(Writer writer) throws IOException;

}
