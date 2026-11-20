/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jmx.jmxexporting;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jmx.export.MBeanExporter;

// tag::snippet[]
@Configuration
public class JmxConfiguration {

	@Bean
	MBeanExporter exporter(JmxTestBean testBean) {
		MBeanExporter exporter = new MBeanExporter();
		exporter.setBeans(Map.of("bean:name=testBean1", testBean));
		return exporter;
	}

	@Bean
	JmxTestBean testBean() {
		JmxTestBean testBean = new JmxTestBean();
		testBean.setName("TEST");
		testBean.setAge(100);
		return testBean;
	}
}
// end::snippet[]
