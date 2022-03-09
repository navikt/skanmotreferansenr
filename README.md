Skanmotreferansenr
================
Skanmotreferansenr arkiverer inngåande dokument som har forside frå [fyrstesidegeneratoren](https://confluence.adeo.no/display/BOA/Foerstesidegenerator).
Appen arkiverer dei inngåande dokumenta saman med metadata som blir henta frå fyrstesidedatabasa.

Du finn meir informasjon om  [skanmotreferansenr på Confluence](https://confluence.adeo.no/display/BOA/skanmotreferansenr).

Dekryptering av filer frå Iron Mountain kan skje på to måtar:
- dekryptering av aes-krypterte filer
- dekryptering av pgp-krypterte filer

I fylgjande lenke finn du [oppskrift for skifte av pgp-nøkkelpar](https://confluence.adeo.no/display/BOA/PGP-kryptering+for+filer).

## Testing
### Oppskrift for testing av pgp-dekryptering i dev
To måtar som er nyttige, der dette er måte nr. 1:
* Få tilgang til mappa /inbound/skanmotreferansenr på sftpserveren sftp-irmo-q.nav.no (devmiljøet). 
* Flytt ei allereie eksisterande .zip.pgp-fil frå /inbound/skanmotreferansenr/processed til /inbound/skanmotreferansenr og vent til appen hentar fila (skjer kvart 5. minutt).

Måte nr. 2: 
* Få tilgang til mappa /inbound/skanmotreferansenr på sftpserveren sftp-irmo-q.nav.no (devmiljøet).
* Legg inn noverande dev-public key for appen i pgp-mappa i /resources som du finn i skanmotreferansenr/keys i vault. Denne har namnet pgppublickey.
* I getPublicKey()-metoda i PGPManualTest-testklassa kan du vise til fila over.
* Krypter ei .zip-mappe i skanmotreferansenr-prosjektet frå /resources-mappa ved å bruke generateEncryptedFile() i PGPManualTest-testklassa.
* Legg inn ei .zip.pgp-fil på /inbound/skanmotreferansenr og vent til appen hentar fila (skjer kvart 5. minutt). Det kan vere praktisk å maile denne til seg sjølv dersom ein arbeider lokalt og skal hente den ut på utviklarimage.

## Førespurnadar
Spørsmål om koda eller prosjektet kan stillast på Slack-kanalen til Team Dokumentløsninger:
[\#Team Dokumentløsninger](https://nav-it.slack.com/client/T5LNAMWNA/C6W9E5GPJ)
