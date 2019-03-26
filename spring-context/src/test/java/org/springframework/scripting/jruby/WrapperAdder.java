/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.scripting.jruby;

import java.util.Map;

/**
 * https://opensource.atlassian.com/projects/spring/browse/SPR-3038
 *
 * @author Rick Evans
 */
public interface WrapperAdder {

	Integer addInts(Integer x, Integer y);

	Short addShorts(Short x, Short y);

	Long addLongs(Long x, Long y);

	Float addFloats(Float x, Float y);

	Double addDoubles(Double x, Double y);

	Boolean resultIsPositive(Integer x, Integer y);

	String concatenate(Character c, Character d);

	Character echo(Character c);

	String concatArrayOfIntegerWrappers(Integer[] numbers);

	Short[] populate(Short one, Short two);

	String[][] createListOfLists(String one, String second, String third);

	Map<?, ?> toMap(String key, Object value);

}
