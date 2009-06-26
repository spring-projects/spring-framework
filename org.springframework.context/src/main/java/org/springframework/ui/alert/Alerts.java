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
 * A static factory for conveniently constructing Alerts.
 * Usage example:
 * <pre>
 *    import static org.springframework.ui.alert.Alerts;
 *    
 *    public void example() {
 *        info("An info alert");
 *        warning("A warning alert");
 *        error("An error alert");
 *        fatal("A fatal alert");
 *    }
 * </pre>
 * @author Keith Donald
 * @since 3.0
 */
public final class Alerts {

	/**
	 * Creates a new info alert.
	 * @param message the alert message
	 * @return the info alert
	 * @see Severity#INFO
	 */
	public static Alert info(String message) {
		return new GenericAlert(Severity.INFO, message);
	}

	/**
	 * Creates a new warning alert.
	 * @param message the alert message
	 * @return the info alert
	 * @see Severity#WARNING
	 */
	public static Alert warning(String message) {
		return new GenericAlert(Severity.WARNING, message);
	}

	/**
	 * Creates a new error alert.
	 * @param message the alert message
	 * @return the info alert
	 * @see Severity#ERROR
	 */
	public static Alert error(String message) {
		return new GenericAlert(Severity.ERROR, message);
	}

	/**
	 * Creates a new fatal alert.
	 * @param message the alert message
	 * @return the info alert
	 * @see Severity#ERROR
	 */
	public static Alert fatal(String message) {
		return new GenericAlert(Severity.FATAL, message);
	}

	private static class GenericAlert implements Alert {

		private Severity severity;
		
		private String message;

		public GenericAlert(Severity severity, String message) {
			this.severity = severity;
			this.message = message;
		}

		public String getCode() {
			return null;
		}

		public Severity getSeverity() {
			return severity;
		}
		
		public String getMessage() {
			return message;
		}
		
		public String toString() {
			return getSeverity() + ":" + getMessage();
		}
	}

}