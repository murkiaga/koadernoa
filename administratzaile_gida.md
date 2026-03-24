# Administratzaile gida

## Sarrera

Dokumentu hau **administratzaile rola** duten erabiltzaileentzat da. Administratzailearen eginkizuna ez da eguneroko kudeaketa akademikoa egitea, baizik eta aplikazioaren **konfigurazio globala**, **sarbide-sistema** eta **oinarrizko identitate bisuala** mantentzea.

Gaur egungo aplikazio-egituraren arabera, administrazio panelak lau bloke nagusi ditu:

- Logoa
- Ebaluazio koloreak
- Autentikazioa
- Logak

## 1. Administrazio panelera sarrera

Administrazio atala `/admin` bidean dago. Bertan ikusten den informazioa eta egin daitezkeen aldaketak sistema osoari eragiten diote; beraz, gomendagarria da administrazio-lanak erabiltzaile gutxi batzuek soilik egitea.

## 2. Logoa kudeatu

Administratzaileak aplikazioaren logo instituzionala igo edo ezabatu dezake.

### Onartutako formatuak
- PNG
- JPG / JPEG

### Muga nagusiak
- fitxategi mota egokia izan behar du,
- eta gehienezko tamaina **300 KB** da.

### Gomendio praktikoak
- erabili logo horizontal edo sinple bat,
- optimizatu fitxategia igo aurretik,
- eta saiatu atzeko plano garbia edo gardenarekin lan egiten.

### Zer gertatzen da logoa aldatzean?
Sistema osoan erakusten den identitate bisuala eguneratzen da. Beraz, aldaketa txikia izan arren, erabiltzaile guztiek nabarituko dute.

## 3. Ebaluazio koloreak konfiguratu

Administrazio panelean ebaluazio bakoitzerako koloreak definitu daitezke. Gaur egungo konfigurazioan, gutxienez hiru balio hauek kudeatzen dira:

- 1. ebaluazioaren kolorea
- 2. ebaluazioaren kolorea
- 3. ebaluazioaren kolorea

### Zertarako balio dute?
Kolore hauek erabiltzaile-interfazean lagungarri dira:
- egutegi edo denboralizazio ikuspegietan,
- ebaluazioen bereizketa bisualerako,
- eta erabiltzaile guztientzat irakurketa argiagoa izateko.

### Gomendioak
- aukeratu elkarren artean bereizten diren koloreak,
- mantendu kontraste egokia,
- eta saihestu oso tonu ilunak edo irakurgarritasuna zailtzen duten koloreak.

## 4. Autentikazioa kudeatu

Aplikazioak gaur egun bi autentikazio-hornitzaile nagusi onartzen ditu:

- **Google**
- **LDAP**

Administratzaileak hornitzaile horiek aktibatu edo desaktibatu ditzake, baina arau garrantzitsu batekin:

> Gutxienez autentikazio mota bat aktibo egon behar da.

### Kontzeptu garrantzitsua: “konfiguratuta” vs “aktibo”
Hornitzaile bat:
- **konfiguratuta** egon daiteke ingurune-aldagaien edo properties fitxategien bidez,
- baina administrazio panelean **aktibo** edo **ez-aktibo** jarri behar da benetan erabil dadin.

