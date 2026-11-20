/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.support.lob;

import java.io.InputStream;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jspecify.annotations.Nullable;

/**
 * Abstract base class for {@link LobHandler} implementations.
 *
 * <p>Implements all accessor methods for column names through a column lookup
 * and delegating to the corresponding accessor that takes a column index.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see java.sql.ResultSet#findColumn
 * @deprecated as of 6.2, in favor of {@link org.springframework.jdbc.core.support.SqlBinaryValue}
 * and {@link org.springframework.jdbc.core.support.SqlCharacterValue}
 */
@Deprecated(since = "6.2")
public abstract class AbstractLobHandler implements LobHandler {

	@Override
	public byte @Nullable [] getBlobAsBytes(ResultSet rs, String columnName) throws SQLException {
		return getBlobAsBytes(rs, rs.findColumn(columnName));
	}

	@Override
	public @Nullable InputStream getBlobAsBinaryStream(ResultSet rs, String columnName) throws SQLException {
		return getBlobAsBinaryStream(rs, rs.findColumn(columnName));
	}

	@Override
	public @Nullable String getClobAsString(ResultSet rs, String columnName) throws SQLException {
		return getClobAsString(rs, rs.findColumn(columnName));
	}

	@Override
	public @Nullable InputStream getClobAsAsciiStream(ResultSet rs, String columnName) throws SQLException {
		return getClobAsAsciiStream(rs, rs.findColumn(columnName));
	}

	@Override
	public Reader getClobAsCharacterStream(ResultSet rs, String columnName) throws SQLException {
		return getClobAsCharacterStream(rs, rs.findColumn(columnName));
	}

}
