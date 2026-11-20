/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.transaction.ejb.dao;

/**
 * Test DAO for EJB transaction support in the TestContext framework.
 *
 * @author Xavier Detant
 * @author Sam Brannen
 * @since 4.0.1
 */
public interface TestEntityDao {

	int getCount(String name);

	int incrementCount(String name);

}
