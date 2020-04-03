# peproxy

`peproxy` is a REST application that acts as a proxy between fss and the internet.

The httpRequest that is sent to the `peproxy` will be forwarded towards client with the same setup such as httpMethod, body and header fields. 


## HttpRequest settup
Outside the http forbidden header name, there are some header fields that will not be sent to the client. These header fields are used for setting up the call to the proxy.

<strong>Header fields used by application</strong>: <br/>
`Authorization`: This field is required if call is called inside fss.<br/>
`target-authorization`: optional, sett the authentication header towards client.<br/>
`max-age`: optional, ttl for the httpRequest call<br/>
`target-url`: required, set the url for the proxy client<br/>

List of forbidden header name: https://developer.mozilla.org/en-US/docs/Glossary/Forbidden_header_name

## Example of httpRequest with curl
```bash
curl https://peproxy.nais.preprod.local 
    -H "Authorization: <token>" 
    -H "target-authorization: <clientToken>" 
    -H "max-age: <ttl>" 
    -H "target-url: <clientUrl>" 
    -H "Content-Type: <Content-Type>"
    -d "<body>"
```
## Metrics
[Grafana dashboard](https://grafana.adeo.no/d/6IzDnOVWk/peproxy)

## For NAV-ansatte

Interal inquiries can be sendt through slack in the channel `#samhandling_pensjonsområdet`.