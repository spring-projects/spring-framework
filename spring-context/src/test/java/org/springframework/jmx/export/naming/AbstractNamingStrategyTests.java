/*
 * Copyright 2002-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.jmx.export.naming;

import org.junit.Test;

import javax.management.ObjectName;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 */
public abstract class AbstractNamingStrategyTests {

	@Test
	public void naming() throws Exception {
		ObjectNamingStrategy strat = getStrategy();
		ObjectName objectName = strat.getObjectName(getManagedResource(), getKey());
		assertEquals(objectName.getCanonicalName(), getCorrectObjectName());
	}

	protected abstract ObjectNamingStrategy getStrategy() throws Exception;

	protected abstract Object getManagedResource() throws Exception;

	protected abstract String getKey();

	protected abstract String getCorrectObjectName();

}
