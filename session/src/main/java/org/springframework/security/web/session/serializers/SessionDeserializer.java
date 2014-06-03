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
package org.springframework.security.web.session.serializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.security.web.session.MapSession;
import org.springframework.security.web.session.Session;

import java.io.IOException;
import java.util.Iterator;

/**
 * @author Rob Winch
 */
public class SessionDeserializer extends JsonDeserializer<Session> {
    @Override
    public Session deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = jp.readValueAsTree();
        String id = node.get("id").textValue();
        long creationTime = node.get("creationTime").longValue();
        long lastAccessedTime = node.get("lastAccessedTime").longValue();
        int maxInactiveInterval = node.get("maxInactiveInterval").asInt();

        MapSession session = new MapSession();
        ObjectNode a = (ObjectNode) node.get("attributes");

        Iterator<String> iAttrNames = a.fieldNames();
        while(iAttrNames.hasNext()) {
            String attrName = iAttrNames.next();
            Object attrValue = a.get(attrName).asText();
            session.setAttribute(attrName, attrValue);
        }
        session.setId(id);
        session.setCreationTime(creationTime);
        session.setLastAccessedTime(lastAccessedTime);
        session.setMaxInactiveInterval(maxInactiveInterval);

        return session;
    }
}
