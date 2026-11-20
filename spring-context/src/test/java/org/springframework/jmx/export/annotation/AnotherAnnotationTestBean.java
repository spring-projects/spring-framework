/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.annotation;

import org.springframework.jmx.support.MetricType;

/**
 * @author Stephane Nicoll
 */
@ManagedResource(objectName = "bean:name=interfaceTestBean", description = "My Managed Bean")
public interface AnotherAnnotationTestBean {

	@ManagedOperation(description = "invoke foo")
	void foo();

	@ManagedAttribute(description = "Bar description")
	String getBar();

	void setBar(String bar);

	@ManagedMetric(description = "a metric", metricType = MetricType.COUNTER)
	int getCacheEntries();

}
