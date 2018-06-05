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

package org.springframework.jdbc.core.support;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.Test;

import static org.mockito.BDDMockito.*;


/**
 *
 * @author Philippe Marschall
 * @since 04.18.2003
 */
public class SqlArrayValueTests {

	private final Connection con = mock(Connection.class);

	private final PreparedStatement ps = mock(PreparedStatement.class);

	private final Array arr = mock(Array.class);
	
	private final Object[] elements = new Object[] {1, 2, 3};

	private final SqlArrayValue sqlArrayValue = new SqlArrayValue("smallint", elements);

	@Test
	public void setValue() throws SQLException {
		given(this.ps.getConnection()).willReturn(this.con);
		given(this.con.createArrayOf("smallint", elements)).willReturn(this.arr);

		int paramIndex = 42;
		this.sqlArrayValue.setValue(this.ps, paramIndex);
		verify(ps).setArray(paramIndex, arr);
		
		this.sqlArrayValue.cleanup();
		verify(this.arr).free();
	}

}
