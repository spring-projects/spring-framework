/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.test.context.web;

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.springframework.test.context.junit4.TrackingRunListener;

import static org.junit.Assert.*;

/**
 * Introduced to investigate claims in SPR-11145.
 * 
 * <p>
 * Yes, this test class does in fact use JUnit to run JUnit. ;)
 * 
 * @author Sam Brannen
 * @since 4.0.2
 */
public class ServletContextAwareBeanWacTests {

	@Test
	public void ensureServletContextAwareBeanIsProcessedProperlyWhenExecutingJUnitManually() {
		TrackingRunListener listener = new TrackingRunListener();
		JUnitCore junit = new JUnitCore();
		junit.addListener(listener);

		junit.run(BasicAnnotationConfigWacTests.class);

		assertEquals(3, listener.getTestStartedCount());
		assertEquals(3, listener.getTestFinishedCount());
		assertEquals(0, listener.getTestFailureCount());
	}

}
