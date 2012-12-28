/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.scripting.support;

import static org.mockito.Mockito.mock;
import junit.framework.TestCase;

import org.springframework.beans.factory.BeanFactory;

/**
 * @author Rick Evans
 */
public class RefreshableScriptTargetSourceTests extends TestCase {

	public void testCreateWithNullScriptSource() throws Exception {
		try {
			new RefreshableScriptTargetSource(mock(BeanFactory.class), "a.bean", null, null, false);
			fail("Must have failed when passed a null ScriptSource.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

}
