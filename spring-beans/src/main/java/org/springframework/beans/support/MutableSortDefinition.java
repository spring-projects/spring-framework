/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.support;

import java.io.Serializable;

import org.jspecify.annotations.Nullable;

import org.springframework.util.StringUtils;

/**
 * Mutable implementation of the {@link SortDefinition} interface.
 * Supports toggling the ascending value on setting the same property again.
 *
 * @author Juergen Hoeller
 * @author Jean-Pierre Pawlak
 * @since 26.05.2003
 * @see #setToggleAscendingOnProperty
 * @deprecated as severely outdated and superseded by more modern solutions,
 * for example in Spring Data Commons
 */
@Deprecated(since = "7.0.3", forRemoval = true)
@SuppressWarnings({"removal", "serial"})
public class MutableSortDefinition implements SortDefinition, Serializable {

	private String property = "";

	private boolean ignoreCase = true;

	private boolean ascending = true;

	private boolean toggleAscendingOnProperty = false;


	/**
	 * Create an empty MutableSortDefinition,
	 * to be populated via its bean properties.
	 * @see #setProperty
	 * @see #setIgnoreCase
	 * @see #setAscending
	 */
	public MutableSortDefinition() {
	}

	/**
	 * Copy constructor: create a new MutableSortDefinition
	 * that mirrors the given sort definition.
	 * @param source the original sort definition
	 */
	public MutableSortDefinition(SortDefinition source) {
		this.property = source.getProperty();
		this.ignoreCase = source.isIgnoreCase();
		this.ascending = source.isAscending();
	}

	/**
	 * Create a MutableSortDefinition for the given settings.
	 * @param property the property to compare
	 * @param ignoreCase whether upper and lower case in String values should be ignored
	 * @param ascending whether to sort ascending (true) or descending (false)
	 */
	public MutableSortDefinition(String property, boolean ignoreCase, boolean ascending) {
		this.property = property;
		this.ignoreCase = ignoreCase;
		this.ascending = ascending;
	}

	/**
	 * Create a new MutableSortDefinition.
	 * @param toggleAscendingOnSameProperty whether to toggle the ascending flag
	 * if the same property gets set again (that is, {@code setProperty} gets
	 * called with already set property name again).
	 */
	public MutableSortDefinition(boolean toggleAscendingOnSameProperty) {
		this.toggleAscendingOnProperty = toggleAscendingOnSameProperty;
	}


	/**
	 * Set the property to compare.
	 * <p>If the property was the same as the current, the sort is reversed if
	 * "toggleAscendingOnProperty" is activated, else simply ignored.
	 * @see #setToggleAscendingOnProperty
	 */
	public void setProperty(String property) {
		if (!StringUtils.hasLength(property)) {
			this.property = "";
		}
		else {
			// Implicit toggling of ascending?
			if (isToggleAscendingOnProperty()) {
				this.ascending = (!property.equals(this.property) || !this.ascending);
			}
			this.property = property;
		}
	}

	@Override
	public String getProperty() {
		return this.property;
	}

	/**
	 * Set whether upper and lower case in String values should be ignored.
	 */
	public void setIgnoreCase(boolean ignoreCase) {
		this.ignoreCase = ignoreCase;
	}

	@Override
	public boolean isIgnoreCase() {
		return this.ignoreCase;
	}

	/**
	 * Set whether to sort ascending (true) or descending (false).
	 */
	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}

	@Override
	public boolean isAscending() {
		return this.ascending;
	}

	/**
	 * Set whether to toggle the ascending flag if the same property gets set again
	 * (that is, {@link #setProperty} gets called with already set property name again).
	 * <p>This is particularly useful for parameter binding through a web request,
	 * where clicking on the field header again might be supposed to trigger a
	 * resort for the same field but opposite order.
	 */
	public void setToggleAscendingOnProperty(boolean toggleAscendingOnProperty) {
		this.toggleAscendingOnProperty = toggleAscendingOnProperty;
	}

	/**
	 * Return whether to toggle the ascending flag if the same property gets set again
	 * (that is, {@code setProperty} gets called with already set property name again).
	 */
	public boolean isToggleAscendingOnProperty() {
		return this.toggleAscendingOnProperty;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof SortDefinition that &&
				getProperty().equals(that.getProperty()) &&
				isAscending() == that.isAscending() &&
				isIgnoreCase() == that.isIgnoreCase()));
	}

	@Override
	public int hashCode() {
		int hashCode = getProperty().hashCode();
		hashCode = 29 * hashCode + (isIgnoreCase() ? 1 : 0);
		hashCode = 29 * hashCode + (isAscending() ? 1 : 0);
		return hashCode;
	}

}
