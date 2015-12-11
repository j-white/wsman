package org.opennms;
import org.openwsman.Client;
import org.openwsman.ClientOptions;
import org.openwsman.Filter;
import org.openwsman.XmlDoc;
import org.openwsman.XmlNode;
import org.openwsman.OpenWSManConstants;

public class OpenmWSManClient {

    public int getInputVoltage(Client client) {
        ClientOptions options = new ClientOptions();
        options.set_dump_request();
        client.transport().set_auth_method(OpenWSManConstants.BASIC_AUTH_STR);
        // Disable SSL cert check
        client.transport().set_verify_host(0);
        client.transport().set_verify_peer(0);

        Filter filter = new Filter();
        filter.wql("select DeviceDescription,PrimaryStatus,TotalOutputPower,InputVoltage,Range1MaxInputPower,FirmwareVersion,RedundancyStatus from DCIM_PowerSupplyView where DetailedState != 'Absent' and PrimaryStatus != 0");
        
        XmlDoc result = client.enumerate(options, filter, OpenWSManConstants.CIM_ALL_AVAILABLE_CLASSES);
        System.err.println(result);
        if ((result == null) || result.isFault())
            System.err.println("Enumeration failed: "
                    + ((result != null) ? result.fault().reason() : "?"));
        else {
            String context = result.context();
            while (context != null) {
                System.out.println("Context: " + context);
                result = client.pull(options, null, OpenWSManConstants.CIM_ALL_AVAILABLE_CLASSES, context);
                if (result == null || result.isFault())  {
                    System.err.println("Pull failed: " +
                            ((result != null) ? result.fault().reason() : "?"));
                    context = null;
                    continue;
                }
                System.out.println(result.encode("UTF-8"));
                XmlNode id = result.root().find(null,"InputVoltage", 1);
                        
                context = result.context();
                
                return Integer.valueOf(id.toString());
            }
        }
        return 0;
    }
}
