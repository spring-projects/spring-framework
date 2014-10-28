/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.orm.hibernate4.support;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.springframework.lang.UsesJava8;
import org.springframework.util.FixedBatchSpliteratorBase;

import static org.hibernate.ScrollMode.*;

/**
 * Adapts Hibernate's {@link ScrollableResults} into a {@code Spliterator} which
 * implements {@link Spliterator#trySplit trySplit()} such that batches of configurable
 * size are produced. Each batch will be a strict prefix of this spliterator's remaining
 * elements. The spliterator is {@link #ORDERED ORDERED} and {@link #NONNULL NONNULL}.
 * <p>
 * The type of elements produced by this spliterator is either {@code Object[]} for
 * results whose {@link ScrollableResults#get get()} method returns an array of length
 * above 1, or the type of the sole member when the array is of length 1 (the element will
 * be unwrapped). The type conforming with the above logic must be passed into the
 * constructor.
 * <p>
 * This spliterator can be used to achieve good parallel speedup, provided that:
 * <ol>
 * <li>typical time taken to fetch one element from the underlying
 * {@code ScrollableResults} is negligible compared to the time needed to process it in
 * the stream pipeline;
 * <li>the batch size is appropriately chosen such that the processing of one batch takes
 * about 1 to 10 milliseconds (fetch time is not included in this).</li>
 * </ol>
 *
 * @param <T> The type of elements produced by this Spliterator
 * @author Marko Topolnik
 * @since 4.2
 */
@UsesJava8
public class ScrollableResultsSpliterator<T> extends FixedBatchSpliteratorBase<T> {

	private final ScrollableResults results;

	private boolean closed;

	private Boolean canUnwrap;

	/**
	 * Creates a spliterator from {@code ScrollableResults} obtained by invoking
	 * {@link Query#scroll(ScrollMode) scroll(ScrollMode.FORWARD_ONLY)} on the supplied
	 * {@code Query}. Spliterator's {@code trySplit()} method will split off its strict
	 * prefix with the default batch size (defined by
	 * {@link #DEFAULT_BATCH_SIZE DEFAULT_BATCH_SIZE}).
	 *
	 * @param clazz the type of the elements which will be produced by the spliterator
	 * @param query the Hibernate query
	 */
	public ScrollableResultsSpliterator(Class<T> clazz, Query query) {
		this(clazz, query.scroll(FORWARD_ONLY));
	}

	/**
	 * Creates a spliterator from {@code ScrollableResults} obtained by invoking
	 * {@link Criteria#scroll(ScrollMode) scroll(ScrollMode.FORWARD_ONLY)} on the supplied
	 * {@code Criteria}. Spliterator's {@code trySplit()} method will split off its strict
	 * prefix with the default batch size (defined by
	 * {@link #DEFAULT_BATCH_SIZE DEFAULT_BATCH_SIZE}).
	 *
	 * @param clazz the type of the elements which will be produced by the spliterator
	 * @param criteria the Hibernate criteria
	 */
	public ScrollableResultsSpliterator(Class<T> clazz, Criteria criteria) {
		this(clazz, criteria.scroll(FORWARD_ONLY));
	}

	/**
	 * Creates a spliterator from Hibernate's {@code ScrollableResults}. Spliterator's
	 * {@code trySplit()} method will split off its strict prefix with the default batch
	 * size (defined by {@link #DEFAULT_BATCH_SIZE DEFAULT_BATCH_SIZE}). Callers should be
	 * careful in choosing the scroll mode for the {@code ScrollableResults} because the
	 * JDBC driver implementation may need to retrieve the entire result set for modes
	 * other than {@link ScrollMode#FORWARD_ONLY} .
	 *
	 * @param clazz the type of the elements which will be produced by the spliterator
	 * @param results the Hibernate scrollable results
	 */
	public ScrollableResultsSpliterator(Class<T> clazz, ScrollableResults results) {
		this(clazz, DEFAULT_BATCH_SIZE, results);
	}

