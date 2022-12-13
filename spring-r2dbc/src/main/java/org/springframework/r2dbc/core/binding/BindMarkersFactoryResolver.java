/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.r2dbc.core.binding;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;

import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedCaseInsensitiveMap;

/**
 * Resolves a {@link BindMarkersFactory} from a {@link ConnectionFactory} using
 * {@link BindMarkerFactoryProvider}. Dialect resolution uses Spring's
 * {@link SpringFactoriesLoader spring.factories} to determine available extensions.
 *
 * @author Mark Paluch
 * @since 5.3
 * @see BindMarkersFactory
 * @see SpringFactoriesLoader
 */
public final class BindMarkersFactoryResolver {

	private static final List<BindMarkerFactoryProvider> DETECTORS = SpringFactoriesLoader.loadFactories(
			BindMarkerFactoryProvider.class, BindMarkersFactoryResolver.class.getClassLoader());


	/**
	 * Retrieve a {@link BindMarkersFactory} by inspecting {@link ConnectionFactory}
	 * and its metadata.
	 * @param connectionFactory the connection factory to inspect
	 * @return the resolved {@link BindMarkersFactory}
	 * @throws NoBindMarkersFactoryException if no {@link BindMarkersFactory} can be resolved
	 */
	public static BindMarkersFactory resolve(ConnectionFactory connectionFactory) {
		for (BindMarkerFactoryProvider detector : DETECTORS) {
			BindMarkersFactory bindMarkersFactory = detector.getBindMarkers(connectionFactory);
			if (bindMarkersFactory != null) {
				return bindMarkersFactory;
			}
		}
		throw new NoBindMarkersFactoryException(String.format(
				"Cannot determine a BindMarkersFactory for %s using %s",
				connectionFactory.getMetadata().getName(), connectionFactory));
	}


	private BindMarkersFactoryResolver() {
	}


	/**
	 * SPI to extend Spring's default R2DBC BindMarkersFactory discovery mechanism.
	 * Implementations of this interface are discovered through Spring's
	 * {@link SpringFactoriesLoader} mechanism.
	 * @see SpringFactoriesLoader
	 */
	@FunctionalInterface
	public interface BindMarkerFactoryProvider {

		/**
		 * Return a {@link BindMarkersFactory} for a {@link ConnectionFactory}.
		 * @param connectionFactory the connection factory to be used with the {@link BindMarkersFactory}
		 * @return the {@link BindMarkersFactory} if the {@link BindMarkerFactoryProvider}
		 * can provide a bind marker factory object, otherwise {@code null}
		 */
		@Nullable
		BindMarkersFactory getBindMarkers(ConnectionFactory connectionFactory);
	}


	/**
	 * Exception thrown when {@link BindMarkersFactoryResolver} cannot resolve a
	 * {@link BindMarkersFactory}.
	 */
	@SuppressWarnings("serial")
	public static class NoBindMarkersFactoryException extends NonTransientDataAccessException {

		/**
		 * Constructor for NoBindMarkersFactoryException.
		 * @param msg the detail message
		 */
		public NoBindMarkersFactoryException(String msg) {
			super(msg);
		}
	}


	/**
	 * Built-in bind maker factories. Used typically as last {@link BindMarkerFactoryProvider}
	 * when other providers register with a higher precedence.
	 * @see org.springframework.core.Ordered
	 * @see org.springframework.core.annotation.AnnotationAwareOrderComparator
	 */
	static class BuiltInBindMarkersFactoryProvider implements BindMarkerFactoryProvider {

		private static final Map<String, BindMarkersFactory> BUILTIN =
				new LinkedCaseInsensitiveMap<>(Locale.ENGLISH);

		static {
			BUILTIN.put("H2", BindMarkersFactory.indexed("$", 1));
			BUILTIN.put("MariaDB", BindMarkersFactory.anonymous("?"));
			BUILTIN.put("Microsoft SQL Server", BindMarkersFactory.named("@", "P", 32,
					BuiltInBindMarkersFactoryProvider::filterBindMarker));
			BUILTIN.put("MySQL", BindMarkersFactory.anonymous("?"));
			BUILTIN.put("Oracle", BindMarkersFactory.named(":", "P", 32,
					BuiltInBindMarkersFactoryProvider::filterBindMarker));
			BUILTIN.put("PostgreSQL", BindMarkersFactory.indexed("$", 1));
		}


		@Override
		public BindMarkersFactory getBindMarkers(ConnectionFactory connectionFactory) {
			ConnectionFactoryMetadata metadata = connectionFactory.getMetadata();
			BindMarkersFactory r2dbcDialect = BUILTIN.get(metadata.getName());
			if (r2dbcDialect != null) {
				return r2dbcDialect;
			}
			for (String it : BUILTIN.keySet()) {
				if (metadata.getName().contains(it)) {
					return BUILTIN.get(it);
				}
			}
			return null;
		}

		private static String filterBindMarker(CharSequence input) {
			StringBuilder builder = new StringBuilder(input.length());
			for (int i = 0; i < input.length(); i++) {
				char ch = input.charAt(i);
				// ascii letter or digit
				if (Character.isLetterOrDigit(ch) && ch < 127) {
					builder.append(ch);
				}
			}
			if (builder.length() == 0) {
				return "";
			}
			return "_" + builder.toString();
		}
	}

}
