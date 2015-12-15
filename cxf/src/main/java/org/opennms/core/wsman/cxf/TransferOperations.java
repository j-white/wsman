/*
 * Copyright 2015, The OpenNMS Group
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opennms.core.wsman.cxf;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.Action;
import javax.xml.ws.soap.Addressing;

import org.xmlsoap.schemas.ws._2004._09.transfer.ObjectFactory;
import org.xmlsoap.schemas.ws._2004._09.transfer.TransferElement;

/**
 * This port type defines a resource that may be read, written, and deleted.
 *
 * See org.xmlsoap.schemas.ws._2004._09.transfer.Resource for generated class.
 *
 * @author jwhite
 */
@WebService(targetNamespace = "http://schemas.xmlsoap.org/ws/2004/09/transfer", name = "Resource")
@XmlSeeAlso({ObjectFactory.class, org.xmlsoap.schemas.ws._2004._08.addressing.ObjectFactory.class})
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@Addressing(required = false, enabled = true)
public interface TransferOperations {

    @WebMethod(operationName = "Get")
    @Action(input = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Get", output = "http://schemas.xmlsoap.org/ws/2004/09/transfer/GetResponse")
    @WebResult(name = "TransferElement", targetNamespace = "http://schemas.xmlsoap.org/ws/2004/09/transfer", partName = "Body")
    public TransferElement get();

    @WebMethod(operationName = "Put")
    @Action(input = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Put", output = "http://schemas.xmlsoap.org/ws/2004/09/transfer/PutResponse")
    public void put(
        @WebParam(partName = "Body", mode = WebParam.Mode.INOUT, name = "TransferElement", targetNamespace = "http://schemas.xmlsoap.org/ws/2004/09/transfer")
        javax.xml.ws.Holder<TransferElement> body
    );

    @WebMethod(operationName = "Delete")
    @Action(input = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete", output = "http://schemas.xmlsoap.org/ws/2004/09/transfer/DeleteResponse")
    public void delete();
}