	/**
	 * Creates a spliterator from {@code ScrollableResults} obtained by invoking
	 * {@link Query#scroll(ScrollMode) scroll(ScrollMode.FORWARD_ONLY)} on the supplied
	 * {@code Query}. Spliterator's {@code trySplit()} method will split off its strict
	 * prefix with the given batch size.
	 *
	 * @param clazz the type of the elements which will be produced by the spliterator
	 * @param batchSize this spliterator's batch size
	 * @param query the Hibernate query
	 */
	public ScrollableResultsSpliterator(Class<T> clazz, int batchSize, Query query) {
		this(clazz, batchSize, query.scroll(FORWARD_ONLY));
	}

	/**
	 * Creates a spliterator from {@code ScrollableResults} obtained by invoking
	 * {@link Criteria#scroll(ScrollMode) scroll(ScrollMode.FORWARD_ONLY)} on the supplied
	 * {@code Criteria}. Spliterator's {@code trySplit()} method will split off its strict
	 * prefix with the given batch size.
	 *
	 * @param clazz the type of the elements which will be produced by the spliterator
	 * @param batchSize this spliterator's batch size
	 * @param criteria the Hibernate criteria
	 */
	public ScrollableResultsSpliterator(Class<T> clazz, int batchSize, Criteria criteria) {
		this(clazz, batchSize, criteria.scroll(FORWARD_ONLY));
	}

