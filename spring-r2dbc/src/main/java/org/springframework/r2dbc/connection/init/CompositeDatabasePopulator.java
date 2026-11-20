/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.connection.init;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import io.r2dbc.spi.Connection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.util.Assert;

/**
 * Composite {@link DatabasePopulator} that delegates to a list of given
 * {@code DatabasePopulator} implementations, executing all scripts.
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Kazuki Shimizu
 * @author Mark Paluch
 * @since 5.3
 */
public class CompositeDatabasePopulator implements DatabasePopulator {

	private final List<DatabasePopulator> populators = new ArrayList<>(4);


	/**
	 * Create an empty {@code CompositeDatabasePopulator}.
	 * @see #setPopulators
	 * @see #addPopulators
	 */
	public CompositeDatabasePopulator() {
	}

	/**
	 * Create a {@code CompositeDatabasePopulator}. with the given populators.
	 * @param populators one or more populators to delegate to.
	 */
	public CompositeDatabasePopulator(Collection<DatabasePopulator> populators) {
		Assert.notNull(populators, "DatabasePopulators must not be null");
		this.populators.addAll(populators);
	}

	/**
	 * Create a {@code CompositeDatabasePopulator} with the given populators.
	 * @param populators one or more populators to delegate to.
	 */
	public CompositeDatabasePopulator(DatabasePopulator... populators) {
		Assert.notNull(populators, "DatabasePopulators must not be null");
		this.populators.addAll(Arrays.asList(populators));
	}


	/**
	 * Specify one or more populators to delegate to.
	 */
	public void setPopulators(DatabasePopulator... populators) {
		Assert.notNull(populators, "DatabasePopulators must not be null");
		this.populators.clear();
		this.populators.addAll(Arrays.asList(populators));
	}

	/**
	 * Add one or more populators to the list of delegates.
	 */
	public void addPopulators(DatabasePopulator... populators) {
		Assert.notNull(populators, "DatabasePopulators must not be null");
		this.populators.addAll(Arrays.asList(populators));
	}

	@Override
	public Mono<Void> populate(Connection connection) throws ScriptException {
		Assert.notNull(connection, "Connection must not be null");
		return Flux.fromIterable(this.populators).concatMap(populator -> populator.populate(connection))
				.then();
	}

}
