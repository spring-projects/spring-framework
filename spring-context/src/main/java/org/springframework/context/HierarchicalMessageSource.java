/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context;

import org.jspecify.annotations.Nullable;

/**
 * Sub-interface of MessageSource to be implemented by objects that
 * can resolve messages hierarchically.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public interface HierarchicalMessageSource extends MessageSource {

	/**
	 * Set the parent that will be used to try to resolve messages
	 * that this object can't resolve.
	 * @param parent the parent MessageSource that will be used to
	 * resolve messages that this object can't resolve.
	 * May be {@code null}, in which case no further resolution is possible.
	 */
	void setParentMessageSource(@Nullable MessageSource parent);

	/**
	 * Return the parent of this MessageSource, or {@code null} if none.
	 */
	@Nullable MessageSource getParentMessageSource();

}