	/**
	 * Creates a spliterator from Hibernate's {@code ScrollableResults}. Spliterator's
	 * {@code trySplit()} method will split off its strict prefix with the given batch
	 * size. Callers should be careful in choosing the scroll mode for
	 * {@code ScrollableResults} because the JDBC driver implementation may need to
	 * retrieve the entire result set for modes other than {@link ScrollMode#FORWARD_ONLY}
	 * .
	 *
	 * @param clazz the type of the elements which will be produced by the spliterator
	 * @param batchSize this spliterator's batch size
	 * @param results the Hibernate scrollable results
	 */
	public ScrollableResultsSpliterator(Class<T> clazz, int batchSize, ScrollableResults results) {
		super(ORDERED | NONNULL, batchSize);
		if (results == null) {
			throw new NullPointerException("ScrollableResults must not be null");
		}
		this.results = results;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws ClassCastException if the type of element derived from the invocation of
	 *         {@link ScrollableResults#get} (as explained in the class-level
	 *         documentation above) cannot be cast into the type passed to the
	 *         constructor.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean tryAdvance(Consumer<? super T> action) {
		if (closed) {
			return false;
		}
		if (!results.next()) {
			close();
			return false;
		}
		if (canUnwrap == null) {
			final Object[] r = results.get();
			canUnwrap = r.length == 1;
			action.accept((T) (canUnwrap ? r[0] : r));
		}
		else {
			action.accept((T) (canUnwrap ? results.get(0) : results.get()));
		}
		return true;
	}

	/**
	 * Closes the underlying {@code ScrollableResults}, releasing all JDBC resources it
	 * has acquired. After this method is called, {@link #tryAdvance tryAdvace()} will
	 * always return {@code false}.
	 */
	public void close() {
		if (!closed) {
			results.close();
			closed = true;
		}
	}

	/**
	 * Builds a {@code Stream} backed by {@code ScrollableResults} obtained from the given
	 * {@code Query}. Convenience around
	 * {@link #ScrollableResultsSpliterator(Class, Query)}. Closing the stream will
	 * propagate to the underlying {@code ScrollableResults}.
	 *
	 * @param clazz the type of the stream elements in reified form
	 * @param query the Hibernate query
	 * @param <T> the type of stream elements inferred from {@code clazz}
	 * @return a {@code Stream} of elements returned by the query
	 */
	public static <T> Stream<T> resultStream(Class<T> clazz, Query query) {
		return resultStream(new ScrollableResultsSpliterator<T>(clazz, query));
	}

	/**
	 * Builds a {@code Stream} backed by {@code ScrollableResults} obtained from the given
	 * {@code Criteria}. Convenience around
	 * {@link #ScrollableResultsSpliterator(Class, Criteria)}. Closing the stream will
	 * propagate to the underlying {@code ScrollableResults}.
	 *
	 * @param clazz the type of the stream elements in reified form
	 * @param criteria the Hibernate criteria
	 * @param <T> the type of stream elements inferred from {@code clazz}
	 * @return a {@code Stream} of elements returned by the criteria.
	 */
	public static <T> Stream<T> resultStream(Class<T> clazz, Criteria criteria) {
		return resultStream(new ScrollableResultsSpliterator<T>(clazz, criteria));
	}

	/**
	 * Builds a {@code Stream} backed by the supplied {@code ScrollableResults}.
	 * Convenience around {@link #ScrollableResultsSpliterator(Class, ScrollableResults)}.
	 * Closing the stream will propagate to the underlying {@code ScrollableResults}.
	 *
	 * @param clazz the type of the stream elements in reified form
	 * @param results the Hibernate scrollable results.
	 * @param <T> the type of stream elements inferred from {@code clazz}
	 * @return a {@code Stream} of elements returned by the scrollable results
	 */
	public static <T> Stream<T> resultStream(Class<T> clazz, ScrollableResults results) {
		return resultStream(new ScrollableResultsSpliterator<T>(clazz, results));
	}

	/**
	 * Builds a {@code Stream} backed by {@code ScrollableResults} obtained from the given
	 * {@code Query}. Convenience around
	 * {@link #ScrollableResultsSpliterator(Class, int, Query)}. Closing the stream will
	 * propagate to the underlying {@code ScrollableResults}.
	 *
	 * @param clazz the type of the stream elements in reified form
	 * @param batchSize the underlying spliterator's batch size.
	 * @param query the Hibernate query.
	 * @param <T> the type of stream elements inferred from {@code clazz}
	 * @return a {@code Stream} of elements returned by the query
	 */
	public static <T> Stream<T> resultStream(Class<T> clazz, int batchSize, Query query) {
		return resultStream(new ScrollableResultsSpliterator<T>(clazz, batchSize, query));
	}

	/**
	 * Builds a {@code Stream} backed by {@code ScrollableResults} obtained from the given
	 * {@code Criteria}. Convenience around
	 * {@link #ScrollableResultsSpliterator(Class, int, Criteria)}. Closing the stream
	 * will propagate to the underlying {@code ScrollableResults}.
	 *
	 * @param clazz the type of the stream elements in reified form
	 * @param batchSize the underlying spliterator's batch size
	 * @param criteria the Hibernate criteria
	 * @param <T> the type of stream elements inferred from {@code clazz}
	 * @return a {@code Stream} of elements returned by the criteria
	 */
	public static <T> Stream<T> resultStream(Class<T> clazz, int batchSize, Criteria criteria) {
		return resultStream(new ScrollableResultsSpliterator<T>(clazz, batchSize, criteria));
	}

	/**
	 * Builds a {@code Stream} backed by the supplied {@code ScrollableResults}.
	 * Convenience around
	 * {@link #ScrollableResultsSpliterator(Class, int, ScrollableResults)}. Closing the
	 * stream will propagate to the underlying {@code ScrollableResults}.
	 *
	 * @param clazz the type of the stream elements in reified form
	 * @param batchSize the underlying spliterator's batch size
	 * @param results the Hibernate scrollable results
	 * @param <T> the type of stream elements inferred from {@code clazz}
	 * @return a {@code Stream} of elements returned by the scrollable results
	 */
	public static <T> Stream<T> resultStream(Class<T> clazz, int batchSize, ScrollableResults results) {
		return resultStream(new ScrollableResultsSpliterator<T>(clazz, batchSize, results));
	}

	/**
	 * Returns an initially sequential {@code Stream} based on the supplied spliterator.
	 * Closing the stream will invoke {@link #close} on the spliterator.
	 *
	 * @param spliterator the spliterator
	 * @param <T> the type of stream elements inferred from {@code spliterator}
	 * @return a {@code Stream} of elements produced by the spliterator
	 */
	public static <T> Stream<T> resultStream(final ScrollableResultsSpliterator<T> spliterator) {
		return StreamSupport.stream(spliterator, false).onClose(new Runnable() {
			public void run() {
				spliterator.close();
			}
		});
	}
}