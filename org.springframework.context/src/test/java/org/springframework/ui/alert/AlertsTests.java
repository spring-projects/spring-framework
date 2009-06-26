package org.springframework.ui.alert;

import static org.junit.Assert.assertEquals;
import static org.springframework.ui.alert.Alerts.error;
import static org.springframework.ui.alert.Alerts.fatal;
import static org.springframework.ui.alert.Alerts.info;
import static org.springframework.ui.alert.Alerts.warning;

import org.junit.Test;

public class AlertsTests {
	
	@Test
	public void testFactoryMethods() {
		Alert a1 = info("alert 1");
		assertEquals(Severity.INFO, a1.getSeverity());
		assertEquals("alert 1", a1.getMessage());

		Alert a2 = warning("alert 2");
		assertEquals(Severity.WARNING, a2.getSeverity());
		assertEquals("alert 2", a2.getMessage());

		Alert a3 = error("alert 3");
		assertEquals(Severity.ERROR, a3.getSeverity());
		assertEquals("alert 3", a3.getMessage());

		Alert a4 = fatal("alert 4");
		assertEquals(Severity.FATAL, a4.getSeverity());
		assertEquals("alert 4", a4.getMessage());

	}
}
