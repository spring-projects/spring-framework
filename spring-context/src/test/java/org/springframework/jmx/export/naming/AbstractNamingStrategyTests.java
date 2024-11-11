/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.jmx.export.naming;

import javax.management.ObjectName;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 */
public abstract class AbstractNamingStrategyTests {

	@Test
	void naming() throws Exception {
		ObjectNamingStrategy strat = getStrategy();
		ObjectName objectName = strat.getObjectName(getManagedResource(), getKey());
		assertThat(getCorrectObjectName()).isEqualTo(objectName.getCanonicalName());
	}

	protected abstract ObjectNamingStrategy getStrategy() throws Exception;

	protected abstract Object getManagedResource();

	protected abstract String getKey();

	protected abstract String getCorrectObjectName();

}
