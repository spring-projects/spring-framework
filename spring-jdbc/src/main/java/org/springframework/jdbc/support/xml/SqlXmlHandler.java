/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.support.xml;

import java.io.InputStream;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;

import org.w3c.dom.Document;

import org.springframework.lang.Nullable;

/**
 * Abstraction for handling XML fields in specific databases. Its main purpose
 * is to isolate database-specific handling of XML stored in the database.
 *
 * <p>JDBC 4.0 introduces the new data type {@code java.sql.SQLXML}
 * but most databases and their drivers currently rely on database-specific
 * data types and features.
 *
 * <p>Provides accessor methods for XML fields and acts as factory for
 * {@link SqlXmlValue} instances.
 *
 * @author Thomas Risberg
 * @since 2.5.5
 * @see Jdbc4SqlXmlHandler
 * @see java.sql.SQLXML
 * @see java.sql.ResultSet#getSQLXML
 * @see java.sql.PreparedStatement#setSQLXML
 */
public interface SqlXmlHandler {

	//-------------------------------------------------------------------------
	// Convenience methods for accessing XML content
	//-------------------------------------------------------------------------

	/**
	 * Retrieve the given column as String from the given ResultSet.
	 * <p>Might simply invoke {@code ResultSet.getString} or work with
	 * {@code SQLXML} or database-specific classes depending on the
	 * database and driver.
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnName the column name to use
	 * @return the content as String, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getString
	 * @see java.sql.ResultSet#getSQLXML
	 */
	@Nullable
	String getXmlAsString(ResultSet rs, String columnName) throws SQLException;

	/**
	 * Retrieve the given column as String from the given ResultSet.
	 * <p>Might simply invoke {@code ResultSet.getString} or work with
	 * {@code SQLXML} or database-specific classes depending on the
	 * database and driver.
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnIndex the column index to use
	 * @return the content as String, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getString
	 * @see java.sql.ResultSet#getSQLXML
	 */
	@Nullable
	String getXmlAsString(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * Retrieve the given column as binary stream from the given ResultSet.
	 * <p>Might simply invoke {@code ResultSet.getAsciiStream} or work with
	 * {@code SQLXML} or database-specific classes depending on the
	 * database and driver.
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnName the column name to use
	 * @return the content as a binary stream, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getSQLXML
	 * @see java.sql.SQLXML#getBinaryStream
	 */
	@Nullable
	InputStream getXmlAsBinaryStream(ResultSet rs, String columnName) throws SQLException;

	/**
	 * Retrieve the given column as binary stream from the given ResultSet.
	 * <p>Might simply invoke {@code ResultSet.getAsciiStream} or work with
	 * {@code SQLXML} or database-specific classes depending on the
	 * database and driver.
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnIndex the column index to use
	 * @return the content as binary stream, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getSQLXML
	 * @see java.sql.SQLXML#getBinaryStream
	 */
	@Nullable
	InputStream getXmlAsBinaryStream(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * Retrieve the given column as character stream from the given ResultSet.
	 * <p>Might simply invoke {@code ResultSet.getCharacterStream} or work with
	 * {@code SQLXML} or database-specific classes depending on the
	 * database and driver.
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnName the column name to use
	 * @return the content as character stream, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getSQLXML
	 * @see java.sql.SQLXML#getCharacterStream
	 */
	@Nullable
	Reader getXmlAsCharacterStream(ResultSet rs, String columnName) throws SQLException;

	/**
	 * Retrieve the given column as character stream from the given ResultSet.
	 * <p>Might simply invoke {@code ResultSet.getCharacterStream} or work with
	 * {@code SQLXML} or database-specific classes depending on the
	 * database and driver.
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnIndex the column index to use
	 * @return the content as character stream, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getSQLXML
	 * @see java.sql.SQLXML#getCharacterStream
	 */
	@Nullable
	Reader getXmlAsCharacterStream(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * Retrieve the given column as Source implemented using the specified source class
	 * from the given ResultSet.
	 * <p>Might work with {@code SQLXML} or database-specific classes depending
	 * on the database and driver.
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnName the column name to use
	 * @param sourceClass the implementation class to be used
	 * @return the content as character stream, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getSQLXML
	 * @see java.sql.SQLXML#getSource
	 */
	@Nullable
	Source getXmlAsSource(ResultSet rs, String columnName, @Nullable Class<? extends Source> sourceClass) throws SQLException;

	/**
	 * Retrieve the given column as Source implemented using the specified source class
	 * from the given ResultSet.
	 * <p>Might work with {@code SQLXML} or database-specific classes depending
	 * on the database and driver.
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnIndex the column index to use
	 * @param sourceClass the implementation class to be used
	 * @return the content as character stream, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getSQLXML
	 * @see java.sql.SQLXML#getSource
	 */
	@Nullable
	Source getXmlAsSource(ResultSet rs, int columnIndex, @Nullable Class<? extends Source> sourceClass) throws SQLException;


	//-------------------------------------------------------------------------
	// Convenience methods for building XML content
	//-------------------------------------------------------------------------

	/**
	 * Create a {@code SqlXmlValue} instance for the given XML data,
	 * as supported by the underlying JDBC driver.
	 * @param value the XML String value providing XML data
	 * @return the implementation specific instance
	 * @see SqlXmlValue
	 * @see java.sql.SQLXML#setString(String)
	 */
	SqlXmlValue newSqlXmlValue(String value);

	/**
	 * Create a {@code SqlXmlValue} instance for the given XML data,
	 * as supported by the underlying JDBC driver.
	 * @param provider the {@code XmlBinaryStreamProvider} providing XML data
	 * @return the implementation specific instance
	 * @see SqlXmlValue
	 * @see java.sql.SQLXML#setBinaryStream()
	 */
	SqlXmlValue newSqlXmlValue(XmlBinaryStreamProvider provider);

	/**
	 * Create a {@code SqlXmlValue} instance for the given XML data,
	 * as supported by the underlying JDBC driver.
	 * @param provider the {@code XmlCharacterStreamProvider} providing XML data
	 * @return the implementation specific instance
	 * @see SqlXmlValue
	 * @see java.sql.SQLXML#setCharacterStream()
	 */
	SqlXmlValue newSqlXmlValue(XmlCharacterStreamProvider provider);

	/**
	 * Create a {@code SqlXmlValue} instance for the given XML data,
	 * as supported by the underlying JDBC driver.
	 * @param resultClass the Result implementation class to be used
	 * @param provider the {@code XmlResultProvider} that will provide the XML data
	 * @return the implementation specific instance
	 * @see SqlXmlValue
	 * @see java.sql.SQLXML#setResult(Class)
	 */
	SqlXmlValue newSqlXmlValue(Class<? extends Result> resultClass, XmlResultProvider provider);

	/**
	 * Create a {@code SqlXmlValue} instance for the given XML data,
	 * as supported by the underlying JDBC driver.
	 * @param doc the XML Document to be used
	 * @return the implementation specific instance
	 * @see SqlXmlValue
	 */
	SqlXmlValue newSqlXmlValue(Document doc);

}
