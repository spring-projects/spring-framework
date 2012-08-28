/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.context.junit4.spr8849;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Test suite to investigate claims raised in
 * <a href="https://jira.springsource.org/browse/SPR-8849">SPR-8849</a>.
 *
 * <p>By using a SpEL expression to generate a random {@code id} for the
 * embedded database (see {@code datasource-config.xml}), we ensure that each
 * {@code ApplicationContext} that imports the common configuration will create
 * an embedded database with a unique name (since the {@code id} is used as the
 * database name within
 * {@link org.springframework.jdbc.config.EmbeddedDatabaseBeanDefinitionParser#useIdAsDatabaseNameIfGiven()}).
 *
 * <p>To reproduce the problem mentioned in SPEX-8849, change the {@code id} of
 * the embedded database in {@code datasource-config.xml} to "dataSource" (or
 * anything else that is not random) and run this <em>suite</em>.
 *
 * @author Sam Brannen
 * @since 3.2
 */
@RunWith(Suite.class)
@SuiteClasses({ TestClass1.class, TestClass2.class })
public class Spr8849Tests {

}
