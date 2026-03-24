# Koaderno Berria

## Dokumentazioa

Repo honetan lau dokumentu nagusi aurkituko dituzu:

- [Erabiltzaile gida](erabiltzaile_gida.md)
- [Kudeatzaile gida](kudeatzaile_gida.md)
- [Administratzaile gida](administratzaile_gida.md)

## Sarrera
**Koaderno Berria** Lanbide Heziketako irakasleen eguneroko jarduna kudeatzeko web aplikazioa da. Helburu nagusia irakasleen koadernoak, ikasleen jarraipena, egutegiak, programazioa, notak eta estatistikak tresna bakarrean biltzea, ikastetxeko rol ezberdinen arteko koordinazioa erraztuz.

Proiektua bereziki pentsatuta dago:
- irakasle bakoitzak bere koadernoak kudeatzeko
- kudeatzaileek ikastetxeko egitura akademikoa eta operatiboa administratzeko
- administratzaileek aplikazioaren konfigurazio orokorra eta sarbide sistema gobernatzeko

## Zer arazo konpontzen du

Koaderno Berriaren helburua ez da soilik “koaderno bat digitalizatzea”. Tresna honek ondoko beharrei erantzuten die:

- Ikasturteko programazioa sortzeko laguntza
- Programaziotik denboralizazioa (egunerokoa) sortu
- asistentzia eta eguneroko jardueren kontrola
- noten eta estatistiken kudeaketa
- irakasleen arteko koordinazioa (koadernoak partekatuz)
- kudeatzailearen bista globala

Horren bidez, gaur egun sakabanatuta egon ohi diren prozesuak —kalkulu orriak, koaderno pertsonalak, dokumentu solteak edo plataformen arteko datu bikoizketak— espazio bakarrean bildu nahi dira.

## Gaur egungo funtzionalitate nagusiak

Aplikazioaren egungo egiturak eta dokumentazio funtzionalak honako gaitasun hauek erakusten dituzte:

### Irakaslearentzat
- Koadernoak sortu eta kudeatu.
- Koaderno aktiboa aukeratu.
- Moduluko ordutegia konfiguratu.
- Ikasle zerrenda bistaratu eta hauek matrikula egoerak aldatu.
- Asistentzia eta eguneroko jarraipena egin.
- Programazioa eta denboralizazioa landu.
- Notak eta estatistikak bete eta kontsultatu.
- Koadernoa beste irakasle batzuekin partekatu.

### Kudeatzailearentzat
- Zikloak, taldeak, moduluak eta irakasleak kudeatu.
- Ikasturteak eta egutegiak konfiguratu.
- Ikasleak kontsultatu, editatu eta inportatu.
- Ikasleen kudeaketa: matrikula, talde... aldaketak
- Koadernoen ikuskaritza eta kontsulta egin.
- Estatistiken dashboard edo ikuspegi bateratuak kontsultatu.
- Ikastetxeko konfigurazio jakin batzuk kudeatu.

### Administratzailearentzat
- Aplikazioaren logoa kudeatu.
- Ebaluazioen koloreak konfiguratu.
- Autentikazio-aukerak aktibatu edo desaktibatu.
- Sistemako log edo erregistroak kontsultatu.

## Rolak

Koadernoan, funtzionalitateak rol bidez banatzen dira:

- **Irakaslea**: bere koadernoen eta ikasleen eguneroko lanaren ardura du.
- **Kudeatzailea**: ikastetxeko egitura akademikoa eta datu nagusiak antolatzen ditu.
- **Administratzailea**: konfigurazio tekniko eta orokorraren ardura du.

## Autentikazioa

Aplikazioak gaur egun bi autentikazio mota onartzen ditu, konfigurazioaren arabera:

- **Google bidezko saio-hasiera**
- **LDAP bidezko saio-hasiera**

Instalazio bakoitzean biak edo horietako bat erabil daitezke. Sistemak gutxienez autentikazio mota bat aktibo izatea eskatzen du.

## Teknologia oinarria

Proiektua honako teknologia hauen gainean eraikita dago:

- Java 17
- Spring Boot
- Spring Security
- Thymeleaf
- Spring Data JPA
- MySQL
- Docker Compose + Traefik (deployment ingurunean)

## Proiektuaren norabidea

Koaderno Berriaren lehen faseak irakasleen ohiko jardunerako oinarria eskaintzen du. Hurrengo hedapenetan, batez ere honako ildo hauek hartu dira kontuan:

- ebaluazio-sistemaren integrazioa
- Ethazi edo koaderno intermodularrak
- falten eta kalifikazioen sinkronizazioa
- tutore txostenak
- ordezkoen kudeaketa
- datuen ofizialtasuna

## Instalazioari buruzko ohar azkarra

Garapen edo instalazio mailan, aplikazioak MySQL datu-basea, ingurune-aldagaiak eta autentikazio konfigurazioa behar ditu. Docker bidezko hedapenean Traefik erabiltzen da reverse proxy gisa, eta aplikazioa lehenespenez **8091** portuan exekutatzen da.

## Lizentzia

Koaderno Berria / Zornotza LHII - Mikel Urkiaga **CC BY-NC-SA 4.0** lizentziarekin lotuta dago repoan adierazitakoaren arabera.

---

Dokumentu hau aurkezpen orokor gisa pentsatuta dago. Erabilera operatiborako, jo rol bakoitzari dagokion gidara.
