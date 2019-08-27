[![CircleCI](https://circleci.com/gh/navikt/peproxy.svg?style=svg)](https://circleci.com/gh/navikt/peproxy)
# peproxy

REST applikasjon som fungerer som en proxy mellom fss og omverden.

```bash
curl https://peproxy.nais.preprod.local -H "target: <url>"

#   <!DOCTYPE html>
#   <html>
#       ...
#   </html>
```
## Metrikker
[Grafana dashboard](https://grafana.adeo.no/d/6IzDnOVWk/peproxy)

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #peon.