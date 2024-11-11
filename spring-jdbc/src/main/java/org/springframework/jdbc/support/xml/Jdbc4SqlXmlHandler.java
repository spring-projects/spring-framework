/*
 * Copyright 2002-2024 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.lang.Nullable;

/**
 * Default implementation of the {@link SqlXmlHandler} interface.
 * Provides database-specific implementations for storing and
 * retrieving XML documents to and from fields in a database,
 * relying on the JDBC 4.0 {@code java.sql.SQLXML} facility.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5.6
 * @see java.sql.SQLXML
 * @see java.sql.ResultSet#getSQLXML
 * @see java.sql.PreparedStatement#setSQLXML
 * @deprecated as of 6.2, in favor of direct {@link ResultSet#getSQLXML} and
 * {@link Connection#createSQLXML()} usage, possibly in combination with a
 * custom {@link org.springframework.jdbc.support.SqlValue} implementation
 */
@Deprecated(since = "6.2")
public class Jdbc4SqlXmlHandler implements SqlXmlHandler {

	//-------------------------------------------------------------------------
	// Convenience methods for accessing XML content
	//-------------------------------------------------------------------------

	@Override
	@Nullable
	public String getXmlAsString(ResultSet rs, String columnName) throws SQLException {
		SQLXML xmlObject = rs.getSQLXML(columnName);
		return (xmlObject != null ? xmlObject.getString() : null);
	}

	@Override
	@Nullable
	public String getXmlAsString(ResultSet rs, int columnIndex) throws SQLException {
		SQLXML xmlObject = rs.getSQLXML(columnIndex);
		return (xmlObject != null ? xmlObject.getString() : null);
	}

	@Override
	@Nullable
	public InputStream getXmlAsBinaryStream(ResultSet rs, String columnName) throws SQLException {
		SQLXML xmlObject = rs.getSQLXML(columnName);
		return (xmlObject != null ? xmlObject.getBinaryStream() : null);
	}

	@Override
	@Nullable
	public InputStream getXmlAsBinaryStream(ResultSet rs, int columnIndex) throws SQLException {
		SQLXML xmlObject = rs.getSQLXML(columnIndex);
		return (xmlObject != null ? xmlObject.getBinaryStream() : null);
	}

	@Override
	@Nullable
	public Reader getXmlAsCharacterStream(ResultSet rs, String columnName) throws SQLException {
		SQLXML xmlObject = rs.getSQLXML(columnName);
		return (xmlObject != null ? xmlObject.getCharacterStream() : null);
	}

	@Override
	@Nullable
	public Reader getXmlAsCharacterStream(ResultSet rs, int columnIndex) throws SQLException {
		SQLXML xmlObject = rs.getSQLXML(columnIndex);
		return (xmlObject != null ? xmlObject.getCharacterStream() : null);
	}

	@Override
	@Nullable
	public Source getXmlAsSource(ResultSet rs, String columnName, @Nullable Class<? extends Source> sourceClass)
			throws SQLException {

		SQLXML xmlObject = rs.getSQLXML(columnName);
		if (xmlObject == null) {
			return null;
		}
		return (sourceClass != null ? xmlObject.getSource(sourceClass) : xmlObject.getSource(DOMSource.class));
	}

	@Override
	@Nullable
	public Source getXmlAsSource(ResultSet rs, int columnIndex, @Nullable Class<? extends Source> sourceClass)
			throws SQLException {

		SQLXML xmlObject = rs.getSQLXML(columnIndex);
		if (xmlObject == null) {
			return null;
		}
		return (sourceClass != null ? xmlObject.getSource(sourceClass) : xmlObject.getSource(DOMSource.class));
	}


	//-------------------------------------------------------------------------
	// Convenience methods for building XML content
	//-------------------------------------------------------------------------

	@Override
	public SqlXmlValue newSqlXmlValue(final String value) {
		return new AbstractJdbc4SqlXmlValue() {
			@Override
			protected void provideXml(SQLXML xmlObject) throws SQLException, IOException {
				xmlObject.setString(value);
			}
		};
	}

	@Override
	public SqlXmlValue newSqlXmlValue(final XmlBinaryStreamProvider provider) {
		return new AbstractJdbc4SqlXmlValue() {
			@Override
			protected void provideXml(SQLXML xmlObject) throws SQLException, IOException {
				provider.provideXml(xmlObject.setBinaryStream());
			}
		};
	}

	@Override
	public SqlXmlValue newSqlXmlValue(final XmlCharacterStreamProvider provider) {
		return new AbstractJdbc4SqlXmlValue() {
			@Override
			protected void provideXml(SQLXML xmlObject) throws SQLException, IOException {
				provider.provideXml(xmlObject.setCharacterStream());
			}
		};
	}

	@Override
	public SqlXmlValue newSqlXmlValue(final Class<? extends Result> resultClass, final XmlResultProvider provider) {
		return new AbstractJdbc4SqlXmlValue() {
			@Override
			protected void provideXml(SQLXML xmlObject) throws SQLException, IOException {
				provider.provideXml(xmlObject.setResult(resultClass));
			}
		};
	}

	@Override
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
	private abstract static class AbstractJdbc4SqlXmlValue implements SqlXmlValue {

		@Nullable
		private SQLXML xmlObject;

		@Override
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

		@Override
		public void cleanup() {
			if (this.xmlObject != null) {
				try {
					this.xmlObject.free();
				}
				catch (SQLException ex) {
					throw new DataAccessResourceFailureException("Could not free SQLXML object", ex);
				}
			}
		}

		protected abstract void provideXml(SQLXML xmlObject) throws SQLException, IOException;
	}

}
