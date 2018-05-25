package org.springframework.test.context.jdbc;

import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import static org.junit.Assert.assertEquals;

/**
 * Test to verify method level override of @Sql annotations.
 *
 * @author Dmitry Semukhin
 */
@ContextConfiguration(classes = EmptyDatabaseConfig.class)
@Sql(value = {"schema.sql", "data-add-catbert.sql"})
@DirtiesContext
public class SqlMethodOverrideTest extends AbstractTransactionalJUnit4SpringContextTests {

	@Test
	@Sql(value = {"schema.sql", "data.sql", "data-add-dogbert.sql", "data-add-catbert.sql"}, mergeMode = Sql.MergeMode.OVERRIDE)
	public void testMerge() {
		assertNumUsers(3);
	}

	protected void assertNumUsers(int expected) {
		assertEquals("Number of rows in the 'user' table.", expected, countRowsInTable("user"));
	}

}
