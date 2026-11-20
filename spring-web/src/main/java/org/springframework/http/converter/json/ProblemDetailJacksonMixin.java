/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter.json;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

import org.springframework.http.ProblemDetail;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

/**
 * An interface to associate Jackson annotations with
 * {@link org.springframework.http.ProblemDetail} to avoid a hard dependency on
 * the Jackson library.
 *
 * <p>The annotations ensure the {@link ProblemDetail#getProperties() properties}
 * map is unwrapped and rendered as top-level JSON properties, and likewise that
 * the {@code properties} map contains unknown properties from the JSON.
 *
 * <p>{@link Jackson2ObjectMapperBuilder} automatically registers this as a
 * "mix-in" for {@link ProblemDetail}, which means it always applies, unless
 * an {@code ObjectMapper} is instantiated directly and configured for use.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
@JsonInclude(NON_EMPTY)
public interface ProblemDetailJacksonMixin {

	@JsonAnySetter
	void setProperty(String name, @Nullable Object value);

	@JsonAnyGetter
	Map<String, Object> getProperties();

}
