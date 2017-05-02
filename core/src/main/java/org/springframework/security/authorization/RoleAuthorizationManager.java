/*
 *
 *  * Copyright 2002-2017 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.security.authorization;

import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

/**
 * @author Rob Winch
 * @since 5.0
 */
public class RoleAuthorizationManager<T> implements ReactiveAuthorizationManager<T> {
	private final String role;

	public RoleAuthorizationManager(String role) {
		this.role = role;
	}

	@Override
	public Mono<Boolean> check(Mono<Authentication> authentication, T object) {
		return authentication
			.filter(a -> a.isAuthenticated())
			.flatMapIterable( a -> a.getAuthorities())
			.map( g-> g.getAuthority())
			.hasElement(this.role)
			.defaultIfEmpty(false);
	}
}
