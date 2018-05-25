package org.springframework.test.context.jdbc;

import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import static org.junit.Assert.assertEquals;

/**
 * Test to verify method level merge of @Sql annotations.
 *
 * @author Dmitry Semukhin
 */
@ContextConfiguration(classes = EmptyDatabaseConfig.class)
@Sql(value = {"schema.sql", "data-add-catbert.sql"})
@DirtiesContext
public class SqlMethodMergeTest extends AbstractTransactionalJUnit4SpringContextTests {

	@Test
	@Sql(value = "data-add-dogbert.sql", mergeMode = Sql.MergeMode.MERGE)
	public void testMerge() {
		assertNumUsers(2);
	}

	protected void assertNumUsers(int expected) {
		assertEquals("Number of rows in the 'user' table.", expected, countRowsInTable("user"));
	}

}
