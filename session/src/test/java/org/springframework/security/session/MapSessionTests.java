package org.springframework.security.session;

import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.*;

public class MapSessionTests {

    private MapSession session;

    @Before
    public void setup() {
        session = new MapSession();
    }

    /**
     * Ensure conforms to the javadoc of {@link Session}
     */
    @Test
    public void setAttributeNullObjectRemoves() {
        String attr = "attr";
        session.setAttribute(attr, new Object());
        session.setAttribute(attr, null);
        assertThat(session.getAttributeNames()).isEmpty();
    }
}