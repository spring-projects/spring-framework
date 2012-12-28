/*
 * Copyright 2002-2005 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Date;

import junit.framework.TestCase;
import org.easymock.ArgumentsMatcher;
import org.easymock.MockControl;

import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;

/**
 * Test cases for the sql lob value:
 *
 * BLOB:
 *   1. Types.BLOB: setBlobAsBytes (byte[])
 *   2. String: setBlobAsBytes (byte[])
 *   3. else: IllegalArgumentException
 *
 * CLOB:
 *   4. String or NULL: setClobAsString (String)
 *   5. InputStream: setClobAsAsciiStream (InputStream)
 *   6. Reader: setClobAsCharacterStream (Reader)
 *   7. else: IllegalArgumentException
 *
 * @author Alef Arendsen
 */
public class SqlLobValueTests extends TestCase {

	private MockControl psControl;
	private PreparedStatement ps;

	private MockControl lobHandlerControl;
	private LobHandler handler;

	private MockControl lobCreatorControl;
	private LobCreator creator;

	public void setUp() {
		//	create preparedstatement
		psControl = MockControl.createControl(PreparedStatement.class);
		ps = (PreparedStatement) psControl.getMock();

		// create handler controler
		lobHandlerControl = MockControl.createControl(LobHandler.class);
		handler = (LobHandler) lobHandlerControl.getMock();

		// create creator control
		lobCreatorControl = MockControl.createControl(LobCreator.class);
		creator = (LobCreator) lobCreatorControl.getMock();

		// set initial state
		handler.getLobCreator();
		lobHandlerControl.setReturnValue(creator);
	}

	private void replay() {
		psControl.replay();
		lobHandlerControl.replay();
		lobCreatorControl.replay();
	}

	public void test1() throws SQLException {
		byte[] testBytes = "Bla".getBytes();
		creator.setBlobAsBytes(ps, 1, testBytes);
		replay();
		SqlLobValue lob = new SqlLobValue(testBytes, handler);
		lob.setTypeValue(ps, 1, Types.BLOB, "test");
		lobHandlerControl.verify();
		lobCreatorControl.verify();
	}

	public void test2() throws SQLException {
		String testString = "Bla";

		creator.setBlobAsBytes(ps, 1, testString.getBytes());
		// set a matcher to match the byte array!
		lobCreatorControl.setMatcher(new ArgumentsMatcher() {
			public boolean matches(Object[] arg0, Object[] arg1) {
				byte[] one = (byte[]) arg0[2];
				byte[] two = (byte[]) arg1[2];
				return Arrays.equals(one, two);
			}
			public String toString(Object[] arg0) {
				return "bla";
			}
		});

		replay();

		SqlLobValue lob = new SqlLobValue(testString, handler);
		lob.setTypeValue(ps, 1, Types.BLOB, "test");
		lobHandlerControl.verify();
		lobCreatorControl.verify();

	}

	public void test3()
	throws SQLException {

		Date testContent = new Date();

		SqlLobValue lob =
			new SqlLobValue(new InputStreamReader(new ByteArrayInputStream("Bla".getBytes())), 12);
		try {
			lob.setTypeValue(ps, 1, Types.BLOB, "test");
			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	public void test4() throws SQLException {
		String testContent = "Bla";
		creator.setClobAsString(ps, 1, testContent);

		replay();

		SqlLobValue lob = new SqlLobValue(testContent, handler);
		lob.setTypeValue(ps, 1, Types.CLOB, "test");
		lobHandlerControl.verify();
		lobCreatorControl.verify();
	}

	public void test5() throws SQLException {
		byte[] testContent = "Bla".getBytes();
		ByteArrayInputStream bais = new ByteArrayInputStream(testContent);
		creator.setClobAsAsciiStream(ps, 1, bais, 3);
		lobCreatorControl.setMatcher(new ArgumentsMatcher() {
			public boolean matches(Object[] arg0, Object[] arg1) {
				// for now, match always
				return true;
			}
			public String toString(Object[] arg0) {
				return null;
			}
		});

		replay();

		SqlLobValue lob = new SqlLobValue(new ByteArrayInputStream(testContent), 3, handler);
		lob.setTypeValue(ps, 1, Types.CLOB, "test");
		lobHandlerControl.verify();
		lobCreatorControl.verify();
	}

	public void test6()throws SQLException {
		byte[] testContent = "Bla".getBytes();
		ByteArrayInputStream bais = new ByteArrayInputStream(testContent);
		InputStreamReader reader = new InputStreamReader(bais);
		creator.setClobAsCharacterStream(ps, 1, reader, 3);
		lobCreatorControl.setMatcher(new ArgumentsMatcher() {
			public boolean matches(Object[] arg0, Object[] arg1) {
				// for now, match always
				return true;
			}
			public String toString(Object[] arg0) {
				return null;
			}
		});

		replay();

		SqlLobValue lob = new SqlLobValue(reader, 3, handler);
		lob.setTypeValue(ps, 1, Types.CLOB, "test");
		lobHandlerControl.verify();
		lobCreatorControl.verify();

	}

	public void test7() throws SQLException {
		Date testContent = new Date();

		SqlLobValue lob = new SqlLobValue("bla".getBytes());
		try {
			lob.setTypeValue(ps, 1, Types.CLOB, "test");
			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	public void testOtherConstructors() throws SQLException {
		// a bit BS, but we need to test them, as long as they don't throw exceptions

		SqlLobValue lob = new SqlLobValue("bla");
		lob.setTypeValue(ps, 1, Types.CLOB, "test");

		try {
			lob = new SqlLobValue("bla".getBytes());
			lob.setTypeValue(ps, 1, Types.CLOB, "test");
			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException e) {
			// expected
		}

		lob = new SqlLobValue(new ByteArrayInputStream("bla".getBytes()), 3);
		lob.setTypeValue(ps, 1, Types.CLOB, "test");

		lob = new SqlLobValue(new InputStreamReader(
				new ByteArrayInputStream("bla".getBytes())), 3);
		lob.setTypeValue(ps, 1, Types.CLOB, "test");

		// same for BLOB
		lob = new SqlLobValue("bla");
		lob.setTypeValue(ps, 1, Types.BLOB, "test");

		lob = new SqlLobValue("bla".getBytes());
		lob.setTypeValue(ps, 1, Types.BLOB, "test");

		lob = new SqlLobValue(new ByteArrayInputStream("bla".getBytes()), 3);
		lob.setTypeValue(ps, 1, Types.BLOB, "test");

		lob = new SqlLobValue(new InputStreamReader(
				new ByteArrayInputStream("bla".getBytes())), 3);

		try {
			lob.setTypeValue(ps, 1, Types.BLOB, "test");
			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	public void testCorrectCleanup()  throws SQLException {
		creator.setClobAsString(ps, 1, "Bla");
		creator.close();

		replay();
		SqlLobValue lob = new SqlLobValue("Bla", handler);
		lob.setTypeValue(ps, 1, Types.CLOB, "test");
		lob.cleanup();

		lobCreatorControl.verify();
	}

	public void testOtherSqlType() throws SQLException {
		replay();
		SqlLobValue lob = new SqlLobValue("Bla", handler);
		try {
			lob.setTypeValue(ps, 1, Types.SMALLINT, "test");
			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

}
