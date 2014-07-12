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
package org.springframework.web.servlet.config.annotation;

import org.springframework.web.servlet.view.tiles3.TilesConfigurer;

/**
 * Defines a callback method to customize the
 * {@link org.springframework.web.servlet.view.tiles3.TilesConfigurer
 * TilesConfigurer} bean provided when using {@code @EnableWebMvc}.
 *
 * <p>An {@code @EnableWebMvc}-annotated configuration classes can implement
 * this interface to customize the {@code TilesConfigurer}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface TilesWebMvcConfigurer {

	void configureTiles(TilesConfigurer configurer);

}
