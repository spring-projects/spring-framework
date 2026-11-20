/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.support.xml;

import javax.xml.transform.Result;

/**
 * Interface defining handling involved with providing {@code Result}
 * data for XML input.
 *
 * @author Thomas Risberg
 * @since 2.5.5
 * @see javax.xml.transform.Result
 * @deprecated as of 6.2, in favor of direct {@link java.sql.SQLXML} usage
 */
@Deprecated(since = "6.2")
public interface XmlResultProvider {

	/**
	 * Implementations must implement this method to provide the XML content
	 * for the {@code Result}. Implementations will vary depending on
	 * the {@code Result} implementation used.
	 * @param result the {@code Result} object being used to provide the XML input
	 */
	void provideXml(Result result);

}
