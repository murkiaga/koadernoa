# ETHAZI kudeaketa

Aldaketak `feature/ethazi` branchean egin dira. Sarrera kudeatzailearen navbarreko **ETHAZI** esteka da; `/ethazi/gaitasunak` helbidera doa. ADMIN eta KUDEATZAILEA rolek dute sarbidea, gainerako kudeaketa-pantailekin bat etorriz.

## Erabilera

1. **Mailakatzeak**: sortu ziklo + mota bakoitzeko eredua; ondoren gehitu nahi adina maila. Geziekin aldatu ordena, eta gorde izenen aldaketak.
2. **Gaitasunak**: hautatu zikloa eta mota. Errubrikak ereduko mailak erabiltzen ditu. Gaitasun berrian, aukeratu testuingurua eta sakatu **Mailak kargatu**. Bete kodea, izena, deskribapena eta maila bakoitzeko deskribapena.
3. Gaitasuna gordetakoan, haren edizio-pantailan gehitu/editatu/ezabatu lorpen-adierazleak. Adierazle bakoitzeko **Ikaskuntza Emaitzen loturak** atalak gaitasunaren zikloko IEak erakusten ditu, moduluka. Atal bakoitzak bere gordetze-botoia du.
4. **Ikaskuntza Emaitzak**: hautatu zikloa, ondoren modulua. Kudeatu kodea, ordena, deskribapena eta modulua.

Gaitasuna sortutakoan zikloa eta mota finkatuta geratzen dira, mailen eta curriculumeko loturen koherentzia mantentzeko. Ereduari maila berria gehituz gero, errubrikak zutabe berria berehala erakusten du; gaitasuna berriro gordetzeak falta den `GaitasunMaila` sortzen du. Gorde gabeko gaitasunaren aldaketen aurrean nabigazio-oharra dago.

## Fluxua eta errubrika dinamikoa

`EthaziController` → `EthaziService` → JPA repository-ak.

- Controller-ak DTOak lotu, binding-erroreak tratatu, bistak hautatu eta aplikazioaren `success` / `error` mezuak prestatzen ditu. Ez du JPA entitaterik zuzenean formulario gisa lotzen.
- Service-ak transakzioak, testu eta ordenen balidazioa, guraso/seme identifikatzaileen egiaztapena, zikloaren koherentzia, loturak eta ezabaketen dependentziak kudeatzen ditu.
- Repository-ek JPA bilaketak eta iraunkortasuna egiten dituzte; curriculumeko repository-ak `objektuak.modulua` paketearen barruan daude.
- `errubrika(zikloaId, mota)` metodoak dagokion eredua bilatzen du. Mailak `ordena`ren arabera ordenatzen ditu eta gaitasun bakoitzaren gelaxkak maila-IDen arabera lerrokatzen ditu. Mailarik sortu gabe badago, gelaxka hutsa itzultzen du.
- Thymeleaf-ek prestaturiko goiburuak eta gelaxkak iteratzen ditu; ez dago lau zutaberen mugarik. Gelaxketan deskribapena, lorpen-adierazleak eta `modulu-kodea · IE-kodea` badge-ak erakusten dira.
- Errubrikak scroll horizontala eta goiburu finkoa ditu. Pantaila handietan kodea eta deskribapena finkatuta daude; mugikorrean kodea bakarrik, irakurtzeko leku erabilgarria uzteko.

## Datuen babesa

- Erabiltzen ari den eredua edo maila ezabatzea blokeatzen da, mezu argiarekin.
- Mailen ordena trukatzean tarteko ordena libre bat eta flush bereiziak erabiltzen dira, `(eredua_id, ordena)` murrizketa bakarra errespetatzeko.
- Gaitasunaren ezabaketak bere mailak, lorpen-adierazleak eta IE lotura-errenkadak ezabatzen ditu, dagoen cascade/orphanRemoval ereduaren bidez. IEak mantentzen dira.
- IE bati lorpen-adierazleek erreferentzia egiten badiote, IEa ezabatzea eta beste modulu batera mugitzea blokeatzen dira.
- Beste ziklo bateko IEak lotzea eta beste gaitasun edo eredu bateko seme-IDak erabiltzea zerbitzuan baztertzen dira.
- Aldaketak POST bidez egiten dira, aplikazioaren CSRF babesarekin. Ezabatzeko formularioek baieztapena eskatzen dute.
- Balidazio-erroreetan formularioa berriro erakusten da, sartutako datuak mantenduz. Ez da ebidentzia edo pisuen logikarik gehitu, ezta IEekiko cascade berririk ere.

## Endpoint berriak

Ondoko guztiak `/ethazi` aurrizkiaren azpian daude:

