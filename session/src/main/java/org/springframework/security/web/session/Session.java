package org.springframework.security.web.session;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.security.web.session.serializers.SessionDeserializer;
import org.springframework.security.web.session.serializers.SessionSerializer;

import java.io.Serializable;
import java.util.Set;

/**
 * @author Rob Winch
 */
@JsonSerialize(using = SessionSerializer.class)
@JsonDeserialize(using = SessionDeserializer.class)
public interface Session extends Serializable {
    void setLastAccessedTime(long lastAccessedTime);

    long getCreationTime();

    String getId();

    long getLastAccessedTime();

    void setMaxInactiveInterval(int interval);

    int getMaxInactiveInterval();

    Object getAttribute(String name);

    Set<String> getAttributeNames();

    void setAttribute(String name, Object value);

    void removeAttribute(String name);
}
