/*
 * AbstractJdbcTests.java
 *
 * Copyright (C) 2002 by Interprise Software.  All rights reserved.
 */
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

package org.springframework.jdbc;

import java.sql.Connection;

import javax.sql.DataSource;

import junit.framework.TestCase;
import org.easymock.MockControl;

/**
 * @author Trevor D. Cook
 */
public abstract class AbstractJdbcTests extends TestCase {

	protected MockControl ctrlDataSource;
	protected DataSource mockDataSource;
	protected MockControl ctrlConnection;
	protected Connection mockConnection;

	/**
	 * Set to true if the user wants verification, indicated
	 * by a call to replay(). We need to make this optional,
	 * otherwise we setUp() will always result in verification failures
	 */
	private boolean shouldVerify;

	protected void setUp() throws Exception {
		this.shouldVerify = false;
		super.setUp();

		ctrlConnection = MockControl.createControl(Connection.class);
		mockConnection = (Connection) ctrlConnection.getMock();
		mockConnection.getMetaData();
		ctrlConnection.setDefaultReturnValue(null);
		mockConnection.close();
		ctrlConnection.setDefaultVoidCallable();

		ctrlDataSource = MockControl.createControl(DataSource.class);
		mockDataSource = (DataSource) ctrlDataSource.getMock();
		mockDataSource.getConnection();
		ctrlDataSource.setDefaultReturnValue(mockConnection);
	}

	protected void replay() {
		ctrlDataSource.replay();
		ctrlConnection.replay();
		this.shouldVerify = true;
	}

	protected void tearDown() throws Exception {
		super.tearDown();

		// we shouldn't verify unless the user called replay()
		if (shouldVerify()) {
			ctrlDataSource.verify();
			//ctrlConnection.verify();
		}
	}

	protected boolean shouldVerify() {
		return this.shouldVerify;
	}

}
