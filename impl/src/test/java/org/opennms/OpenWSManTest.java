package org.opennms;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.openwsman.Client;

public class OpenWSManTest {

    private String hostname;
    private int port;
    private String url;
    private String protocol;
    private String username;
    private String password;

    @Before
    public void setUp() throws IOException {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream(new File(System.getProperty("user.home"), "wsman.properties"))) {
            prop.load(input);
            hostname = prop.getProperty("hostname" , "127.0.0.1");
            port = Integer.valueOf(prop.getProperty("port", "443"));
            protocol = prop.getProperty("protocol" , "https");
            url = prop.getProperty("url" , "/wsman");
            username = prop.getProperty("username" , "admin");
            password = prop.getProperty("password" , "admin");
        }
    }

    @Test
    public void canGetInputVoltage() {
        Client client = new Client(hostname, port, url, protocol, username, password);
        OpenmWSManClient wrapper = new OpenmWSManClient();
        assertEquals(120, wrapper.getInputVoltage(client));
    }
}
