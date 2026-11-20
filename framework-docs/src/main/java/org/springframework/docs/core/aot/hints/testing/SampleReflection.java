/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.aot.hints.testing;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.ClassUtils;

public class SampleReflection {

	private final Log logger = LogFactory.getLog(SampleReflection.class);

	public void performReflection() {
		try {
			Class<?> springVersion = ClassUtils.forName("org.springframework.core.SpringVersion", null);
			Method getVersion = ClassUtils.getMethod(springVersion, "getVersion");
			String version = (String) getVersion.invoke(null);
			logger.info("Spring version: " + version);
		}
		catch (Exception exc) {
			logger.error("reflection failed", exc);
		}
	}

}
