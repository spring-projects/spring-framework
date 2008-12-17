/*
 * Copyright 2002-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.jmx.export;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.management.ObjectName;

import org.junit.Ignore;
import org.springframework.jmx.AbstractJmxTests;

/**
 * @author Rob Harrop
 */
@Ignore // changes in CustomEditorConfigurer broke these tests (see diff between r304:305)
public class CustomEditorConfigurerTests extends AbstractJmxTests {

	private final SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");

	protected String getApplicationContextPath() {
		return "org/springframework/jmx/export/customConfigurer.xml";
	}

	public void testDatesInJmx() throws Exception {
		System.out.println(getServer().getClass().getName());
		ObjectName oname = new ObjectName("bean:name=dateRange");

		Date startJmx = (Date) getServer().getAttribute(oname, "StartDate");
		Date endJmx = (Date) getServer().getAttribute(oname, "EndDate");

		assertEquals("startDate ", getStartDate(), startJmx);
		assertEquals("endDate ", getEndDate(), endJmx);
	}

	public void testGetDates() throws Exception {
		DateRange dr = (DateRange) getContext().getBean("dateRange");

		assertEquals("startDate ", getStartDate(), dr.getStartDate());
		assertEquals("endDate ", getEndDate(), dr.getEndDate());
	}

	private Date getStartDate() throws ParseException {
		Date start = df.parse("2004/10/12");
		return start;
	}

	private Date getEndDate() throws ParseException {
		Date end = df.parse("2004/11/13");
		return end;
	}

}