### Google gaitzeko
Beharrezkoa da gutxienez:
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`

edo horien baliokideak Spring konfigurazioan.

### LDAP gaitzeko
Beharrezkoa da gutxienez:
- LDAP zerbitzariaren URLa,
- base DN-a,
- eta erabiltzaileen bilaketa konfigurazioa.

### Gomendioak
- Ez aktibatu autentikazio mota bat aurrez benetan probatu gabe.
- Ez desaktibatu unean erabiltzaile gehienek erabiltzen duten hornitzailea alternatiba egonkor bat prestatu gabe.
- Egiaztatu beti administrazio-panelean “configured” egoera egokia dela.

## 5. Logak kontsultatu

Administratzaile panelak log edo erregistroen kontsulta ere eskaintzen du.

### Zer egin daiteke?
- log zerrenda berrikusi,
- ekintza mota baten arabera filtratu,
- eragilearen arabera bilatu,
- eta data-tarte baten barruko eragiketak aztertu.

### Zertarako da erabilgarria?
- arazoak ikertzeko,
- erabiltzaileek egindako ekintzak trazatzeko,
- konfigurazio-aldaketa baten ondorioak ulertzeko,
- eta laguntza edo auditoriako egoeretan.

### Gomendioak
- Gorabehera bat gertatzen denean, jaso lehenik gutxi gorabeherako data eta erabiltzailea.
- Logak berrikustean, saiatu ekintza bakarrera ez mugatzen: aurretik eta ondoren gertatutakoa ere aztertu.

## 6. Ingurune-konfigurazioa

Koaderno Berriaren instalazio teknikoak ingurune-aldagai edo konfigurazio-propietate batzuk behar ditu.

### Oinarrizko multzoak
- Datu-basea (MySQL)
- Google OAuth2
- LDAP
- Uploads direktorioa
- Multipart tamaina-mugak
- Logging mailak

### Aplikazioaren portua
Aplikazioa lehenespenez **8091** portuan exekutatzen da.

### Uploads direktorioa
Aplikazioak fitxategi igoerak gordetzeko direktorio bat erabiltzen du. Bereziki garrantzitsuak dira:
- ikasleen fitxategiak edo irudiak,
- eta aplikazioaren logoa.

## 7. Docker bidezko hedapena

Repoan `docker-compose.yml` fitxategia dago, eta bertan hiru zerbitzu nagusi ageri dira:

- **Traefik**
- **MySQL**
- **app**

### Horrek zer esan nahi du praktikan?
- Traefik reverse proxy eta TLS amaierarako erabiltzen da.
- MySQL datu-base gisa erabiltzen da.
- Aplikazioa container batean exekutatzen da.

### HTTPS eta ziurtagiriak
Repoaren docker konfigurazioan Traefik + Let’s Encrypt erabiltzen denez, **80 eta 443 portuak** behar bezala eskuragarri egotea garrantzitsua da ziurtagiriak lortzeko eta berritzeko.

## 8. Ohiko mantentze-lanak

Administratzailearen eguneroko mantentze-lanen artean hauek egon daitezke:

- logoa eguneratzea,
- ebaluazio-koloreak doitzea,
- autentikazio hornitzaileen egoera berrikustea,
- logak kontsultatzea,
- konfigurazio-aldagaien balioak gainbegiratzea,
- eta hedapen ingurunea (Docker/Traefik/MySQL) osasuntsu dagoela egiaztatzea.

## 9. Aldaketa sentikorrak egin aurretik

Aldaketa sentikorren artean sartzen dira:
- autentikazioaren aldaketak,
- datu-base konfigurazioaren aldaketak,
- logo edo identitate instituzionalaren aldaketak,
- eta proxy edo TLS konfigurazioaren ukituak.

### Gomendioak
- Aldaketa aurretik backup edo plan argi bat izan.
- Ahal bada, lehenik proba-ingurune batean egiaztatu.
- Zuzenean produkzioan aldatu aurretik, kudeatzaileei edo arduradunei abisatu.

## 10. Arazo arruntak

### Ezin da Google aktibatu
Normalean arrazoia:
- client id / secret ez egotea,
- edo konfigurazioa osatu gabe egotea.

### Ezin da LDAP aktibatu
Normalean arrazoia:
- URL, base edo user search konfigurazioa falta izatea.

### Ezin dira erabiltzaileak saioa hasi
Berrikusi:
- zein autentikazio mota dauden aktibo,
- benetan konfiguratuta dauden,
- eta logetan zer ageri den.

### Logoa igo ezin da
Egiaztatu:
- fitxategia PNG edo JPG dela,
- tamaina 300 KB baino txikiagoa dela,
- eta uploads direktorioa idazgarria dela.

## 11. Laburpena

Administratzailearen rola honela laburbil daiteke:

- aplikazioaren itxura orokorra zaintzea,
- sarbide-sistema gobernatzea,
- trazabilitatea bermatzeko logak erabiltzea,
- eta instalazio teknikoaren oinarrizko osasuna mantentzea.

Beste modu batera esanda, administratzaileak ez du koadernoaren eguneroko erabilera pedagogikoa gidatzen; horren ordez, **sistema bera egonkor eta erabilgarri mantentzen du**.
