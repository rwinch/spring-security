/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.security.web.session.redis;

import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.web.session.MapSession;
import org.springframework.security.web.session.Session;
import org.springframework.security.web.session.SessionRepository;

import java.util.Map;

/**
 * @author Rob Winch
 */
public class RedisSessionRepository implements SessionRepository {
    private final String BOUNDED_HASH_KEY = "spring-security-sessions";
    private final RedisTemplate<String,Session> redisTemplate;

    public RedisSessionRepository(RedisTemplate<String, Session> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(Session session) {
        BoundHashOperations<String, Object, Object> operations = this.redisTemplate.boundHashOps(BOUNDED_HASH_KEY);
        operations.put(session.getId(),session);
    }

    @Override
    public Session getSession(String id) {
        Object map = this.redisTemplate.boundHashOps(BOUNDED_HASH_KEY).get(id);
        return (Session) map;
    }

    @Override
    public void delete(String id) {

    }

    @Override
    public Session createSession() {
        return new MapSession();
    }
}
