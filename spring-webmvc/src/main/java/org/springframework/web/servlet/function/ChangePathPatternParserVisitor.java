/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.function;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Implementation of {@link RouterFunctions.Visitor} that changes the
 * {@link PathPatternParser} on path-related request predicates
 * (i.e. {@code RequestPredicates.PathPatternPredicate}.
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
class ChangePathPatternParserVisitor implements RouterFunctions.Visitor {

	private final PathPatternParser parser;


	public ChangePathPatternParserVisitor(PathPatternParser parser) {
		Assert.notNull(parser, "Parser must not be null");
		this.parser = parser;
	}

	@Override
	public void startNested(RequestPredicate predicate) {
		changeParser(predicate);
	}

	@Override
	public void endNested(RequestPredicate predicate) {
	}

	@Override
	public void route(RequestPredicate predicate, HandlerFunction<?> handlerFunction) {
		changeParser(predicate);
	}

	@Override
	public void resources(Function<ServerRequest, Optional<Resource>> lookupFunction) {
	}

	@Override
	public void attributes(Map<String, Object> attributes) {
	}

	@Override
	public void unknown(RouterFunction<?> routerFunction) {
	}

	private void changeParser(RequestPredicate predicate) {
		if (predicate instanceof Target target) {
			target.changeParser(this.parser);
		}
	}


	/**
	 * Interface implemented by predicates that can change the parser.
	 */
	public interface Target {

		void changeParser(PathPatternParser parser);
	}
}
