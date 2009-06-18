package org.springframework.ui.alert;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ui.alert.Alert;
import org.springframework.ui.alert.Severity;
import org.springframework.ui.alert.support.DefaultAlertContext;

public class DefaultMessageContextTests {
	
	private DefaultAlertContext context;

	@Before
	public void setUp() {
		context = new DefaultAlertContext();
	}
	
	@Test
	public void addAlert() {
		Alert alert = new Alert() {
			public String getElement() {
				return "form.property";
			}

			public String getCode() {
				return "invalidFormat";
			}

			public String getMessage() {
				return "Please enter a value in format yyy-dd-mm";
			}

			public Severity getSeverity() {
				return Severity.ERROR;
			}
		};
		context.add(alert);
		assertEquals(1, context.getAlerts().size());
		assertEquals("invalidFormat", context.getAlerts("form.property").get(0).getCode());
	}
}
