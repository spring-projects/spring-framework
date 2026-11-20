/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.management.ObjectName;

import org.junit.jupiter.api.Test;

import org.springframework.jmx.AbstractJmxTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 */
class CustomEditorConfigurerTests extends AbstractJmxTests {

	private final SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");

	@Override
	protected String getApplicationContextPath() {
		return "org/springframework/jmx/export/customConfigurer.xml";
	}

	@Test
	void datesInApplicationContext() throws Exception {
		DateRange dr = getContext().getBean("dateRange", DateRange.class);

		assertThat(dr.getStartDate()).as("startDate").isEqualTo(getStartDate());
		assertThat(dr.getEndDate()).as("endDate").isEqualTo(getEndDate());
	}

	@Test
	void datesInJmx() throws Exception {
		ObjectName oname = new ObjectName("bean:name=dateRange");

		Date startJmx = (Date) getServer().getAttribute(oname, "StartDate");
		Date endJmx = (Date) getServer().getAttribute(oname, "EndDate");

		assertThat(startJmx).as("startDate").isEqualTo(getStartDate());
		assertThat(endJmx).as("endDate").isEqualTo(getEndDate());
	}

	private Date getStartDate() throws ParseException {
		return df.parse("2004/10/12");
	}

	private Date getEndDate() throws ParseException {
		return df.parse("2004/11/13");
	}

}
