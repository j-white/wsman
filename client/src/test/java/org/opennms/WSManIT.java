package org.opennms;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.xmlsoap.schemas.ws._2004._09.enumeration.FilterType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.ItemListType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Pull;
import org.xmlsoap.schemas.ws._2004._09.enumeration.PullResponse;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerateResponse;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerationContextType;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class WSManIT {

    private WSManCxfClient client = new WSManCxfClient("127.0.0.1", 8089, "/wsman", "http", "admin", "admin");
    
    @BeforeClass
    public static void setupClass() {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);
    
    @Test
    public void canEnumerate() throws InterruptedException {
        stubFor(post(urlEqualTo("/wsman"))
                .willReturn(aResponse()
                    .withHeader("Content-Type", "Content-Type: application/soap+xml; charset=utf-8")
                    .withBodyFile("enum-response.xml")));

        FilterType filter = new FilterType();
        filter.setDialect("http://schemas.microsoft.com/wbem/wsman/1/WQL");
        filter.getContent().add("select DeviceDescription,PrimaryStatus,TotalOutputPower,InputVoltage,Range1MaxInputPower,FirmwareVersion,RedundancyStatus from DCIM_PowerSupplyView where DetailedState != 'Absent' and PrimaryStatus != 0");

        Enumerate msg = new Enumerate();
        msg.setFilter(filter);
        EnumerateResponse enumResponse = client.getEnumerator().enumerate(msg);
        
        List<LoggedRequest> requests = findAll(postRequestedFor(urlMatching("/.*")));
        for (LoggedRequest r : requests) {
            System.out.println(prettyFormat(r.getBodyAsString(), 4));
        }

        assertEquals("c6595ee1-2664-1664-801f-c115cfb5fe14", enumResponse.getEnumerationContext().getContent().iterator().next());
    }

    @Test
    public void canPull() throws InterruptedException {
        stubFor(post(urlEqualTo("/wsman"))
                .willReturn(aResponse()
                    .withHeader("Content-Type", "Content-Type: application/soap+xml; charset=utf-8")
                    .withBodyFile("pull-response.xml")));

        EnumerationContextType enumContextType = new EnumerationContextType();
        enumContextType.getContent().add("c6595ee1-2664-1664-801f-c115cfb5fe14");
        Pull pull = new Pull();
        pull.setEnumerationContext(enumContextType);

        PullResponse pullResponse = client.getEnumerator().pull(pull);
        ItemListType itemList = pullResponse.getItems();
        // Note: Items in list is of type com.sun.org.apache.xerces.internal.dom.ElementNSImpl
        assertEquals(1, itemList.getAny().size());
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
