# Overview

Prototype code for a pure Java WS-Man collector. 

## DMTF WSDLs
* Grab WSDLs from WSDLs https://www.dmtf.org/standards/wsman
* Replace resource-specific-GED with 'TransferElement'  `sed -i 's/resource-specific-GED/tns:TransferElement/g' *.wsdl`

## Debugging HTTPS Requests

```
mitmproxy -R https://idracthost:443 -p 4443
```

## Examples
### Enumeration request


```xml
<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope" xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing" xmlns:wsen="http://schemas.xmlsoap.org/ws/2004/09/enumeration" xmlns:wsman="http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd">
   <s:Header>
      <wsa:Action s:mustUnderstand="true">http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate</wsa:Action>
      <wsa:To s:mustUnderstand="true">https://127.0.0.1:4443/wsman</wsa:To>
      <wsman:ResourceURI s:mustUnderstand="true">http://schemas.dmtf.org/wbem/wscim/1/*</wsman:ResourceURI>
      <wsa:MessageID s:mustUnderstand="true">uuid:f6e752bc-2668-1668-8002-303180ea2ae8</wsa:MessageID>
      <wsa:ReplyTo>
         <wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address>
      </wsa:ReplyTo>
   </s:Header>
   <s:Body>
      <wsen:Enumerate>
         <wsman:Filter Dialect="http://schemas.microsoft.com/wbem/wsman/1/WQL">select DeviceDescription,PrimaryStatus,TotalOutputPower,InputVoltage,Range1MaxInputPower,FirmwareVersion,RedundancyStatus from DCIM_PowerSupplyView where DetailedState != 'Absent' and PrimaryStatus != 0</wsman:Filter>
      </wsen:Enumerate>
   </s:Body>
</s:Envelope>
```

### Enumeration response


```xml
<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope" xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing" xmlns:wsen="http://schemas.xmlsoap.org/ws/2004/09/enumeration">
   <s:Header>
      <wsa:To>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>
      <wsa:Action>http://schemas.xmlsoap.org/ws/2004/09/enumeration/EnumerateResponse</wsa:Action>
      <wsa:RelatesTo>uuid:f6e752bc-2668-1668-8002-303180ea2ae8</wsa:RelatesTo>
      <wsa:MessageID>uuid:c6610461-2664-1664-8020-c115cfb5fe14</wsa:MessageID>
   </s:Header>
   <s:Body>
      <wsen:EnumerateResponse>
         <wsen:EnumerationContext>c6595ee1-2664-1664-801f-c115cfb5fe14</wsen:EnumerationContext>
      </wsen:EnumerateResponse>
   </s:Body>
</s:Envelope>
```

### Pull Request


```xml
<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope" xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing" xmlns:wsen="http://schemas.xmlsoap.org/ws/2004/09/enumeration" xmlns:wsman="http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd">
   <s:Header>
      <wsa:Action s:mustUnderstand="true">http://schemas.xmlsoap.org/ws/2004/09/enumeration/Pull</wsa:Action>
      <wsa:To s:mustUnderstand="true">https://127.0.0.1:4443/wsman</wsa:To>
      <wsman:ResourceURI s:mustUnderstand="true">http://schemas.dmtf.org/wbem/wscim/1/*</wsman:ResourceURI>
      <wsa:MessageID s:mustUnderstand="true">uuid:f715a46e-2668-1668-8003-303180ea2ae8</wsa:MessageID>
      <wsa:ReplyTo>
         <wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address>
      </wsa:ReplyTo>
   </s:Header>
   <s:Body>
      <wsen:Pull>
         <wsen:EnumerationContext>c6595ee1-2664-1664-801f-c115cfb5fe14</wsen:EnumerationContext>
      </wsen:Pull>
   </s:Body>
</s:Envelope>
```

### Pull Response


```xml
<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope" xmlns:n1="http://schemas.dell.com/wbem/wscim/1/cim-schema/2/DCIM_PowerSupplyView" xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing" xmlns:wsen="http://schemas.xmlsoap.org/ws/2004/09/enumeration">
   <s:Header>
      <wsa:To>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>
      <wsa:Action>http://schemas.xmlsoap.org/ws/2004/09/enumeration/PullResponse</wsa:Action>
      <wsa:RelatesTo>uuid:f715a46e-2668-1668-8003-303180ea2ae8</wsa:RelatesTo>
      <wsa:MessageID>uuid:c664ae53-2664-1664-8021-c115cfb5fe14</wsa:MessageID>
   </s:Header>
   <s:Body>
      <wsen:PullResponse>
         <wsen:Items>
            <n1:DCIM_PowerSupplyView>
               <n1:FirmwareVersion>04.15.00</n1:FirmwareVersion>
               <n1:InputVoltage>120</n1:InputVoltage>
               <n1:InstanceID>PSU.Slot.1</n1:InstanceID>
               <n1:PrimaryStatus>1</n1:PrimaryStatus>
               <n1:RedundancyStatus>0</n1:RedundancyStatus>
               <n1:TotalOutputPower>502</n1:TotalOutputPower>
            </n1:DCIM_PowerSupplyView>
         </wsen:Items>
         <wsen:EndOfSequence />
      </wsen:PullResponse>
   </s:Body>
</s:Envelope>
```
