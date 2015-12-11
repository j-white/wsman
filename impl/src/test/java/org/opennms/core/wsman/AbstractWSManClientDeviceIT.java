package org.opennms.core.wsman;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opennms.core.wsman.WSManClientFactory;
import org.opennms.core.wsman.WSManEndpoint;
import org.opennms.core.wsman.utils.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;

/**
 * This test connects to an iDrac device using the properties
 * store in ~/wsman.properties.
 *
 * @author jwhite
 */
public abstract class AbstractWSManClientDeviceIT {
    private final static Logger LOG = LoggerFactory.getLogger(AbstractWSManClientDeviceIT.class);

    private WSManClient client;

    public abstract WSManClientFactory getFactory();

    @BeforeClass
    public static void setupClass() {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
    }

    @Before
    public void setUp() throws IOException {
        WSManEndpoint endpoint = TestUtils.getEndpointFromLocalConfiguration();
        LOG.info("Using endpoint: {}", endpoint);
        client = getFactory().getClient(endpoint);
    }

    @Test
    public void canGetInputVoltage() {
        List<Node> powerSupplies = client.enumerateAndPullUsingFilter(
                WSManConstants.XML_NS_WQL_DIALECT,
                "select DeviceDescription,PrimaryStatus,TotalOutputPower,InputVoltage,Range1MaxInputPower,FirmwareVersion,RedundancyStatus from DCIM_PowerSupplyView where DetailedState != 'Absent' and PrimaryStatus != 0",
                WSManConstants.CIM_ALL_AVAILABLE_CLASSES);
        assertEquals(1, powerSupplies.size());

        XMLTag tag = XMLDoc.from(powerSupplies.get(0), true);
        int inputVoltage = Integer.valueOf(tag.gotoChild("n1:InputVoltage").getText());
        assertEquals(120, inputVoltage);
    }
}
