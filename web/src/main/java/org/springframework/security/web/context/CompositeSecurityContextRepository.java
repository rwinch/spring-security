/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.security.web.context;

import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.util.Assert;

/**
 * A composite {@link SecurityContextRepository} that delegates all work to other
 * implementations.
 *
 * @author Rob Winch
 * @since 5.8
 */
public class CompositeSecurityContextRepository implements SecurityContextRepository {

	private static final SecurityContext EMPTY_CONTEXT = new SecurityContextImpl();

	private final List<SecurityContextRepository> delegates;

	/**
	 * Creates a new instance.
	 * @param delegates the {@link SecurityContextRepository} instances to delegate to.
	 * Cannot be null or empty.
	 */
	public CompositeSecurityContextRepository(List<SecurityContextRepository> delegates) {
		Assert.notEmpty(delegates, "delegates cannot be null or empty");
		this.delegates = delegates;
	}

	@Override
	public Supplier<SecurityContext> loadContext(HttpServletRequest request) {
		Iterator<SecurityContextRepository> iDelegates = this.delegates.iterator();
		while (iDelegates.hasNext()) {
			SecurityContextRepository repository = iDelegates.next();
			Supplier<SecurityContext> deferredSecurityContext = repository.loadContext(request);
			// FIXME: Should use new method on DeferredSecurityContext
			if (deferredSecurityContext != null || !iDelegates.hasNext()) {
				return deferredSecurityContext;
			}
		}
		throw new IllegalStateException("delegates cannot be empty");
	}

	@Override
	public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
		Iterator<SecurityContextRepository> iDelegates = this.delegates.iterator();
		while (iDelegates.hasNext()) {
			SecurityContextRepository repository = iDelegates.next();
			SecurityContext context = repository.loadContext(requestResponseHolder);

			if (!EMPTY_CONTEXT.equals(context) || !iDelegates.hasNext()) {
				return context;
			}
		}
		throw new IllegalStateException("delegates cannot be empty");
	}

	@Override
	public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
		for (SecurityContextRepository repository : this.delegates) {
			repository.saveContext(context, request, response);
		}
	}

	@Override
	public boolean containsContext(HttpServletRequest request) {
		for (SecurityContextRepository repository : this.delegates) {
			if (repository.containsContext(request)) {
				return true;
			}
		}
		return false;
	}

}
