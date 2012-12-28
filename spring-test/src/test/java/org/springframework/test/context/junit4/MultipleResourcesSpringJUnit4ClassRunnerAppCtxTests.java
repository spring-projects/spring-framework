/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.test.context.junit4;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.ResourceUtils;

/**
 * Extension of {@link SpringJUnit4ClassRunnerAppCtxTests}, which verifies that
 * we can specify multiple resource locations for our application context, each
 * configured differently.
 * <p>
 * As of Spring 3.0,
 * <code>MultipleResourcesSpringJUnit4ClassRunnerAppCtxTests</code> is also used
 * to verify support for the new <code>value</code> attribute alias for
 * <code>&#064;ContextConfiguration</code>'s <code>locations</code> attribute.
 * </p>
 *
 * @author Sam Brannen
 * @since 2.5
 * @see SpringJUnit4ClassRunnerAppCtxTests
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration( { MultipleResourcesSpringJUnit4ClassRunnerAppCtxTests.CLASSPATH_RESOURCE_PATH,
	MultipleResourcesSpringJUnit4ClassRunnerAppCtxTests.LOCAL_RESOURCE_PATH,
	MultipleResourcesSpringJUnit4ClassRunnerAppCtxTests.ABSOLUTE_RESOURCE_PATH })
public class MultipleResourcesSpringJUnit4ClassRunnerAppCtxTests extends SpringJUnit4ClassRunnerAppCtxTests {

	public static final String CLASSPATH_RESOURCE_PATH = ResourceUtils.CLASSPATH_URL_PREFIX
			+ "/org/springframework/test/context/junit4/MultipleResourcesSpringJUnit4ClassRunnerAppCtxTests-context1.xml";
	public static final String LOCAL_RESOURCE_PATH = "MultipleResourcesSpringJUnit4ClassRunnerAppCtxTests-context2.xml";
	public static final String ABSOLUTE_RESOURCE_PATH = "/org/springframework/test/context/junit4/MultipleResourcesSpringJUnit4ClassRunnerAppCtxTests-context3.xml";

	/* all tests are in the parent class. */
}
