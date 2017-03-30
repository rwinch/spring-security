/*
 * Copyright 2012-2017 the original author or authors.
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
package org.springframework.security.oauth2.core.protocol;

import org.springframework.security.oauth2.core.OAuth2Attributes;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Joe Grandja
 */
public final class ResponseAttributesExtractor {

	private ResponseAttributesExtractor() {
	}

	public static AuthorizationCodeAuthorizationResponseAttributes extractAuthorizationCodeResponse(HttpServletRequest request) {
		AuthorizationCodeAuthorizationResponseAttributes response;

		String code = request.getParameter(OAuth2Attributes.CODE);
		Assert.hasText(code, OAuth2Attributes.CODE + " attribute is required");

		String state = request.getParameter(OAuth2Attributes.STATE);

		response = new AuthorizationCodeAuthorizationResponseAttributes(code, state);

		return response;
	}

	public static ErrorResponseAttributes extractErrorResponse(HttpServletRequest request) {
		ErrorResponseAttributes response;

		String error = request.getParameter(OAuth2Attributes.ERROR);
		Assert.hasText(error, OAuth2Attributes.ERROR + " attribute is required");

		String errorDescription = request.getParameter(OAuth2Attributes.ERROR_DESCRIPTION);
		String errorUri = request.getParameter(OAuth2Attributes.ERROR_URI);
		String state = request.getParameter(OAuth2Attributes.STATE);

		response = new ErrorResponseAttributes(error, errorDescription, errorUri, state);

		return response;
	}
}
