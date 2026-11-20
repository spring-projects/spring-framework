/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.metadata;

import org.jspecify.annotations.Nullable;

/**
 * Metadata that indicates to expose a given bean property as JMX attribute.
 * Only valid when used on a JavaBean getter or setter.
 *
 * @author Rob Harrop
 * @since 1.2
 * @see org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler
 * @see org.springframework.jmx.export.MBeanExporter
 */
public class ManagedAttribute extends AbstractJmxAttribute {

	/**
	 * Empty attributes.
	 */
	public static final ManagedAttribute EMPTY = new ManagedAttribute();


	private @Nullable Object defaultValue;

	private @Nullable String persistPolicy;

	private int persistPeriod = -1;


	/**
	 * Set the default value of this attribute.
	 */
	public void setDefaultValue(@Nullable Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Return the default value of this attribute.
	 */
	public @Nullable Object getDefaultValue() {
		return this.defaultValue;
	}

	public void setPersistPolicy(@Nullable String persistPolicy) {
		this.persistPolicy = persistPolicy;
	}

	public @Nullable String getPersistPolicy() {
		return this.persistPolicy;
	}

	public void setPersistPeriod(int persistPeriod) {
		this.persistPeriod = persistPeriod;
	}

	public int getPersistPeriod() {
		return this.persistPeriod;
	}

}