| Metodoa | Bidea | Erabilera |
|---|---|---|
| GET | `/`, edo hutsik | Errubrikara birbideratu |
| GET | `/gaitasunak` | Errubrika (`zikloaId`, `mota`) |
| GET, POST | `/gaitasunak/berria` | Sortzeko formularioa / gorde |
| GET, POST | `/gaitasunak/{id}/editatu` | Editatzeko formularioa / gorde |
| POST | `/gaitasunak/{id}/ezabatu` | Ezabatu |
| POST | `/gaitasunak/{id}/mailak/{mailaId}/adierazleak/berria` | Lorpen-adierazlea sortu |
| POST | `/gaitasunak/{id}/mailak/{mailaId}/adierazleak/{adierazleaId}/editatu` | Adierazlea eta IE loturak gorde |
| POST | `/gaitasunak/{id}/mailak/{mailaId}/adierazleak/{adierazleaId}/ezabatu` | Adierazlea ezabatu |
| GET | `/mailakatzeak` | Ereduen zerrenda |
| GET, POST | `/mailakatzeak/berria` | Eredua sortu |
| GET, POST | `/mailakatzeak/{id}/editatu` | Eredua editatu |
| POST | `/mailakatzeak/{id}/ezabatu` | Eredua ezabatu |
| POST | `/mailakatzeak/{id}/mailak/berria` | Maila gehitu |
| POST | `/mailakatzeak/{id}/mailak/{mailaId}/editatu` | Mailaren izena gorde |
| POST | `/mailakatzeak/{id}/mailak/{mailaId}/ezabatu` | Maila ezabatu |
| POST | `/mailakatzeak/{id}/mailak/{mailaId}/mugitu` | Ordena aldatu (`norabidea`: -1 edo 1) |
| GET | `/ikaskuntza-emaitzak` | IE zerrenda (`zikloaId`, `moduloaId`) |
| GET, POST | `/ikaskuntza-emaitzak/berria` | IE sortu |
| GET, POST | `/ikaskuntza-emaitzak/{id}/editatu` | IE editatu |
| POST | `/ikaskuntza-emaitzak/{id}/ezabatu` | IE ezabatu |

## Fitxategiak

Sortuak:

- `src/main/java/com/koadernoa/app/ethazi/controller/EthaziController.java`
- `src/main/java/com/koadernoa/app/ethazi/service/EthaziService.java`
- `src/main/java/com/koadernoa/app/ethazi/dto/EthaziForms.java`
- `src/main/java/com/koadernoa/app/ethazi/repository/GaitasunaRepository.java`
- `src/main/java/com/koadernoa/app/ethazi/repository/GaitasunMailaRepository.java`
- `src/main/java/com/koadernoa/app/ethazi/repository/MailakatzeEreduaRepository.java`
- `src/main/java/com/koadernoa/app/ethazi/repository/MailakatzeMailaRepository.java`
- `src/main/java/com/koadernoa/app/ethazi/repository/LorpenAdierazleaRepository.java`
- `src/main/java/com/koadernoa/app/objektuak/modulua/repository/IkaskuntzaEmaitzaRepository.java`
- `src/main/resources/templates/Ethazi/fragments/navbar.html`
- `src/main/resources/templates/Ethazi/fragments/common.html`
- `src/main/resources/templates/Ethazi/fragments/adierazlea.html`
- `src/main/resources/templates/Ethazi/gaitasunak/index.html`
- `src/main/resources/templates/Ethazi/gaitasunak/form.html`
- `src/main/resources/templates/Ethazi/mailakatzeak/index.html`
- `src/main/resources/templates/Ethazi/mailakatzeak/form.html`
- `src/main/resources/templates/Ethazi/ikaskuntza-emaitzak/index.html`
- `src/main/resources/templates/Ethazi/ikaskuntza-emaitzak/form.html`
- `src/main/resources/static/css/ethazi.css`
- `src/main/resources/static/js/ethazi.js`
- `src/test/java/com/koadernoa/app/ethazi/EthaziServiceTest.java`
- `docs/ethazi-kudeaketa.md`

Aldatuak:

- `src/main/java/com/koadernoa/app/objektuak/modulua/repository/ModuloaRepository.java`: zikloko moduluen bilaketa.
- `src/main/java/com/koadernoa/app/security/SecurityConfig.java`: ETHAZIren sarbide-rolak.
- `src/main/resources/templates/kudeatzaile/fragments/navbar.html`: ETHAZI esteka mahaigaineko eta mugikorreko menuetan.
- `pom.xml`: H2, test scope-an soilik; ez da produkzioko datu-basea aldatzen.

## Egiaztapenak

`mvn -q -DskipTests compile`

`mvn -q -Dtest=EthaziServiceTest test`

