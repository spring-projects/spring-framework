/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.test.context.junit;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.UseTechnicalNames;
import org.junit.runner.RunWith;

/**
 * JUnit 4 based test suite for tests that involve the Spring TestContext
 * Framework and JUnit Jupiter (i.e., JUnit 5's programming model).
 *
 * <p>This class intentionally does not reside in the "jupiter" package
 * so that the entire "jupiter" package can be excluded from the Gradle
 * build. This class is therefore responsible for executing all JUnit
 * Jupiter based tests in Spring's official test suite.
 *
 * <h3>Logging Configuration</h3>
 *
 * <p>In order for our log4j2 configuration to be used in an IDE, you must
 * set the following system property before running any tests &mdash; for
 * example, in <em>Run Configurations</em> in Eclipse.
 *
 * <pre style="code">
 * -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager
 * </pre>
 *
 * @author Sam Brannen
 * @since 5.0
 */
@RunWith(JUnitPlatform.class)
@IncludeEngines("junit-jupiter")
@SelectPackages("org.springframework.test.context.junit.jupiter")
@IncludeClassNamePatterns("^.*TestCase$")
@UseTechnicalNames
public class SpringJUnitJupiterTestSuite {
}
