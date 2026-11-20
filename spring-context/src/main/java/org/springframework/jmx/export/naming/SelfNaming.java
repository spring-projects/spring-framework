/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.naming;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * Interface that allows infrastructure components to provide their own
 * {@code ObjectName}s to the {@code MBeanExporter}.
 *
 * <p><b>Note:</b> This interface is mainly intended for internal usage.
 *
 * @author Rob Harrop
 * @since 1.2.2
 * @see org.springframework.jmx.export.MBeanExporter
 */
public interface SelfNaming {

	/**
	 * Return the {@code ObjectName} for the implementing object.
	 * @throws MalformedObjectNameException if thrown by the ObjectName constructor
	 * @see javax.management.ObjectName#ObjectName(String)
	 * @see javax.management.ObjectName#getInstance(String)
	 * @see org.springframework.jmx.support.ObjectNameManager#getInstance(String)
	 */
	ObjectName getObjectName() throws MalformedObjectNameException;

}
