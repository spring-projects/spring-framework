/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.parsing;

/**
 * Empty implementation of the {@link ReaderEventListener} interface,
 * providing no-op implementations of all callback methods.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public class EmptyReaderEventListener implements ReaderEventListener {

	@Override
	public void defaultsRegistered(DefaultsDefinition defaultsDefinition) {
		// no-op
	}

	@Override
	public void componentRegistered(ComponentDefinition componentDefinition) {
		// no-op
	}

	@Override
	public void aliasRegistered(AliasDefinition aliasDefinition) {
		// no-op
	}

	@Override
	public void importProcessed(ImportDefinition importDefinition) {
		// no-op
	}

}
