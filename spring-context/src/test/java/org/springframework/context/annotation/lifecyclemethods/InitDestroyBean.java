/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.lifecyclemethods;

import java.util.ArrayList;
import java.util.List;

public class InitDestroyBean {

	public final List<String> initMethods = new ArrayList<>();
	public final List<String> destroyMethods = new ArrayList<>();


	public void initMethod() {
		this.initMethods.add("initMethod");
	}

	public void destroyMethod() {
		this.destroyMethods.add("destroyMethod");
	}

}
