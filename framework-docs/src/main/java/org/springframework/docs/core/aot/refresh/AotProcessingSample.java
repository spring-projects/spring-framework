/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.aot.refresh;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

public class AotProcessingSample {

	public void createAotContext() {
		// tag::aotcontext[]
		RuntimeHints hints = new RuntimeHints();
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(MyApplication.class);
		context.refreshForAotProcessing(hints);
		// ...
		context.close();
		// end::aotcontext[]
	}

	// tag::myapplication[]
	@Configuration(proxyBeanMethods=false)
	@ComponentScan
	@Import({DataSourceConfiguration.class, ContainerConfiguration.class})
	public class MyApplication {
	}
	// end::myapplication[]

	class DataSourceConfiguration {
	}

	class ContainerConfiguration {
	}

}
