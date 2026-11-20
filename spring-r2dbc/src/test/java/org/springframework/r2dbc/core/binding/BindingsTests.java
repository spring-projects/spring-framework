/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.core.binding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link Bindings}.
 *
 * @author Mark Paluch
 */
class BindingsTests {

	BindMarkersFactory markersFactory = BindMarkersFactory.indexed("$", 1);

	BindTarget bindTarget = mock();


	@Test
	void shouldCreateBindings() {
		MutableBindings bindings = new MutableBindings(markersFactory.create());

		bindings.bind(bindings.nextMarker(), "foo");
		bindings.bindNull(bindings.nextMarker(), String.class);

		assertThat(bindings).hasSize(2);
	}

	@Test
	void shouldApplyValueBinding() {
		MutableBindings bindings = new MutableBindings(markersFactory.create());

		bindings.bind(bindings.nextMarker(), "foo");
		bindings.apply(bindTarget);

		verify(bindTarget).bind(0, "foo");
	}

	@Test
	void shouldApplySimpleValueBinding() {
		MutableBindings bindings = new MutableBindings(markersFactory.create());

		BindMarker marker = bindings.bind("foo");
		bindings.apply(bindTarget);

		assertThat(marker.getPlaceholder()).isEqualTo("$1");
		verify(bindTarget).bind(0, "foo");
	}

	@Test
	void shouldApplyNullBinding() {
		MutableBindings bindings = new MutableBindings(markersFactory.create());

		bindings.bindNull(bindings.nextMarker(), String.class);

		bindings.apply(bindTarget);

		verify(bindTarget).bindNull(0, String.class);
	}

	@Test
	void shouldApplySimpleNullBinding() {
		MutableBindings bindings = new MutableBindings(markersFactory.create());

		BindMarker marker = bindings.bindNull(String.class);
		bindings.apply(bindTarget);

		assertThat(marker.getPlaceholder()).isEqualTo("$1");
		verify(bindTarget).bindNull(0, String.class);
	}

	@Test
	void shouldConsumeBindings() {
		MutableBindings bindings = new MutableBindings(markersFactory.create());

		bindings.bind(bindings.nextMarker(), "foo");
		bindings.bindNull(bindings.nextMarker(), String.class);

		AtomicInteger counter = new AtomicInteger();

		bindings.forEach(binding -> {

			if (binding.hasValue()) {
				counter.incrementAndGet();
				assertThat(binding.getValue()).isEqualTo("foo");
				assertThat(binding.getBindMarker().getPlaceholder()).isEqualTo("$1");
			}

			if (binding.isNull()) {
				counter.incrementAndGet();

				assertThat(((Bindings.NullBinding) binding).getValueType()).isEqualTo(String.class);
				assertThat(binding.getBindMarker().getPlaceholder()).isEqualTo("$2");
			}
		});

		assertThat(counter).hasValue(2);
	}

	@Test
	void shouldMergeBindings() {
		BindMarkers markers = markersFactory.create();

		BindMarker shared = markers.next();
		BindMarker leftMarker = markers.next();
		List<Bindings.Binding> left = new ArrayList<>();
		left.add(new Bindings.NullBinding(shared, String.class));
		left.add(new Bindings.ValueBinding(leftMarker, "left"));

		BindMarker rightMarker = markers.next();
		List<Bindings.Binding> right = new ArrayList<>();
		left.add(new Bindings.ValueBinding(shared, "override"));
		left.add(new Bindings.ValueBinding(rightMarker, "right"));

		Bindings merged = Bindings.merge(new Bindings(left), new Bindings(right));

		assertThat(merged).hasSize(3);

		merged.apply(bindTarget);
		verify(bindTarget).bind(0, "override");
		verify(bindTarget).bind(1, "left");
		verify(bindTarget).bind(2, "right");
	}

}
