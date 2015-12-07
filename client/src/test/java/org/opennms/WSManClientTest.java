package org.opennms;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WSManClientTest {

    @Test
    public void testIt() throws Exception {
        WSManClient wsc = new WSManClient();
        assertEquals(1.0d, wsc.getIt(), 0.0001);
    }
}
