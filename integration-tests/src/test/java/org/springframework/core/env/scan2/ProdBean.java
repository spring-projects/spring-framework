/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.env.scan2;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile(org.springframework.core.env.EnvironmentSystemIntegrationTests.Constants.PROD_ENV_NAME)
@Component(org.springframework.core.env.EnvironmentSystemIntegrationTests.Constants.PROD_BEAN_NAME)
class ProdBean {

}
