/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.expression.spel;

import java.util.Map;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.TypedValue;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * This is a local COPY of {@link org.springframework.context.expression.MapAccessor}.
 *
 * @author Juergen Hoeller
 * @author Andy Clement
 * @since 4.1
 */
public class CompilableMapAccessor implements CompilablePropertyAccessor {

	@Override
	public Class<?>[] getSpecificTargetClasses() {
		return new Class<?>[] {Map.class};
	}

	@Override
	public boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
		return (target instanceof Map && ((Map<?, ?>) target).containsKey(name));
	}

	@Override
	public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
		Assert.state(target instanceof Map, "Target must be of type Map");
		Map<?, ?> map = (Map<?, ?>) target;
		Object value = map.get(name);
		if (value == null && !map.containsKey(name)) {
			throw new MapAccessException(name);
		}
		return new TypedValue(value);
	}

	@Override
	public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue)
			throws AccessException {

		Assert.state(target instanceof Map, "Target must be a Map");
		Map<Object, Object> map = (Map<Object, Object>) target;
		map.put(name, newValue);
	}

	@Override
	public boolean isCompilable() {
		return true;
	}

	@Override
	public Class<?> getPropertyType() {
		return Object.class;
	}

	@Override
	public void generateCode(String propertyName, MethodVisitor mv, CodeFlow cf) {
		String descriptor = cf.lastDescriptor();
		if (descriptor == null || !descriptor.equals("Ljava/util/Map")) {
			if (descriptor == null) {
				cf.loadTarget(mv);
			}
			CodeFlow.insertCheckCast(mv, "Ljava/util/Map");
		}
		mv.visitLdcInsn(propertyName);
		mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get","(Ljava/lang/Object;)Ljava/lang/Object;",true);
	}


	/**
	 * Exception thrown from {@code read} in order to reset a cached
	 * PropertyAccessor, allowing other accessors to have a try.
	 */
	@SuppressWarnings("serial")
	private static class MapAccessException extends AccessException {

		private final String key;

		public MapAccessException(String key) {
			super("");
			this.key = key;
		}

		@Override
		public String getMessage() {
			return "Map does not contain a value for key '" + this.key + "'";
		}
	}

}
