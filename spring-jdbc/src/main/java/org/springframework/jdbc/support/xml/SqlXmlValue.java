/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.support.xml;

import org.springframework.jdbc.support.SqlValue;

/**
 * Subinterface of {@link org.springframework.jdbc.support.SqlValue}
 * that specifically indicates passing in XML data to a specified column.
 *
 * @author Thomas Risberg
 * @since 2.5.5
 * @see org.springframework.jdbc.support.SqlValue
 * @deprecated as of 6.2, in favor of a direct {@link SqlValue} implementation
 */
@Deprecated(since = "6.2")
public interface SqlXmlValue extends SqlValue {

}
