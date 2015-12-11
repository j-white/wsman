package org.opennms.core.wsman;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.opennms.core.wsman.WSManEndpoint;
import org.opennms.core.wsman.WSManEndpoint.WSManVersion;
import org.w3c.dom.Node;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * This test connects to a local HTTP server provided
 * by WireMock that returns static content.
 *
 * Used to verify the generated requests and validate response parsing.
 *
 * @author jwhite
 */
public abstract class AbstractWSManClientIT {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    private WSManClient client;

    public abstract WSManClientFactory getFactory();

    @BeforeClass
    public static void setupClass() {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
    }

    @Before
    public void setUp() throws MalformedURLException {
        WSManEndpoint endpoint = new WSManEndpoint.Builder("http://127.0.0.1:8089/wsman")
                .withServerVersion(WSManVersion.WSMAN_1_0)
                .build();
        client = getFactory().getClient(endpoint);
    }

    @Test
    public void canEnumerate() throws InterruptedException {
        stubFor(post(urlEqualTo("/wsman"))
                .willReturn(aResponse()
                    .withHeader("Content-Type", "Content-Type: application/soap+xml; charset=utf-8")
                    .withBodyFile("enum-response.xml")));

        String contextId = client.enumerateWithWQLFilter("select DeviceDescription,PrimaryStatus,TotalOutputPower,InputVoltage,Range1MaxInputPower,FirmwareVersion,RedundancyStatus from DCIM_PowerSupplyView where DetailedState != 'Absent' and PrimaryStatus != 0",
                WSManConstants.CIM_ALL_AVAILABLE_CLASSES);

        dumpRequestsToStdout();

        assertEquals("c6595ee1-2664-1664-801f-c115cfb5fe14", contextId);
    }

    @Test
    public void canPull() throws InterruptedException {
        stubFor(post(urlEqualTo("/wsman"))
                .willReturn(aResponse()
                    .withHeader("Content-Type", "Content-Type: application/soap+xml; charset=utf-8")
                    .withBodyFile("pull-response.xml")));

        List<Node> nodes = client.pull("c6595ee1-2664-1664-801f-c115cfb5fe14", WSManConstants.CIM_ALL_AVAILABLE_CLASSES);

        dumpRequestsToStdout();

        assertEquals(1, nodes.size());

        XMLTag tag = XMLDoc.from(nodes.get(0), true);
        int inputVoltage = Integer.valueOf(tag.gotoChild("n1:InputVoltage").getText());
        assertEquals(120, inputVoltage);
    }

    private void dumpRequestsToStdout() {
        findAll(postRequestedFor(urlMatching("/.*"))).forEach(r -> System.out.println(prettyFormat(r.getBodyAsString(), 4)));
    }

    public static String prettyFormat(String input, int indent) {
        try {
            Source xmlInput = new StreamSource(new StringReader(input));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer(); 
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (Exception e) {
            throw new RuntimeException(e); // simple exception handling, please review it
        }
    }
}
