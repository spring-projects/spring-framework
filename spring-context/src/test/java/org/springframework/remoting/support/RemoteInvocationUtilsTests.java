/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.remoting.support;

import junit.framework.TestCase;

/**
 * @author Rick Evans
 */
public class RemoteInvocationUtilsTests extends TestCase {

	public void testFillInClientStackTraceIfPossibleSunnyDay() throws Exception {
		try {
			throw new IllegalStateException("Mmm");
		}
		catch (Exception ex) {
			int originalStackTraceLngth = ex.getStackTrace().length;
			RemoteInvocationUtils.fillInClientStackTraceIfPossible(ex);
			assertTrue("Stack trace not being filled in",
					ex.getStackTrace().length > originalStackTraceLngth);
		}
	}

	public void testFillInClientStackTraceIfPossibleWithNullThrowable() throws Exception {
		// just want to ensure that it doesn't bomb
		RemoteInvocationUtils.fillInClientStackTraceIfPossible(null);
	}

}
