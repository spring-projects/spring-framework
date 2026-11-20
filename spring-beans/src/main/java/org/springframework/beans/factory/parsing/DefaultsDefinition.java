/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.parsing;

import org.springframework.beans.BeanMetadataElement;

/**
 * Marker interface for a defaults definition,
 * extending BeanMetadataElement to inherit source exposure.
 *
 * <p>Concrete implementations are typically based on 'document defaults',
 * for example specified at the root tag level within an XML document.
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see org.springframework.beans.factory.xml.DocumentDefaultsDefinition
 * @see ReaderEventListener#defaultsRegistered(DefaultsDefinition)
 */
public interface DefaultsDefinition extends BeanMetadataElement {

}
