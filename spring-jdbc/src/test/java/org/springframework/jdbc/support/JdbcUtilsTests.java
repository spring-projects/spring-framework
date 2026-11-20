/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.support;

import java.sql.Types;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JdbcUtils}.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Ben Blinebury
 */
class JdbcUtilsTests {

	@Test
	void commonDatabaseName() {
		assertThat(JdbcUtils.commonDatabaseName("Oracle")).isEqualTo("Oracle");
		assertThat(JdbcUtils.commonDatabaseName("DB2-for-Spring")).isEqualTo("DB2");
		assertThat(JdbcUtils.commonDatabaseName("Sybase SQL Server")).isEqualTo("Sybase");
		assertThat(JdbcUtils.commonDatabaseName("Adaptive Server Enterprise")).isEqualTo("Sybase");
		assertThat(JdbcUtils.commonDatabaseName("MySQL")).isEqualTo("MySQL");
		assertThat(JdbcUtils.commonDatabaseName("MariaDB")).isEqualTo("MariaDB");
	}

	@Test
	void resolveTypeName() {
		assertThat(JdbcUtils.resolveTypeName(Types.VARCHAR)).isEqualTo("VARCHAR");
		assertThat(JdbcUtils.resolveTypeName(Types.NUMERIC)).isEqualTo("NUMERIC");
		assertThat(JdbcUtils.resolveTypeName(Types.INTEGER)).isEqualTo("INTEGER");
		assertThat(JdbcUtils.resolveTypeName(JdbcUtils.TYPE_UNKNOWN)).isNull();
	}

	@Test
	void convertUnderscoreNameToPropertyName() {
		assertThat(JdbcUtils.convertUnderscoreNameToPropertyName("MY_NAME")).isEqualTo("myName");
		assertThat(JdbcUtils.convertUnderscoreNameToPropertyName("yOUR_nAME")).isEqualTo("yourName");
		assertThat(JdbcUtils.convertUnderscoreNameToPropertyName("a_name")).isEqualTo("AName");
		assertThat(JdbcUtils.convertUnderscoreNameToPropertyName("someone_elses_name")).isEqualTo("someoneElsesName");
	}

}
