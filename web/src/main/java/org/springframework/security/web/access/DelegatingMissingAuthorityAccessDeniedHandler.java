/*
 * Copyright 2004-present the original author or authors.
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

package org.springframework.security.web.access;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authorization.AuthorityAuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.util.Assert;

/**
 * Maps missing granted authorities to {@link AuthenticationEntryPoint}. If no mapping is
 * found a default {@link AccessDeniedHandler} is used.
 *
 * @author Josh Cummings
 * @author Rob Winch
 * @since 7.0
 */
public final class DelegatingMissingAuthorityAccessDeniedHandler implements AccessDeniedHandler {

	private final Map<String, AuthenticationEntryPoint> missingAuthorityToEntryPoint;

	private final AccessDeniedHandler deniedHandler;

	private RequestCache requestCache = new NullRequestCache();

	public DelegatingMissingAuthorityAccessDeniedHandler(
			Map<String, AuthenticationEntryPoint> missingAuthorityToEntryPoint, AccessDeniedHandler deniedHandler) {
		Assert.notEmpty(missingAuthorityToEntryPoint, "missingAuthorityToEntryPoint cannot be empty");
		Assert.notNull(deniedHandler, "deniedHandler cannot be null");
		this.missingAuthorityToEntryPoint = missingAuthorityToEntryPoint;
		this.deniedHandler = deniedHandler;
	}

	public void setRequestCache(RequestCache requestCache) {
		Assert.notNull(requestCache, "requestCache cannot be null");
		this.requestCache = requestCache;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
			throws IOException, ServletException {
		Collection<GrantedAuthority> authorization = missingAuthorities(ex);
		AuthenticationEntryPoint entryPoint = entryPointFor(authorization);
		if (entryPoint != null) {
			this.requestCache.saveRequest(request, response);
			entryPoint.commence(request, response,
					new InsufficientAuthenticationException("Missing Authentication", ex));
		}
		else {
			this.deniedHandler.handle(request, response, ex);
		}
	}

	private @Nullable AuthenticationEntryPoint entryPointFor(Collection<GrantedAuthority> authorities) {
		if (authorities == null) {
			return null;
		}
		for (GrantedAuthority needed : authorities) {
			AuthenticationEntryPoint deniedHandler = this.missingAuthorityToEntryPoint.get(needed.getAuthority());
			if (deniedHandler != null) {
				return deniedHandler;
			}
		}
		return null;
	}

	private Collection<GrantedAuthority> missingAuthorities(AccessDeniedException accessEx) {
		if (accessEx == null) {
			return List.of();
		}
		if (!(accessEx instanceof AuthorizationDeniedException denied)) {
			return List.of();
		}
		if (!(denied.getAuthorizationResult() instanceof AuthorityAuthorizationDecision authorization)) {
			return List.of();
		}
		return authorization.getAuthorities();
	}

}