ETHAZIren integrazio-probek H2 memoriazko datu-base isolatua erabiltzen dute. Maila kopuru aldakorra, ordena-trukeak, ezabaketen dependentziak, IEak mantentzea, beste zikloko loturen bazterketa, formulario zaharkituak, eredu bikoiztuak eta Thymeleaf pantaila guztien errendaketa egiaztatzen dituzte.

Azken egiaztapenaren emaitzak:

- Konpilazioa zuzena da, eta ETHAZIren 12 integrazio-probak gainditu dira.
- Proba zabalagoan (`mvn -q '-Dtest=*Test,!ApplicationTests' test`), orduko 61 probetatik 60 gainditu ziren. `IkasleaMatrikulaSyncTest`-eko inportazio-probak `NullPointerException` du `InportazioZerbitzua.kalkulatuArgazkiPath` metodoan. Errore bera berretsi da aldatu gabeko `HEAD`-en kopia isolatuan; ETHAZItik kanpoko aurretiazko akatsa da.
- `ApplicationTests` ez da exekutatu: aplikazio osoaren kanpoko zerbitzuen konfigurazioa behar du.
- Errubrikaren mahaigaineko eta 390 px-ko mugikorreko bistak nabigatzailean egiaztatu dira, baita lorpen-adierazlearen IE aukeratzailea ere. Egiaztapen bisualak probek sortutako HTML eta datu sintetikoak erabili ditu.

## Erronkak eta Kinielak

- Moduluaren fitxan hizkuntza aukeratu daiteke: hutsik, `Euskara`, `Gaztelera` edo `Ingelera`. Lehendik dauden moduluek hizkuntza zehaztu gabe hartzen dute, Hibernate-ren eskema-eguneratzean gehitutako zutabearen balio lehenetsiaren bidez.
- Erronkak ziklo, konfigurazioko maila aktibo, hizkuntza, izen, deskribapen eta data tarte batekin sortzen dira. Gutxienez modulu bat aukeratu behar da. Zikloa eta maila bat etorri behar dira; hizkuntza zehaztuta badago, hizkuntza bereko edo hizkuntza hutsik duten moduluak bakarrik onartzen dira. Hizkuntza-talde bakoitzak bere erronka du, izen bera erabiltzeko aukerarekin.
- Kiniela zikloaren curriculumari dagokio. Gaitasunetako lehendik dauden IE–lorpen-adierazle loturak erabiltzen ditu. IE batean klikatuta, errubrika osoa irekitzen da, gaitasun tekniko eta zeharkakoekin. `Onartu` botoiak hautatutako loturak gordetzen ditu; `Ezeztatu` edo Escape erabiliz gero ez da aldaketarik gordetzen.
- Adierazle bakoitzak IE bakoitzerako pisu independentea dauka (0–100, bi hamartar gehienez). Fokua kentzean edo Enter sakatzean pisua automatikoki gordetzen da; baturak berehala eguneratzen dira eta gordetzeko dagoen aldaketa adierazten da. IE eta moduluaren baturak ezin dira eskuz editatu. %100era iritsi ez diren zirriborroak gorde daitezke; batura kolorez adierazten da.
- Moduluak tolestu/zabaldu daitezke eta nabigatzaileak hautaketa gogoratzen du. Lotura kentzean haren pisua ere ezabatzen da, bai Kinielatik bai Gaitasunak pantailatik.

Datu berriak `ethazi_erronka`, `ethazi_erronka_moduloa` eta `ethazi_kiniela_pisua` tauletan gordetzen dira; `moduloa.hizkuntza` zutabea gehitzen da. Proiektuaren `ddl-auto=update` konfigurazioak eskema eguneratzen du aplikazioa abiatzean.

### Helbideak eta tokiko txantiloiak

`KinielaController` kontroladorearen oinarria `/ethazi/kiniela` da, eta `ErronkaController` kontroladorearena `/ethazi/erronkak`. Navbarra eta formularioak helbide horietara doaz. `/ethazi/kinielak` helbide zaharrak berrira birbideratzen du, hautatutako zikloa mantenduz.

STSn kanpotik sortutako fitxategiak gehitzean, proiektua freskatu (`Refresh` / F5) eta aplikazioa berrabiarazi. `Error resolving template` agertuz gero, egiaztatu txantiloiak `target/classes/templates/Ethazi/` karpetan daudela; `./mvnw resources:resources` komandoak `src/main/resources` baliabideak exekuzioaren classpath-era kopiatzen ditu. Kontroladoreek `@RequestMapping` oinarri bera edukitzeak ez du berez txantiloi-errore hori eragiten.

Hizkuntzaren Java konstantea `GAZTELERA` da. `HizkuntzaConverter` bihurgailuak lehendik gordetako `ERDERA` balioak ere irakurtzen ditu moduluetan eta erronketan; idazketa berriek `GAZTELERA` erabiltzen dute.
