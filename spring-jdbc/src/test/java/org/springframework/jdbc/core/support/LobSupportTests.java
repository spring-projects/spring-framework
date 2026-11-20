/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.support;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.LobRetrievalFailureException;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Alef Arendsen
 */
@SuppressWarnings("deprecation")
class LobSupportTests {

	@Test
	void creatingPreparedStatementCallback() throws SQLException {
		LobHandler handler = mock();
		LobCreator creator = mock();
		PreparedStatement ps = mock();

		given(handler.getLobCreator()).willReturn(creator);
		given(ps.executeUpdate()).willReturn(3);

		class SetValuesCalled {
			boolean b = false;
		}

		final SetValuesCalled svc = new SetValuesCalled();

		AbstractLobCreatingPreparedStatementCallback psc = new AbstractLobCreatingPreparedStatementCallback(handler) {
			@Override
			protected void setValues(PreparedStatement ps, LobCreator lobCreator) {
				svc.b = true;
			}
		};

		assertThat(psc.doInPreparedStatement(ps)).isEqualTo(Integer.valueOf(3));
		assertThat(svc.b).isTrue();
		verify(creator).close();
		verify(handler).getLobCreator();
		verify(ps).executeUpdate();
	}

	@Test
	void abstractLobStreamingResultSetExtractorNoRows() throws SQLException {
		ResultSet rs = mock();
		AbstractLobStreamingResultSetExtractor<Void> lobRse = getResultSetExtractor(false);
		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
				.isThrownBy(() -> lobRse.extractData(rs));
		verify(rs).next();
	}

	@Test
	void abstractLobStreamingResultSetExtractorOneRow() throws SQLException {
		ResultSet rs = mock();
		given(rs.next()).willReturn(true, false);
		AbstractLobStreamingResultSetExtractor<Void> lobRse = getResultSetExtractor(false);
		lobRse.extractData(rs);
		verify(rs).clearWarnings();
	}

	@Test
	void abstractLobStreamingResultSetExtractorMultipleRows() throws SQLException {
		ResultSet rs = mock();
		given(rs.next()).willReturn(true, true, false);
		AbstractLobStreamingResultSetExtractor<Void> lobRse = getResultSetExtractor(false);
		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
				.isThrownBy(() -> lobRse.extractData(rs));
		verify(rs).clearWarnings();
	}

	@Test
	void abstractLobStreamingResultSetExtractorCorrectException() throws SQLException {
		ResultSet rs = mock();
		given(rs.next()).willReturn(true);
		AbstractLobStreamingResultSetExtractor<Void> lobRse = getResultSetExtractor(true);
		assertThatExceptionOfType(LobRetrievalFailureException.class)
				.isThrownBy(() -> lobRse.extractData(rs));
	}

	private AbstractLobStreamingResultSetExtractor<Void> getResultSetExtractor(final boolean ex) {
		return new AbstractLobStreamingResultSetExtractor<>() {
			@Override
			protected void streamData(ResultSet rs) throws SQLException, IOException {
				if (ex) {
					throw new IOException();
				}
				else {
					rs.clearWarnings();
				}
			}
		};
	}

}
