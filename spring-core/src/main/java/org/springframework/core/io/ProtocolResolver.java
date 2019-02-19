/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.io;

import org.springframework.lang.Nullable;

/**
 * 用户自定义协议资源解决策略，作为 DefaultResourceLoader 的 SPI：它允许用户自定义资源加载协议，而不需要继承 ResourceLoader 的子类。
 * 如果要实现自定义 Resource，我们只需要继承 DefaultResource 即可，
 * 但是有了 ProtocolResolver 后，我们不需要直接继承 DefaultResourceLoader，改为实现 ProtocolResolver 接口也可以实现自定义的 ResourceLoader。
 */
@FunctionalInterface
public interface ProtocolResolver {

	/**
	 * 使用指定的 ResourceLoader ，解析指定的 location 。
	 * 若成功，则返回对应的 Resource 。
	 *
	 * 该接口并没有实现类，它需要用户自定义，自定义的 Resolver 如何加入 Spring 体系呢？
	 * 调用 DefaultResourceLoader#addProtocolResolver(ProtocolResolver) 方法即可
	 */
	@Nullable
	Resource resolve(String location, ResourceLoader resourceLoader);

}
