/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
