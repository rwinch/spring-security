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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.security.web.session.Session;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Rob Winch
 */
public class SessionSerializer extends JsonSerializer<Session> {
    @Override
    public void serialize(Session value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeStartObject();
        jgen.writeStringField("id", value.getId());
        jgen.writeNumberField("creationTime", value.getCreationTime());
        jgen.writeNumberField("lastAccessedTime", value.getLastAccessedTime());
        jgen.writeNumberField("maxInactiveInterval", value.getMaxInactiveInterval());

        Map<String, Object> attributes = new HashMap<String, Object>(value.getAttributeNames().size());
        for(String attrName : value.getAttributeNames()) {
            attributes.put(attrName, value.getAttribute(attrName));
        }
        jgen.writeObjectField("attributes", attributes);
        jgen.writeEndObject();
    }
}
