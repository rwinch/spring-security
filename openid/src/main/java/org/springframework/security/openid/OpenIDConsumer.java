/*
 * Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
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

package org.springframework.security.openid;

import javax.servlet.http.HttpServletRequest;

/**
 * An interface for OpenID library implementations
 *
 * @author Ray Krueger
 * @author Robin Bramley, Opsera Ltd
 * @deprecated The OpenID 1.0 and 2.0 protocols have been deprecated and users are
 * <a href="https://openid.net/specs/openid-connect-migration-1_0.html">encouraged to
 * migrate</a> to <a href="https://openid.net/connect/">OpenID Connect</a>, which is
 * supported by <code>spring-security-oauth2</code>.
 */
@Deprecated
public interface OpenIDConsumer {

	/**
	 * Given the request, the claimedIdentity, the return to url, and a realm, lookup the
	 * openId authentication page the user should be redirected to.
	 * @param req HttpServletRequest
	 * @param claimedIdentity String URI the user presented during authentication
	 * @param returnToUrl String URI of the URL we want the user sent back to by the OP
	 * @param realm URI pattern matching the realm we want the user to see
	 * @return String URI to redirect user to for authentication
	 * @throws OpenIDConsumerException if anything bad happens
	 */
	String beginConsumption(HttpServletRequest req, String claimedIdentity, String returnToUrl, String realm)
			throws OpenIDConsumerException;

	OpenIDAuthenticationToken endConsumption(HttpServletRequest req) throws OpenIDConsumerException;

}
