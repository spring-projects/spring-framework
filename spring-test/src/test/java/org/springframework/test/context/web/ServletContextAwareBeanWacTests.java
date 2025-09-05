/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.context.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.testkit.engine.EngineTestKit;

import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Introduced to investigate claims in SPR-11145.
 *
 * <p>Yes, this test class does in fact use JUnit to run JUnit. ;)
 *
 * @author Sam Brannen
 * @since 4.0.2
 */
@ExtendWith(SpringExtension.class)
class ServletContextAwareBeanWacTests {

	@Test
	void ensureServletContextAwareBeanIsProcessedProperlyWhenExecutingJUnitManually() {
		EngineTestKit.engine("junit-jupiter")
				.selectors(selectClass(BasicAnnotationConfigWacTests.class))
				.execute()
				.testEvents()
				.assertStatistics(stats -> stats.started(3).succeeded(3).failed(0));
	}

}
