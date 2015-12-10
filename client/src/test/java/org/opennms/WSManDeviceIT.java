package org.opennms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opennms.wsman.Enumerate;
import org.opennms.wsman.FilterType;
import org.w3c.dom.Node;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerateResponse;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerationContextType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.ItemListType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Pull;
import org.xmlsoap.schemas.ws._2004._09.enumeration.PullResponse;

import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;

public class WSManDeviceIT {

    private String hostname;
    private int port;
    private String url;
    private String protocol;
    private String username;
    private String password;

    private WSManCxfClient client;

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
        // Enumerate
        FilterType filter = new FilterType();
        filter.setDialect("http://schemas.microsoft.com/wbem/wsman/1/WQL");
        filter.getContent().add("select DeviceDescription,PrimaryStatus,TotalOutputPower,InputVoltage,Range1MaxInputPower,FirmwareVersion,RedundancyStatus from DCIM_PowerSupplyView where DetailedState != 'Absent' and PrimaryStatus != 0");
        Enumerate msg = new Enumerate();
        msg.setFilter(filter);
                
        EnumerateResponse enumResponse = client.getEnumerator().enumerate(msg);
        assertNotNull("enumerate failed", enumResponse);

        // Pull the enumerated objects one by one (we could pull them in a single request if we wanted to)
        for (Object content : enumResponse.getEnumerationContext().getContent()) {
            String contentId = (String)content;
            
            EnumerationContextType enumContext = new EnumerationContextType();
            enumContext.getContent().add(contentId);
            Pull pull = new Pull();
            pull.setEnumerationContext(enumContext);
            
            PullResponse pullResponse = client.getEnumerator().pull(pull);
            assertNotNull("pull failed for " + contentId, pullResponse);

            // We got the items we wanted, now parse the results
            ItemListType itemList = pullResponse.getItems();
            assertEquals(1, itemList.getAny().size());

            XMLTag tag = XMLDoc.from((Node)itemList.getAny().get(0), true);
            int inputVoltage = Integer.valueOf(tag.gotoChild("n1:InputVoltage").getText());
            assertEquals(120, inputVoltage);
        }
    }
}
