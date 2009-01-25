/*
 * Copyright 2002-2009 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Default implementation of the {@link SqlXmlHandler} interface.
 * Provides database-specific implementations for storing and
 * retrieving XML documents to and from fields in a database,
 * relying on the JDBC 4.0 <code>java.sql.SQLXML</code> facility.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5.6
 * @see java.sql.SQLXML
 * @see java.sql.ResultSet#getSQLXML
 * @see java.sql.PreparedStatement#setSQLXML
 */
public class Jdbc4SqlXmlHandler implements SqlXmlHandler {

	//-------------------------------------------------------------------------
	// Convenience methods for accessing XML content
	//-------------------------------------------------------------------------

	public String getXmlAsString(ResultSet rs, String columnName) throws SQLException {
		return rs.getSQLXML(columnName).getString();
	}

	public String getXmlAsString(ResultSet rs, int columnIndex) throws SQLException {
		return rs.getSQLXML(columnIndex).getString();
	}

	public InputStream getXmlAsBinaryStream(ResultSet rs, String columnName) throws SQLException {
		return rs.getSQLXML(columnName).getBinaryStream();
	}

	public InputStream getXmlAsBinaryStream(ResultSet rs, int columnIndex) throws SQLException {
		return rs.getSQLXML(columnIndex).getBinaryStream();
	}

	public Reader getXmlAsCharacterStream(ResultSet rs, String columnName) throws SQLException {
		return rs.getSQLXML(columnName).getCharacterStream();
	}

	public Reader getXmlAsCharacterStream(ResultSet rs, int columnIndex) throws SQLException {
		return rs.getSQLXML(columnIndex).getCharacterStream();
	}

	@SuppressWarnings("unchecked")
	public Source getXmlAsSource(ResultSet rs, String columnName, Class sourceClass) throws SQLException {
		return rs.getSQLXML(columnName).getSource(sourceClass != null ? sourceClass : DOMSource.class);
	}

	@SuppressWarnings("unchecked")
	public Source getXmlAsSource(ResultSet rs, int columnIndex, Class sourceClass) throws SQLException {
		return rs.getSQLXML(columnIndex).getSource(sourceClass != null ? sourceClass : DOMSource.class);
	}


	//-------------------------------------------------------------------------
	// Convenience methods for building XML content
	//-------------------------------------------------------------------------

	public SqlXmlValue newSqlXmlValue(final String value) {
		return new AbstractJdbc4SqlXmlValue() {
			@Override
			protected void provideXml(SQLXML xmlObject) throws SQLException, IOException {
				xmlObject.setString(value);
			}
		};
	}

	public SqlXmlValue newSqlXmlValue(final XmlBinaryStreamProvider provider) {
		return new AbstractJdbc4SqlXmlValue() {
			@Override
			protected void provideXml(SQLXML xmlObject) throws SQLException, IOException {
				provider.provideXml(xmlObject.setBinaryStream());
			}
		};
	}

	public SqlXmlValue newSqlXmlValue(final XmlCharacterStreamProvider provider) {
		return new AbstractJdbc4SqlXmlValue() {
			@Override
			protected void provideXml(SQLXML xmlObject) throws SQLException, IOException {
				provider.provideXml(xmlObject.setCharacterStream());
			}
		};
	}

	public SqlXmlValue newSqlXmlValue(final Class resultClass, final XmlResultProvider provider) {
		return new AbstractJdbc4SqlXmlValue() {
			@Override
			@SuppressWarnings("unchecked")
			protected void provideXml(SQLXML xmlObject) throws SQLException, IOException {
				provider.provideXml(xmlObject.setResult(resultClass));
			}
		};
	}

	public SqlXmlValue newSqlXmlValue(final Document document) {
		return new AbstractJdbc4SqlXmlValue() {
			@Override
			protected void provideXml(SQLXML xmlObject) throws SQLException, IOException {
				xmlObject.setResult(DOMResult.class).setNode(document);
			}
		};
	}


	/**
	 * Internal base class for {@link SqlXmlValue} implementations.
	 */
	private static abstract class AbstractJdbc4SqlXmlValue implements SqlXmlValue {

		private SQLXML xmlObject;

		public void setValue(PreparedStatement ps, int paramIndex) throws SQLException {
			this.xmlObject = ps.getConnection().createSQLXML();
			try {
				provideXml(this.xmlObject);
			}
			catch (IOException ex) {
				throw new DataAccessResourceFailureException("Failure encountered while providing XML", ex);
			}
			ps.setSQLXML(paramIndex, this.xmlObject);
		}

		public void cleanup() {
			try {
				this.xmlObject.free();
			}
			catch (SQLException ex) {
				throw new DataAccessResourceFailureException("Could not free SQLXML object", ex);
			}
		}

		protected abstract void provideXml(SQLXML xmlObject) throws SQLException, IOException;
	}

}
