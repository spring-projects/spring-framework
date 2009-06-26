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
package org.springframework.ui.alert;

/**
 * Communicates an event of interest to the user.
 * For example, an alert may inform a user of a web application a business rule was violated.
 * @author Keith Donald
 * @since 3.0
 */
public interface Alert {

	/**
	 * The code uniquely identifying this kind of alert; for example, "weakPassword".
	 * May be used as a key to lookup additional alert details.
	 */
	public String getCode();

	/**
	 * The level of impact this alert has on the user.
	 */
	public Severity getSeverity();

	/**
	 * The localized message to display to the user; for example, "Please enter a stronger password".
	 */
	public String getMessage();

}
