/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.support;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
