package org.opennms;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.xmlsoap.schemas.ws._2004._09.transfer.ResourceFactory;

/**
 * WSDLs https://www.dmtf.org/standards/wsman
 * 
 * * Replace resource-specific-GED with 'TransferElement'
 * ** sed -i 's/resource-specific-GED/tns:TransferElement/g' *.wsdl
 *
 */
public class WSManClient  {
    
    public double getIt() throws Exception {
        
        return 0.0d;
    }
}
