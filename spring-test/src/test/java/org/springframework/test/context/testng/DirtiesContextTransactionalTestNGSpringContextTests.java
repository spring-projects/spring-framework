/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.test.context.testng;

import static org.springframework.test.transaction.TransactionTestUtils.assertInTransaction;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;

import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;
import org.testng.annotations.Test;

/**
 * <p>
 * TestNG based integration test to assess the claim in <a
 * href="http://opensource.atlassian.com/projects/spring/browse/SPR-3880"
 * target="_blank">SPR-3880</a> that a &quot;context marked dirty using
 * {@link DirtiesContext &#064;DirtiesContext} in [a] TestNG based test is not
 * reloaded in subsequent tests&quot;.
 * </p>
 * <p>
 * After careful analysis, it turns out that the {@link ApplicationContext} was
 * in fact reloaded; however, due to how the test instance was instrumented with
 * the {@link TestContextManager} in {@link AbstractTestNGSpringContextTests},
 * dependency injection was not being performed on the test instance between
 * individual tests. DirtiesContextTransactionalTestNGSpringContextTests
 * therefore verifies the expected behavior and correct semantics.
 * </p>
 *
 * @author Sam Brannen
 * @since 2.5
 */
@ContextConfiguration
public class DirtiesContextTransactionalTestNGSpringContextTests extends AbstractTransactionalTestNGSpringContextTests {

	private ApplicationContext dirtiedApplicationContext;


	private void performCommonAssertions() {
		assertInTransaction(true);
		assertNotNull(super.applicationContext,
			"The application context should have been set due to ApplicationContextAware semantics.");
		assertNotNull(super.jdbcTemplate,
			"The JdbcTemplate should have been created in setDataSource() via DI for the DataSource.");
	}

	@Test
	@DirtiesContext
	public void dirtyContext() {
		performCommonAssertions();
		this.dirtiedApplicationContext = super.applicationContext;
	}

	@Test(dependsOnMethods = { "dirtyContext" })
	public void verifyContextWasDirtied() {
		performCommonAssertions();
		assertNotSame(super.applicationContext, this.dirtiedApplicationContext,
			"The application context should have been 'dirtied'.");
		this.dirtiedApplicationContext = super.applicationContext;
	}

	@Test(dependsOnMethods = { "verifyContextWasDirtied" })
	public void verifyContextWasNotDirtied() {
		assertSame(this.applicationContext, this.dirtiedApplicationContext,
			"The application context should NOT have been 'dirtied'.");
	}

}
