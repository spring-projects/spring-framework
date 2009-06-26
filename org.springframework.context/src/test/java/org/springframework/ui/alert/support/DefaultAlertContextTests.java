package org.springframework.ui.alert.support;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ui.alert.Alert;
import org.springframework.ui.alert.Severity;
import org.springframework.ui.alert.support.DefaultAlertContext;

public class DefaultAlertContextTests {
	
	private DefaultAlertContext context;

	@Before
	public void setUp() {
		context = new DefaultAlertContext();
	}
	
	@Test
	public void addAlert() {
		Alert alert = new Alert() {
			public String getCode() {
				return "invalidFormat";
			}

			public String getMessage() {
				return "Please enter a value in format yyyy-dd-mm";
			}

			public Severity getSeverity() {
				return Severity.ERROR;
			}
		};
		context.add("form.property", alert);
		assertEquals(1, context.getAlerts().size());
		assertEquals("invalidFormat", context.getAlerts("form.property").get(0).getCode());
	}
}
