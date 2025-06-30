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
