package org.opennms;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerateResponse;
import org.xmlsoap.schemas.ws._2004._09.enumeration.FilterType;

public class WSManDeviceIT {
    
    private WSManCxfClient client;
    private String hostname;
    private int port;
    private String url;
    private String protocol;
    private String username;
    private String password;

    @BeforeClass
    public static void setupClass() {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
    }

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

        client = new WSManCxfClient(hostname, port, url, protocol, username, password);
    }

    @Test
    public void canGetInputVoltage() {
        // Prepare request
        FilterType filter = new FilterType();
        filter.setDialect("http://schemas.microsoft.com/wbem/wsman/1/WQL");
        filter.getContent().add("select DeviceDescription,PrimaryStatus,TotalOutputPower,InputVoltage,Range1MaxInputPower,FirmwareVersion,RedundancyStatus from DCIM_PowerSupplyView where DetailedState != 'Absent' and PrimaryStatus != 0");
        Enumerate msg = new Enumerate();
        msg.setFilter(filter);
        
        // Make the call
        EnumerateResponse enumResponse = client.getEnumerator().enumerate(msg);
        assertNotNull(enumResponse);
    }
}
