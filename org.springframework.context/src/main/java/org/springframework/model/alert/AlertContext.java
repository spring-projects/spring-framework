/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.model.alert;

import java.util.List;
import java.util.Map;

/**
 * A context for adding and getting alerts for display in a user interface.
 * @author Keith Donald
 * @since 3.0
 */
public interface AlertContext {

	/**
	 * Return all alerts in this context indexed by the UI element they are associated with.
	 * @return the message map
	 */
	public Map<String, List<Alert>> getAlerts();
	
	/**
	 * Get all alerts on the UI element provided.
	 * Returns an empty list if no alerts have been added for the element.
	 * Alerts are returned in the order they were added.
	 * @param element the id of the element to lookup alerts against
	 */
	public List<Alert> getAlerts(String element);

	/**
	 * Add an alert to this context.
	 * @param the element this alert is associated with
	 * @param alert the alert to add
	 */
	public void add(String element, Alert alert);

}