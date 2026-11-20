/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package example.scannable_implicitbasepackage;

import org.springframework.stereotype.Component;

/**
 * @author Juergen Hoeller
 */
@Component
public class ConfigurableComponent {

	private final boolean flag;

	public ConfigurableComponent() {
		this(false);
	}

	public ConfigurableComponent(boolean flag) {
		this.flag = flag;
	}

	public boolean isFlag() {
		return this.flag;
	}

}
