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

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import reactor.core.publisher.Mono;

/**
 *
 * @author Rob Winch
 * @since 5.0
 */
public interface ReactiveAuthorizationManager<T> {
	// FIXME return Mono<Decision>
	Mono<Boolean> check(Mono<Authentication> authentication, T object);

	default Mono<Void> verify(Mono<Authentication> authentication, T object) {
		return check(authentication, object)
			.filter( d -> d)
			.switchIfEmpty( Mono.defer(() -> Mono.error(new AccessDeniedException("Access Denied"))) )
			.flatMap( d -> Mono.empty() );
	}
}
