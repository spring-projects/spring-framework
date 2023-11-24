/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;
import org.springframework.jmx.support.RegistrationPolicy;

/**
 * Enables default exporting of all standard {@code MBean}s from the Spring context, as
 * well as all {@code @ManagedResource} annotated beans.
 *
 * <p>The resulting {@link org.springframework.jmx.export.MBeanExporter MBeanExporter}
 * bean is defined under the name "mbeanExporter". Alternatively, consider defining a
 * custom {@link AnnotationMBeanExporter} bean explicitly.
 *
 * <p>This annotation is modeled after and functionally equivalent to Spring XML's
 * {@code <context:mbean-export/>} element.
 *
 * @author Phillip Webb
 * @since 3.2
 * @see MBeanExportConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(MBeanExportConfiguration.class)
public @interface EnableMBeanExport {

	/**
	 * The default domain to use when generating JMX ObjectNames.
	 */
	String defaultDomain() default "";

	/**
	 * The bean name of the MBeanServer to which MBeans should be exported. Default is to
	 * use the platform's default MBeanServer.
	 */
	String server() default "";

	/**
	 * The policy to use when attempting to register an MBean under an
	 * {@link javax.management.ObjectName} that already exists. Defaults to
	 * {@link RegistrationPolicy#FAIL_ON_EXISTING}.
	 */
	RegistrationPolicy registration() default RegistrationPolicy.FAIL_ON_EXISTING;
}
