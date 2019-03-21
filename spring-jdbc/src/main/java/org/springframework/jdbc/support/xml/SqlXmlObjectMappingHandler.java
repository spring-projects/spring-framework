/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.support.xml;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Abstraction for handling XML object mapping to fields in a database.
 *
 * <p>Provides accessor methods for XML fields unmarshalled to an Object,
 * and acts as factory for {@link SqlXmlValue} instances for marshalling
 * purposes.
 *
 * @author Thomas Risberg
 * @since 2.5.5
 * @see java.sql.ResultSet#getSQLXML
 * @see java.sql.SQLXML
 */
public interface SqlXmlObjectMappingHandler extends SqlXmlHandler {

	/**
	 * Retrieve the given column as an object marshalled from the XML data retrieved
	 * from the given ResultSet.
	 * <p>Works with an internal Object to XML Mapping implementation.
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnName the column name to use
	 * @return the content as an Object, or {@code null} in case of SQL NULL
	 * @throws java.sql.SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getSQLXML
	 */
	Object getXmlAsObject(ResultSet rs, String columnName) throws SQLException;

	/**
	 * Retrieve the given column as an object marshalled from the XML data retrieved
	 * from the given ResultSet.
	 * <p>Works with an internal Object to XML Mapping implementation.
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnIndex the column index to use
	 * @return the content as an Object, or {@code null} in case of SQL NULL
	 * @throws java.sql.SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getSQLXML
	 */
	Object getXmlAsObject(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * Get an instance of an {@code SqlXmlValue} implementation to be used together
	 * with the database specific implementation of this {@code SqlXmlObjectMappingHandler}.
	 * @param value the Object to be marshalled to XML
	 * @return the implementation specific instance
	 * @see SqlXmlValue
	 */
	SqlXmlValue newMarshallingSqlXmlValue(Object value);

}
