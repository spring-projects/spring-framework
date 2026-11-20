/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.event;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationEvent;

/**
 * Root object used during event listener SpEL expression evaluation.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.2
 * @param event the application event
 * @param args the arguments supplied to the listener method
 * @see EventListener#condition()
 */
record EventExpressionRootObject(ApplicationEvent event, @Nullable Object[] args) {
}
