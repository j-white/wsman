package org.opennms;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class WSManIT {
    
    @BeforeClass
    public static void setupClass() {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);
    
    @Test
    public void canEnumerate() throws InterruptedException {
        final String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
"<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\">" +
   "<s:Header>" +
      "<wsa:To>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>" +
      "<wsa:Action>http://schemas.xmlsoap.org/ws/2004/09/enumeration/EnumerateResponse</wsa:Action>" +
      "<wsa:RelatesTo>uuid:f6e752bc-2668-1668-8002-303180ea2ae8</wsa:RelatesTo>" +
      "<wsa:MessageID>uuid:c6610461-2664-1664-8020-c115cfb5fe14</wsa:MessageID>" +
   "</s:Header>" +
   "<s:Body>" +
      "<wsen:EnumerateResponse>" +
         "<wsen:EnumerationContext>c6595ee1-2664-1664-801f-c115cfb5fe14</wsen:EnumerationContext>" +
      "</wsen:EnumerateResponse>" +
   "</s:Body>" +
"</s:Envelope>";
        stubFor(post(urlEqualTo("/wsman"))
                .willReturn(aResponse()
                    .withHeader("Content-Type", "Content-Type: application/soap+xml; charset=utf-8")
                    .withBody(response)));
                    

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(EnumerationOperations.class);
        factory.setAddress("http://127.0.0.1:8089/wsman");
        factory.setBindingId("http://schemas.xmlsoap.org/wsdl/soap12/");
        EnumerationOperations enumerator = factory.create(EnumerationOperations.class);

        FilterType filter = new FilterType();
        filter.setDialect("http://schemas.microsoft.com/wbem/wsman/1/WQL");
        filter.getContent().add("select DeviceDescription,PrimaryStatus,TotalOutputPower,InputVoltage,Range1MaxInputPower,FirmwareVersion,RedundancyStatus from DCIM_PowerSupplyView where DetailedState != 'Absent' and PrimaryStatus != 0");

        Enumerate msg = new Enumerate();
        msg.setFilter(filter);
        EnumerateResponse enumResponse = enumerator.enumerate(msg);
        assertEquals("c6595ee1-2664-1664-801f-c115cfb5fe14", enumResponse.getEnumerationContext().getContent().iterator().next());

        /*
        List<LoggedRequest> requests = findAll(postRequestedFor(urlMatching("/.*")));
        for (LoggedRequest r : requests) {
            System.out.println(prettyFormat(r.getBodyAsString(), 4));
        }
        */
    }

    @Test
    public void canPull() throws InterruptedException {
        final String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
"<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:n1=\"http://schemas.dell.com/wbem/wscim/1/cim-schema/2/DCIM_PowerSupplyView\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\">" +
   "<s:Header>" +
      "<wsa:To>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>" +
      "<wsa:Action>http://schemas.xmlsoap.org/ws/2004/09/enumeration/PullResponse</wsa:Action>" +
      "<wsa:RelatesTo>uuid:f715a46e-2668-1668-8003-303180ea2ae8</wsa:RelatesTo>" +
      "<wsa:MessageID>uuid:c664ae53-2664-1664-8021-c115cfb5fe14</wsa:MessageID>" +
   "</s:Header>" +
   "<s:Body>" +
      "<wsen:PullResponse>" +
         "<wsen:Items>" +
            "<n1:DCIM_PowerSupplyView>" +
               "<n1:FirmwareVersion>04.15.00</n1:FirmwareVersion>" +
               "<n1:InputVoltage>120</n1:InputVoltage>" +
               "<n1:InstanceID>PSU.Slot.1</n1:InstanceID>" +
               "<n1:PrimaryStatus>1</n1:PrimaryStatus>" +
               "<n1:RedundancyStatus>0</n1:RedundancyStatus>" +
               "<n1:TotalOutputPower>502</n1:TotalOutputPower>" +
            "</n1:DCIM_PowerSupplyView>" +
         "</wsen:Items>" +
         "<wsen:EndOfSequence />" +
      "</wsen:PullResponse>" +
   "</s:Body>" +
"</s:Envelope>";

        stubFor(post(urlEqualTo("/wsman"))
                .willReturn(aResponse()
                    .withHeader("Content-Type", "Content-Type: application/soap+xml; charset=utf-8")
                    .withBody(response)));
                    

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(EnumerationOperations.class);
        factory.setAddress("http://127.0.0.1:8089/wsman");
        factory.setBindingId("http://schemas.xmlsoap.org/wsdl/soap12/");
        EnumerationOperations enumerator = factory.create(EnumerationOperations.class);

        EnumerationContextType enumContextType = new EnumerationContextType();
        enumContextType.getContent().add("c6595ee1-2664-1664-801f-c115cfb5fe14");
        Pull pull = new Pull();
        pull.setEnumerationContext(enumContextType);

        PullResponse pullResponse = enumerator.pull(pull);
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
