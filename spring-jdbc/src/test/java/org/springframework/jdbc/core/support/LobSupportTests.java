/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.jdbc.core.support;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.LobRetrievalFailureException;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;

/**
 * @author Alef Arendsen
 */
public class LobSupportTests extends TestCase {

	public void testCreatingPreparedStatementCallback() throws SQLException {
		// - return value should match
		// - lob creator should be closed
		// - set return value should be called
		// - execute update should be called

		MockControl lobHandlerControl = MockControl.createControl(LobHandler.class);
		LobHandler handler = (LobHandler)lobHandlerControl.getMock();

		MockControl lobCreatorControl = MockControl.createControl(LobCreator.class);
		LobCreator creator = (LobCreator)lobCreatorControl.getMock();

		MockControl psControl = MockControl.createControl(PreparedStatement.class);
		PreparedStatement ps = (PreparedStatement)psControl.getMock();

		handler.getLobCreator();
		lobHandlerControl.setReturnValue(creator);
		ps.executeUpdate();
		psControl.setReturnValue(3);
		creator.close();

		lobHandlerControl.replay();
		lobCreatorControl.replay();
		psControl.replay();

		class SetValuesCalled {
			boolean b = false;
		}

		final SetValuesCalled svc = new SetValuesCalled();

		AbstractLobCreatingPreparedStatementCallback psc =
			new AbstractLobCreatingPreparedStatementCallback(handler) {

			protected void setValues(PreparedStatement ps, LobCreator lobCreator)
					throws SQLException, DataAccessException {
				svc.b = true;
			}
		};

		assertEquals(new Integer(3), psc.doInPreparedStatement(ps));

		lobHandlerControl.verify();
		lobCreatorControl.verify();
		psControl.verify();
		assertTrue(svc.b);
	}

	public void testAbstractLobStreamingResultSetExtractorNoRows() throws SQLException {
		MockControl rsetControl = MockControl.createControl(ResultSet.class);
		ResultSet rset = (ResultSet)rsetControl.getMock();
		rset.next();
		rsetControl.setReturnValue(false);
		rsetControl.replay();

		AbstractLobStreamingResultSetExtractor lobRse = getResultSetExtractor(false);
		try {
			lobRse.extractData(rset);
			fail("IncorrectResultSizeDataAccessException should have been thrown");
		} catch (IncorrectResultSizeDataAccessException e) {
			// expected
		}
	}

	public void testAbstractLobStreamingResultSetExtractorOneRow() throws SQLException {
		MockControl rsetControl = MockControl.createControl(ResultSet.class);
		ResultSet rset = (ResultSet)rsetControl.getMock();
		rset.next();
		rsetControl.setReturnValue(true);
		// see if it's called
		rset.clearWarnings();
		rset.next();
		rsetControl.setReturnValue(false);
		rsetControl.replay();

		AbstractLobStreamingResultSetExtractor lobRse = getResultSetExtractor(false);
		lobRse.extractData(rset);
		rsetControl.verify();
	}

	public void testAbstractLobStreamingResultSetExtractorMultipleRows() throws SQLException {
		MockControl rsetControl = MockControl.createControl(ResultSet.class);
		ResultSet rset = (ResultSet)rsetControl.getMock();
		rset.next();
		rsetControl.setReturnValue(true);
		// see if it's called
		rset.clearWarnings();
		rset.next();
		rsetControl.setReturnValue(true);
		rsetControl.replay();

		AbstractLobStreamingResultSetExtractor lobRse = getResultSetExtractor(false);
		try {
			lobRse.extractData(rset);
			fail("IncorrectResultSizeDataAccessException should have been thrown");
		} catch (IncorrectResultSizeDataAccessException e) {
			// expected
		}
		rsetControl.verify();
	}

	public void testAbstractLobStreamingResultSetExtractorCorrectException() throws SQLException {
		MockControl rsetControl = MockControl.createControl(ResultSet.class);
		ResultSet rset = (ResultSet)rsetControl.getMock();
		rset.next();
		rsetControl.setReturnValue(true);
		rsetControl.replay();

		AbstractLobStreamingResultSetExtractor lobRse = getResultSetExtractor(true);
		try {
			lobRse.extractData(rset);
			fail("LobRetrievalFailureException should have been thrown");
		} catch (LobRetrievalFailureException e) {
			// expected
		}
		rsetControl.verify();
	}

	private AbstractLobStreamingResultSetExtractor getResultSetExtractor(final boolean ex) {
		AbstractLobStreamingResultSetExtractor lobRse = new AbstractLobStreamingResultSetExtractor() {
			protected void streamData(ResultSet rs) throws SQLException, IOException {
				if (ex) {
					throw new IOException();
				}
				else {
					rs.clearWarnings();
				}
			}
		};
		return lobRse;
	}
}
