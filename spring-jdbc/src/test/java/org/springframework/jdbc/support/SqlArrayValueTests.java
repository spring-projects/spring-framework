/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.jdbc.support;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;

/**
 * Tests for {@link SqlArrayValue}.
 *
 * @author Philippe Marschall
 * @since 6.1
 */
class SqlArrayValueTests {

	private final Connection con = mock();

	private final PreparedStatement ps = mock();

	private final Array arr = mock();

	private final Object[] elements = new Object[] {1, 2, 3};

	private final SqlArrayValue sqlArrayValue = new SqlArrayValue("smallint", elements);


	@Test
	void setValue() throws SQLException {
		given(this.ps.getConnection()).willReturn(this.con);
		given(this.con.createArrayOf("smallint", elements)).willReturn(this.arr);

		int paramIndex = 42;
		this.sqlArrayValue.setValue(this.ps, paramIndex);
		verify(ps).setArray(paramIndex, arr);

		this.sqlArrayValue.cleanup();
		verify(this.arr).free();
	}

}
