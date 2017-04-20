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
package org.springframework.security.oauth2.client.web.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.endpoint.OAuth2Parameter;
import org.springframework.security.oauth2.core.endpoint.ErrorResponseAttributes;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Joe Grandja
 */
public final class ErrorResponseAttributesConverter implements Converter<HttpServletRequest, ErrorResponseAttributes> {

	@Override
	public ErrorResponseAttributes convert(HttpServletRequest request) {
		ErrorResponseAttributes response;

		String error = request.getParameter(OAuth2Parameter.ERROR);
		if (!StringUtils.hasText(error)) {
			return null;
		}

		String errorDescription = request.getParameter(OAuth2Parameter.ERROR_DESCRIPTION);
		String errorUri = request.getParameter(OAuth2Parameter.ERROR_URI);
		String state = request.getParameter(OAuth2Parameter.STATE);

		response = new ErrorResponseAttributes(error, errorDescription, errorUri, state);

		return response;
	}
}
