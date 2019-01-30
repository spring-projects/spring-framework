/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.context.annotation;

import org.springframework.core.env.Profiles;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;

/**
 * {@link Condition} that matches based on the value of a {@link Profile @Profile}
 * annotation.
 *
 * @author Chris Beams
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @since 4.0
 */
class ProfileCondition implements Condition {

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
	    // 获得 @Profile 注解的属性
		MultiValueMap<String, Object> attrs = metadata.getAllAnnotationAttributes(Profile.class.getName());
		// 如果非空，进行判断
		if (attrs != null) {
		    // 遍历所有 @Profile 的 value 属性
			for (Object value : attrs.get("value")) {
			    // 判断 environment 有符合的 Profile ，则返回 true ，表示匹配
				if (context.getEnvironment().acceptsProfiles(Profiles.of((String[]) value))) {
					return true;
				}
			}
			// 如果没有，则返回 false
			return false;
		}
		// 如果为空，就表示满足条件
		return true;
	}

}