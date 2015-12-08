# Overview

Prototype code for a pure Java WS-Man collector. 

## DMTF WSDLs
* Grab WSDLs from WSDLs https://www.dmtf.org/standards/wsman
* Replace resource-specific-GED with 'TransferElement'
** sed -i 's/resource-specific-GED/tns:TransferElement/g' *.wsdl

## Debugging HTTPS Requests

```
mitmproxy -R https://idracthost:443 -p 4443
```
