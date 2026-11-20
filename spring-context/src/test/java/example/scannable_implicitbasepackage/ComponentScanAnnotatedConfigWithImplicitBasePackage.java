/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package example.scannable_implicitbasepackage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author Phillip Webb
 */
@Configuration
@ComponentScan
public class ComponentScanAnnotatedConfigWithImplicitBasePackage {

	@Bean  // override of scanned class
	public ConfigurableComponent configurableComponent() {
		return new ConfigurableComponent(true);
	}

}
