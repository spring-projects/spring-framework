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
package org.springframework.ui.alert.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.style.ToStringCreator;
import org.springframework.ui.alert.Alert;
import org.springframework.ui.alert.AlertContext;
import org.springframework.util.CachingMapDecorator;

/**
 * The default alert context implementation.
 * @author Keith Donald
 * @since 3.0
 */
public class DefaultAlertContext implements AlertContext {

	@SuppressWarnings("serial")
	private Map<String, List<Alert>> alerts = new CachingMapDecorator<String, List<Alert>>(new LinkedHashMap<String, List<Alert>>()) {
		protected List<Alert> create(String element) {
			return new ArrayList<Alert>();
		}
	};

	// implementing AlertContext

	public Map<String, List<Alert>> getAlerts() {
		return Collections.unmodifiableMap(alerts);
	}
	
	public List<Alert> getAlerts(String element) {
		List<Alert> messages = alerts.get(element);
		if (messages.isEmpty()) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(messages);
	}

	public void add(String element, Alert alert) {
		List<Alert> alerts = this.alerts.get(element);
		alerts.add(alert);
	}

	public String toString() {
		return new ToStringCreator(this).append("alerts", alerts).toString();
	}

}