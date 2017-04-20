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
package org.springframework.security.oauth2.core.endpoint;

import org.springframework.security.oauth2.core.OAuth2Error;

/**
 * @author Joe Grandja
 */
public final class ErrorResponseAttributes extends OAuth2Error {
	private final String state;

	public ErrorResponseAttributes(String errorCode) {
		this(errorCode, null, null);
	}

	public ErrorResponseAttributes(String errorCode, String description, String uri) {
		this(errorCode, description, uri, null);
	}

	public ErrorResponseAttributes(String errorCode, String description, String uri, String state) {
		super(errorCode, description, uri);
		this.state = state;
	}

	public String getState() {
		return this.state;
	}
}
