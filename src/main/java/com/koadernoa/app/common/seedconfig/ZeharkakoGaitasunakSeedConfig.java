package com.koadernoa.app.common.seedconfig;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

import com.koadernoa.app.ethazi.entitateak.gaitasunak.GaitasunMaila;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.GaitasunMota;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.Gaitasuna;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.LorpenAdierazlea;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.MailakatzeEredua;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.MailakatzeMaila;
import com.koadernoa.app.ethazi.repository.GaitasunaRepository;
import com.koadernoa.app.ethazi.repository.MailakatzeEreduaRepository;

@Configuration
public class ZeharkakoGaitasunakSeedConfig {

    private static final String[] MAILA_IZENAK = {
        "1. maila", "2. maila", "3. maila", "4. maila"
    };

    @Bean
    CommandLineRunner zeharkakoGaitasunakSeed(
            GaitasunaRepository gaitasunaRepository,
            MailakatzeEreduaRepository mailakatzeEreduaRepository,
            TransactionTemplate transactionTemplate) {

        return args -> transactionTemplate.executeWithoutResult(status ->
            seed(gaitasunaRepository, mailakatzeEreduaRepository));
    }

    void seed(
            GaitasunaRepository gaitasunaRepository,
            MailakatzeEreduaRepository mailakatzeEreduaRepository) {

        Map<String, Gaitasuna> existitzenDirenak = gaitasunaRepository
                .findByZikloaIsNullAndMotaOrderByKodeaAsc(GaitasunMota.ZEHARKAKOA)
                .stream()
                .filter(g -> g.getKodea() != null)
                .collect(Collectors.toMap(Gaitasuna::getKodea, Function.identity(), (a, b) -> a));

        List<GaitasunSeed> gaitasunak = List.of(
            new GaitasunSeed(
                "ZG01",
                "GAITASUN PERTSONALAK",
                "Pertsonen arteko harremanetan duen portaera eraginkortasunez kudeatzen du",
                new String[] {
                    "Pertsonen arteko harremanekin eta gizarte-portaerarekin lotutako oinarrizko kontzeptuak ezagutzen ditu.",
                    "Pertsonen arteko harremanetan portaera ondo kudeatzearen garrantzia azaltzen du, eta bere inplikazioak ulertzen ditu.",
                    "Kudeaketa emozionaleko eta komunikazio eraginkorreko estrategiak aplikatzen ditu pertsonen arteko harremanetan.",
                    "Bere interakzio sozialei buruz hausnartzen du, bere portaera ebaluatzen du eta bere harremanak sendotzeko hobekuntzak proposatzen ditu."
                },
                new String[][] {
                    {
                        "Eguneroko egoeretan norberaren eta besteen emozioak identifikatzen ditu.",
                        "Gizarte-elkarreraginean portaera egokiak eta desegokiak ezagutzen eta bereizten ditu.",
                        "Pertsonen arteko komunikaziorako oinarrizko estrategiak zerrendatzen ditu."
                    },
                    {
                        "Bere emozioek besteekiko portaeran nola eragiten duten interpretatzen du.",
                        "Eraginkortasunik edo errespeturik gabeko komunikazioaren ondorioak azaltzen ditu.",
                        "Portaerak eta emaitza positiboak edo negatiboak lotzen ditu bizikidetzan."
                    },
                    {
                        "Gatazka edo presio sozialeko egoeretan emozioak erregulatzen ditu.",
                        "Askotariko testuinguruetan komunikazio asertiboko teknikak erabiltzen ditu.",
                        "Desadostasunak errespetuz eta elkarlanean ebazten ditu."
                    },
                    {
                        "Pertsonen arteko egoeretan egiten dituen jarduerak kritikoki aztertzen ditu.",
                        "Inguruan bizikidetza eta komunikazioa hobetzeko ekintzak proposatzen ditu.",
                        "Errespetu-, inklusio- eta lankidetza-giroa aktiboki sustatzen du."
                    }
                },
                "COMPETENCIAS PERSONALES",
                "Gestiona eficazmente el comportamiento en las relaciones interpersonales",
                new String[] {
                    "Reconoce conceptos básicos relacionados con las relaciones interpersonales y el comportamiento social.",
                    "Explica la importancia de una buena gestión del comportamiento en las relaciones interpersonales y comprende sus implicaciones.",
                    "Aplica estrategias de gestión emocional y comunicación efectiva en sus relaciones interpersonales.",
                    "Reflexiona sobre sus interacciones sociales, evalúa su comportamiento y propone mejoras para fortalecer sus relaciones."
                },
                new String[][] {
                    {
                        "Identifica emociones propias y ajenas en situaciones cotidianas.",
                        "Reconoce comportamientos adecuados e inadecuados en la interacción social.",
                        "Enumera estrategias básicas de comunicación interpersonal."
                    },
                    {
                        "Interpreta cómo sus emociones influyen en su comportamiento con los demás.",
                        "Explica las consecuencias de una comunicación ineficaz o irrespetuosa.",
                        "Relaciona comportamientos con resultados positivos o negativos en la convivencia."
                    },
                    {
                        "Regula sus emociones en situaciones de conflicto o presión social.",
                        "Utiliza técnicas de comunicación asertiva en contextos diversos.",
                        "Resuelve desacuerdos de forma respetuosa y colaborativa."
                    },
                    {
                        "Analiza críticamente sus actuaciones en situaciones interpersonales.",
                        "Propone acciones para mejorar la convivencia y la comunicación en su entorno.",
                        "Promueve activamente un clima de respeto, inclusión y colaboración."
                    }
                }
            ),
            new GaitasunSeed(
                "ZG02",
                "KOMUNIKAZIO GAITASUNAK",
                "Modu eraginkorrean komunikatzen da ahoz, idatziz eta hitzik gabe",
                new String[] {
                    "Komunikazioaren oinarrizko elementuak identifikatzen ditu.",
                    "Komunikazio eraginkorraren printzipioak azaltzen ditu.",
                    "Komunikazio eraginkorreko estrategiak erabiltzen ditu.",
                    "Komunikazioaren eraginkortasuna ebaluatzen du eta hobekuntzak proposatzen ditu."
                },
                new String[][] {
                    {
                        "Hainbat komunikazio-modalitate identifikatzen ditu.",
                        "Hainbat testuingurutan komunikazio eraginkorra izateko funtsezko elementuak zerrendatzen ditu.",
                        "Komunikatzeko hainbat mota eta estilo identifikatzen ditu, beste pertsona batzuekiko interakzioari dagokionez."
                    },
                    {
                        "Komunikazio eraginkor baten ezaugarriak deskribatzen ditu, hainbat modalitate eta testuingurutan.",
                        "Komunikazio eraginkorraren funtsezko elementuak lotzen ditu komunikazio-modalitate desberdinekin eta horien testuinguruekin.",
                        "Komunikazioak pertsonen arteko harremanetan duen eragina deskribatzen du."
                    },
                    {
                        "Ahozko eta idatzizko mezua modu argi eta antolatuan egituratzen du, testuinguruari egokitutako hizkuntza erabiliz.",
                        "Bere mezua argudiatzen du eta modu eraginkorrean erantzuten die galderei eta iruzkinei.",
                        "Bere komunikazio-estiloa hainbat audientzia-motatara eta egoeratara egokitzen du, eta testuinguruaren malgutasuna eta kontzientzia erakusten du."
                    },
                    {
                        "Hitzezko eta hitzik gabeko estrategien erabilera hobetzeko indarguneak eta arloak identifikatzen ditu.",
                        "Komunikazioaren funtsezko elementuek mezuaren eraginkortasunean duten eragina aztertzen du.",
                        "Hainbat entzule eta testuingururen aurrean komunikazio sinesgarria lortzeko estrategiak proposatzen ditu."
                    }
                },
                "COMPETENCIAS COMUNICATIVAS",
                "Comunica eficazmente de forma oral, escrita y no verbal",
                new String[] {
                    "Identifica los elementos básicos de la comunicación.",
                    "Explica los principios de la comunicación efectiva.",
                    "Utiliza estrategias de comunicación efectiva.",
                    "Evalúa la eficacia de la comunicación y propone mejoras."
                },
                new String[][] {
                    {
                        "Identifica diferentes modalidades(1) de comunicación.",
                        "Enumera elementos clave(2) para una comunicación efectiva en diferentes contextos.",
                        "Identifica diferentes tipos(3) y estilos(4) de comunicación con relación a la interacción con otras personas."
                    },
                    {
                        "Describe las características(5) de una comunicación efectiva en sus diferentes modalidades y contextos.",
                        "Relaciona elementos clave(2) de una comunicación efectiva con las diferentes modalidades(1) de comunicación y sus contextos.",
                        "Describe el impacto de la comunicación en las relaciones interpersonales."
                    },
                    {
                        "Estructura su mensaje oral y escrito de forma clara y organizada utilizando un lenguaje apropiado al contexto.",
                        "Argumenta su mensaje y responde de manera efectiva a preguntas y comentarios.",
                        "Adapta su estilo de comunicación a distintos tipos de audiencia y situaciones mostrando flexibilidad y conciencia del contexto."
                    },
                    {
                        "Identifica fortalezas y áreas de mejora en el uso de estrategias verbales y no verbales.",
                        "Analiza el impacto de los elementos clave de la comunicación en la efectividad del mensaje.",
                        "Propone estrategias para lograr una comunicación convincente ante diferentes audiencias y contextos."
                    }
                }
            ),
            new GaitasunSeed(
                "ZG03",
                "ELKARLANERAKO GAITASUNAK",
                "Aktiboki parte hartzen du helburu partekatuak lortzen",
                new String[] {
                    "Lanbide-inguruneetan eta ikaskuntza-inguruneetan talde-lanak duen garrantzia ezagutzen du.",
                    "Elkarlanaren funtsezko printzipioak azaltzen ditu.",
                    "Taldean modu eraginkorrean lan egiten du.",
                    "Hainbat testuingurutako talde-lanaren eraginkortasuna aztertzen du, eta hobekuntzak proposatzen ditu taldearen lankidetza eta errendimendua optimizatzeko."
                },
                new String[][] {
                    {
                        "Lanbide-inguruneetan lankidetza funtsezkoa den egoerak aipatzen ditu.",
                        "Talde-lanaren funtsezko kontzeptuen ezaugarriak azaltzen ditu: parekoak ezagutzea, komunikazio irekia eta ideiak partekatzearen garrantzia.",
                        "Talde-lanak lan-giroa eta emaitzak nola hobetzen dituen justifikatzen du, eta helburu komunak lortzen laguntzen du."
                    },
                    {
                        "Lankidetza eraginkorrerako enpatia, entzute aktiboa eta trebetasunen aniztasuna defendatzen ditu.",
                        "Talde-dinamikak interpretatzen ditu, eta ulertzen eta azaltzen du banakako desberdintasunek nola laguntzen duten taldearen arrakastan.",
                        "Gatazkak modu eraikitzailean eraldatzeko estrategiak deskribatzen ditu."
                    },
                    {
                        "Bere kideekiko harremanak egokitzen ditu askotariko egoeretan, eta, horretarako, informazio pertsonala maneiatzen eta aberasten du.",
                        "Hainbat rol dituzten talde-erronketan parte hartzen du, besteen iritziak errespetatuz eta erabakiak hartzen lagunduz. Eta hainbat rol hartzen ditu bere gain taldearen barruan.",
                        "Elkarlaneko ingurune batean problemak ebazteko trebetasunak aplikatzen ditu, taldearen erronkak identifikatuz eta gaindituz."
                    },
                    {
                        "Proiektu espezifikoetako lankidetzaren eraginkortasuna aztertzen eta ebaluatzen du, indarguneak eta hobetzeko arloak identifikatuta.",
                        "Lankidetza hobetzeko estrategiak proposatzen ditu.",
                        "Lankidetza-giro positiboa sustatzen du, eta besteak motibatzen ditu."
                    }
                },
                "COMPETENCIAS COLABORATIVAS",
                "Participa activamente en el logro de objetivos compartidos",
                new String[] {
                    "Reconoce la importancia del trabajo en equipo en entornos profesionales y de aprendizaje.",
                    "Explica los principios clave de la colaboración.",
                    "Trabaja en equipo de manera efectiva.",
                    "Analiza la efectividad del trabajo en equipo en distintos contextos y propone mejoras para optimizar la cooperación y el rendimiento del equipo."
                },
                new String[][] {
                    {
                        "Nombra situaciones en las que la colaboración es esencial en entornos profesionales.",
                        "Expone las características de los conceptos fundamentales del trabajo en equipo: conocer a los pares, la comunicación abierta y la importancia de compartir ideas.",
                        "Justifica cómo el trabajo en equipo mejora el ambiente laboral y los resultados contribuyendo al logro de objetivos comunes."
                    },
                    {
                        "Defiende la empatía, la escucha activa y la diversidad de habilidades para la colaboración efectiva.",
                        "Interpreta las dinámicas de equipos y comprende y explica cómo las diferencias individuales contribuyen al éxito del equipo.",
                        "Describe estrategias(2) para transformar conflictos de manera constructiva."
                    },
                    {
                        "Adapta las relaciones con sus pares en situaciones diversas manejando y enriqueciendo información personal.",
                        "Participa en retos de equipo con diferentes roles, respetando las opiniones de los demás y contribuyendo en la toma de decisiones. Y asume distintos roles(1) dentro del equipo.",
                        "Aplica habilidades(3) de resolución de problemas en un entorno colaborativo, identificando y superando desafíos del equipo."
                    },
                    {
                        "Analiza y evalúa la efectividad de la colaboración en proyectos específicos, identificando fortalezas y áreas de mejora.",
                        "Propone estrategias para mejorar la colaboración.",
                        "Promueve un ambiente de colaboración positivo y motiva a los demás."
                    }
                }
            ),
            new GaitasunSeed(
                "ZG04",
                "GAITASUN DIGITALAK",
                "Ingurune digitaletan modu kritiko, sortzaile, seguru eta arduratsuan moldatzen da",
                new String[] {
                    "Ingurune digitaletan gailuak eta teknologiak modu seguruan eta eraginkorrean erabiltzeko oinarrizko gaitasun digitalak aplikatzeko oinarriak ezartzen ditu.",
                    "Bere ezagutzak eta trebetasunak aktiboki aplikatzen ditu ingurune digitaletako egoera praktiko eta kolaboratiboetan.",
                    "Modu kontzientean txertatzen ditu estrategiak informazioaren kudeaketan, edukien sorkuntzan eta identitate digital positibo baten zaintzan.",
                    "Erronka digitalak modu autonomoan ebazten ditu, proaktiboa eta kontzientzia digital osoarekin."
                },
                new String[][] {
                    {
                        "4.1 Erabiltzen dituen gailu digitalak babesten ditu.",
                        "2.1 Teknologia digital egokiekin elkarreragiten du.",
                        "2.5 Ingurune digitaletan eta ingurune horien interakzioan portaera-arau egokiak erabiltzen ditu.",
                        "4.2 Bere datu pertsonalak eta ikaskideenak ingurune digitaletan babesten ditu.",
                        "5.4 Konpetentzia digitalaren arloan dituen beharrak identifikatzen ditu.",
                        "1.2 Iturriaren eta lortutako informazioaren baliozkotasuna ebaluatzen du."
                    },
                    {
                        "2.4 Teknologia digital egokiekin kolaboratzen du.",
                        "5.3 Tresna digitalak erabiltzen ditu ezagutza sortu eta partekatzeko.",
                        "2.2 Eduki digitalak egoki partekatzen ditu.",
                        "1.3 Informazioa biltegiratzeko eta berreskuratzeko estrategiak antolatzen ditu.",
                        "3.1 Edukiak sortzen ditu bitarteko digital egokiekin.",
                        "3.4 Aplikazioak eta gailuak segurtasunez eta behar bezala erabiltzeko konfiguratzen ditu."
                    },
                    {
                        "1.1 Bilaketa pertsonalerako estrategiak antolatzen ditu.",
                        "3.3 Eduki digitalak erabiltzen ditu, erabilera-lizentziak kontuan hartuta.",
                        "3.2 Edukiak aldatzen eta integratzen ditu, hobetzeko eta berriak sortzeko.",
                        "2.6 Bere online ospea babesteko estrategiak erabiltzen ditu."
                    },
                    {
                        "5.1 Arazo teknikoak ebazten ditu.",
                        "2.3 Gizartean parte hartzen du teknologia digital egokien bidez.",
                        "5.2 Tresna digitalak hautatzen ditu, beharren arabera.",
                        "4.3 Teknologia digitala erabiltzean arrisku fisikoak eta mentalak prebenitzen ditu.",
                        "4.4. Teknologia digitalaren ingurumen-inpaktua murrizteko neurriak hartzen ditu."
                    }
                },
                "COMPETENCIAS DIGITALES",
                "Se desenvuelve en entornos digitales de manera crítica, creativa, segura y responsable",
                new String[] {
                    "SIENTA las bases para aplicar competencias digitales básicas para usar dispositivos y tecnologías en entornos digitales de forma segura y efectiva.",
                    "APLICA activamente sus conocimientos y habilidades en situaciones prácticas y colaborativas en entornos digitales.",
                    "INTEGRA de manera consciente estrategias en la gestión de información, la creación de contenidos y la preservación de una identidad digital positiva.",
                    "RESUELVE de manera autónoma desafíos digitales, siendo proactivo y con plena conciencia digital."
                },
                new String[][] {
                    {
                        "4.1 Protege los dispositivos digitales que utiliza.",
                        "2.1 Interactúa con las tecnologías digitales apropiadas.",
                        "2.5 Emplea normas de comportamiento adecuadas en los diferentes entornos digitales y en la interacción de estos.",
                        "4.2 Protege tanto sus datos personales como los de sus compañeros/as en los entornos digitales.",
                        "5.4 Identifica sus necesidades en materia de competencia digital.",
                        "1.2 Evalúa la validez tanto de la fuente como de la información obtenida."
                    },
                    {
                        "2.4 Colabora con tecnologías digitales apropiadas.",
                        "5.3 Utiliza herramientas digitales para crear y compartir conocimiento.",
                        "2.2 Comparte contenidos digitales de manera apropiada.",
                        "1.3 Organiza estrategias de almacenamiento y recuperación de la información.",
                        "3.1 Crea contenidos con los medios digitales adecuados.",
                        "3.4 Configura aplicaciones y dispositivos de manera segura y para su correcto uso."
                    },
                    {
                        "1.1 Organiza estrategias de búsqueda personales.",
                        "3.3 Usa contenidos digitales teniendo en cuenta sus licencias de uso.",
                        "3.2 Modifica e integra contenidos para mejorar los mismos y crear nuevos.",
                        "2.6 Utiliza estrategias para proteger su reputación en línea."
                    },
                    {
                        "5.1 Resuelve problemas técnicos.",
                        "2.3 Participa en la sociedad a través de tecnologías digitales adecuadas.",
                        "5.2 Selecciona herramientas digitales en función de sus necesidades.",
                        "4.3 Previene los peligros tanto físicos como mentales en el uso de la tecnología digital.",
                        "4.4 Toma medidas para la reducción del impacto medioambiental de la tecnología digital."
                    }
                }
            ),
            new GaitasunSeed(
                "ZG05",
                "GAITASUN EKINTZAILEAK",
                "Aukerak eta Ideak balio sozial, kultural edo ekonomiko bihurtzen ditu",
                new String[] {
                    "Balioa eman dezaketen aukerak identifikatzen ditu eta horiei lotutako arriskuak aurreikusten ditu.",
                    "Ideien, beharren eta konponbide posibleen arteko loturak ezartzen ditu.",
                    "Ezagutzak eta trebetasunak aplikatuz, ideiak autonomiaz garatzen ditu.",
                    "Ekintzak eta haien inpaktua ebaluatzen ditu, hobekuntza berritzaileak proposatzeko."
                },
                new String[][] {
                    {
                        "Bere indarguneak eta ahuleziak ezagutzen ditu, eta bera jarduteko gaitasunean sinesten hasten da.",
                        "Testuinguruaren beharrak eta erronkak identifikatzen ditu.",
                        "Bere ekintzen ondorioak eta balizko arriskuak identifikatzen ditu."
                    },
                    {
                        "Modu autonomoan jarduteko eta erronka txikiak bere gain hartzeko prestutasuna erakusten du.",
                        "Identifikatutako behar eta erronken gainean, balioa sortzeko ideiak proposatzen ditu.",
                        "Bere ideiak gauzatzeko, helburuak eta baliabideak eraginkortasunez erlazionatzen ditu.",
                        "Ideia bat ekintza bihurtzeko prozesua planifikatzen du eta bere planak egokitzen ditu aldaketen aurrean."
                    },
                    {
                        "Zailtasunen aurrean ahaleginari eusten dio eta bere helburuekin konprometitzen da.",
                        "Helburu argiak zehazten ditu eta ideia garatzeko beharrezkoak diren baliabideak eraginkortasunez mobilizatzen ditu.",
                        "Erabaki arrazoituak hartzen ditu, arriskuak eta erantzukizunak hartzen ditu.",
                        "Irtenbideak garatzen ditu, ezagutzak eta baliabideak modu originalean konbinatuz."
                    },
                    {
                        "Beste pertsona batzuk inspiratzen eta motibatzen ditu.",
                        "Lortutako emaitzen inguruan hausnartzen eta arrakastetatik zein akatsetatik ikasten du.",
                        "Bizipenen edo gauzatutako ekintzen ebaluazioan oinarritutako hobekuntzak edo doikuntzak proposatzen ditu."
                    }
                },
                "COMPETENCIAS EMPRENDEDORAS",
                "Transforma oportunidades e ideas en valores sociales, culturales o económicos",
                new String[] {
                    "Identifica oportunidades que pueden aportar valor y prevé los riesgos asociados.",
                    "Establece relaciones entre ideas, necesidades y posibles soluciones.",
                    "Desarrolla ideas con autonomía, aplicando conocimientos y habilidades.",
                    "Evalúa acciones y su impacto para proponer mejoras innovadoras."
                },
                new String[][] {
                    {
                        "Reconoce sus fortalezas y debilidades y empieza a creer en su capacidad de actuar.",
                        "Identifica las necesidades y retos del contexto.",
                        "Identifica las consecuencias de sus acciones y los posibles riesgos."
                    },
                    {
                        "Muestra disposición a actuar de forma autónoma y asumir pequeños retos.",
                        "Propone ideas para generar valor sobre las necesidades y retos identificados.",
                        "Relaciona eficazmente objetivos y recursos para llevar a cabo sus ideas.",
                        "Planifica el proceso de convertir una idea en acción y adapta sus planes a los cambios."
                    },
                    {
                        "Ante las dificultades mantiene el esfuerzo y se compromete con sus objetivos.",
                        "Define objetivos claros y moviliza eficazmente los recursos necesarios para el desarrollo de la idea.",
                        "Toma decisiones de manera razonada, asume riesgos y responsabilidades.",
                        "Desarrolla soluciones combinando conocimientos y recursos de forma original."
                    },
                    {
                        "Inspira y es fuente de motivación para otras personas.",
                        "Reflexiona sobre los resultados obtenidos y aprende tanto de los éxitos como de los errores.",
                        "Propone mejoras o ajustes basados en la evaluación de las vivencias y acciones realizadas."
                    }
                }
            ),
            new GaitasunSeed(
                "ZG06",
                "IRAUNKORTASUN GAITASUNAK",
                "Giza garapen jasangarrian laguntzen duten portaera- eta jarduera-ohiturak aplikatzen ditu",
                new String[] {
                    "Jasangarritasunaren funtsezko kontzeptuak ezagutzen ditu.",
                    "Jasangarritasunak lan- eta gizarte-inguruneetan duen garrantzia ulertzen du.",
                    "Irizpide iraunkorrak aplikatzen ditu bere lan- eta gizarte-ingurunean, eta praktika horiek hainbat testuingurutan integratzea sustatzen du.",
                    "Praktika jasangarriak ebaluatzen ditu, eta hobekuntza berritzaileak proposatzen ditu."
                },
                new String[][] {
                    {
                        "Iraunkortasunari buruzko oinarrizko kontzeptuak identifikatzen ditu ingurune hurbilean (etxea, eskola, komunitatea).",
                        "Iraunkortasuna aplikatzeko egoerak deskribatzen ditu.",
                        "Kontsumo arduratsuaren eta energia-eraginkortasunaren onurak defendatzen ditu."
                    },
                    {
                        "Ohitura jasangarriek nola laguntzen duten norbanakoaren eta taldearen ongizatean azaltzen du.",
                        "Jokabide jasangarrien adibideak eta hainbat eremutan dituen onura espezifikoak erlazionatzen ditu.",
                        "Jasangarriak ez diren egoeren eta ekintzen ondorioak justifikatzen ditu."
                    },
                    {
                        "Bere ingurune pertsonal, hezitzaile eta profesionalean praktika iraunkorrak egiten ditu.",
                        "Aktiboki parte hartzen du ekintza jasangarrien erabakietan.",
                        "Etorkizun justu eta iraunkorra lortzeko ekintza kontsekuenteak hartzen ditu."
                    },
                    {
                        "Praktika jasangarrien eraginkortasuna eta inpaktua aztertzen ditu.",
                        "Iraunkortasuna sustatzeko hobekuntzak eta estrategia berriak proposatzen ditu.",
                        "Iraunkortasuneko proiektu berriak diseinatzen ditu."
                    }
                },
                "COMPETENCIAS DE SOSTENIBILIDAD",
                "Aplica hábitos de comportamiento y actuación que contribuyen al Desarrollo Humano Sostenible",
                new String[] {
                    "Conoce los conceptos clave de sostenibilidad.",
                    "Comprende la importancia de la sostenibilidad en entornos laborales y sociales.",
                    "Aplica criterios sostenibles en su entorno laboral y social, promoviendo la integración de estas prácticas en diferentes contextos.",
                    "Evalúa las prácticas sostenibles proponiendo mejoras innovadoras."
                },
                new String[][] {
                    {
                        "Identifica conceptos básicos de sostenibilidad en su entorno cercano (hogar, escuela, comunidad).",
                        "Describe situaciones en las que aplicar la sostenibilidad.",
                        "Defiende los beneficios del consumo responsable y la eficiencia energética."
                    },
                    {
                        "Explica cómo los hábitos sostenibles contribuyen al bienestar individual y colectivo.",
                        "Relaciona ejemplos de comportamientos sostenibles con sus beneficios específicos en diferentes ámbitos.",
                        "Justifica las consecuencias de situaciones y acciones no sostenibles."
                    },
                    {
                        "Realiza prácticas sostenibles en su entorno personal, educativo y profesional.",
                        "Participa activamente en las decisiones de acciones sostenibles.",
                        "Adopta acciones consecuentes para lograr un futuro justo y sostenible."
                    },
                    {
                        "Analiza la efectividad e impacto de prácticas sostenibles.",
                        "Propone mejoras y nuevas estrategias para fomentar la sostenibilidad.",
                        "Diseña proyectos nuevos de sostenibilidad."
                    }
                }
            )
        );

        for (GaitasunSeed seed : gaitasunak) {
            Gaitasuna dagoena = existitzenDirenak.get(seed.kodea());
            if (dagoena == null) {
                sortuGaitasuna(seed, gaitasunaRepository, mailakatzeEreduaRepository);
            } else {
                eguneratuItzulpenak(dagoena, seed, gaitasunaRepository);
            }
        }
    }

    /** Lehengo seed-ak sortutako EU erregistroak osatzen ditu; ez ditu ES testu editatuak ordezten. */
    private void eguneratuItzulpenak(Gaitasuna gaitasuna, GaitasunSeed seed,
                                     GaitasunaRepository gaitasunaRepository) {
        if (hutsik(gaitasuna.getDeskribapenaEs())) {
            gaitasuna.setDeskribapenaEs(seed.deskribapenaEs());
        }
        MailakatzeEredua eredua = gaitasuna.getEredua();
        if (eredua == null) {
            throw new IllegalStateException("Mailakatze-eredurik gabeko gaitasuna: " + seed.kodea());
        }
        if (hutsik(eredua.getIzenaEs())) {
            eredua.setIzenaEs(seed.izenaEs() + " - 4 niveles");
        }
        for (MailakatzeMaila maila : eredua.getMailak()) {
            int i = maila.getOrdena() - 1;
            if (i < 0 || i >= 4) {
                throw new IllegalStateException("Maila-ordena ezezaguna: " + seed.kodea());
            }
            if (hutsik(maila.getIzenaEs())) {
                maila.setIzenaEs((i + 1) + ". nivel");
            }
        }
        for (GaitasunMaila gaitasunMaila : gaitasuna.getMailak()) {
            int i = gaitasunMaila.getMaila().getOrdena() - 1;
            if (i < 0 || i >= 4) {
                throw new IllegalStateException("Maila-ordena ezezaguna: " + seed.kodea());
            }
            if (hutsik(gaitasunMaila.getDeskribapenaEs())) {
                gaitasunMaila.setDeskribapenaEs(seed.mailaDeskribapenakEs()[i]);
            }
            for (LorpenAdierazlea adierazlea : gaitasunMaila.getLorpenAdierazleak()) {
                int j = adierazlea.getOrdena() - 1;
                if (j >= 0 && j < seed.lorpenAdierazleakEs()[i].length
                        && hutsik(adierazlea.getDeskribapenaEs())) {
                    adierazlea.setDeskribapenaEs(seed.lorpenAdierazleakEs()[i][j]);
                }
            }
        }
        gaitasunaRepository.save(gaitasuna);
    }

    private boolean hutsik(String testua) {
        return testua == null || testua.isBlank();
    }

    private void sortuGaitasuna(
            GaitasunSeed seed,
            GaitasunaRepository gaitasunaRepository,
            MailakatzeEreduaRepository mailakatzeEreduaRepository) {

        MailakatzeEredua eredua = new MailakatzeEredua();
        eredua.setZikloa(null);
        eredua.setMota(GaitasunMota.ZEHARKAKOA);
        eredua.setIzena(seed.izena() + " - 4 maila");
        eredua.setIzenaEs(seed.izenaEs() + " - 4 niveles");

        for (int i = 0; i < 4; i++) {
            MailakatzeMaila maila = new MailakatzeMaila();
            maila.setEredua(eredua);
            maila.setOrdena(i + 1);
            maila.setIzena(MAILA_IZENAK[i]);
            maila.setIzenaEs((i + 1) + ". nivel");
            eredua.getMailak().add(maila);
        }

        mailakatzeEreduaRepository.save(eredua);

        Gaitasuna gaitasuna = new Gaitasuna();
        gaitasuna.setZikloa(null);
        gaitasuna.setEredua(eredua);
        gaitasuna.setMota(GaitasunMota.ZEHARKAKOA);
        gaitasuna.setKodea(seed.kodea());
        gaitasuna.setLegacyIzena(seed.izena());
        gaitasuna.setDeskribapena(seed.deskribapena());
        gaitasuna.setDeskribapenaEs(seed.deskribapenaEs());

        for (int i = 0; i < 4; i++) {
            GaitasunMaila gaitasunMaila = new GaitasunMaila();
            gaitasunMaila.setGaitasuna(gaitasuna);
            gaitasunMaila.setMaila(eredua.getMailak().get(i));
            gaitasunMaila.setDeskribapena(seed.mailaDeskribapenak()[i]);
            gaitasunMaila.setDeskribapenaEs(seed.mailaDeskribapenakEs()[i]);

            String[] adierazleak = seed.lorpenAdierazleak()[i];
            for (int j = 0; j < adierazleak.length; j++) {
                LorpenAdierazlea adierazlea = new LorpenAdierazlea();
                adierazlea.setGaitasunMaila(gaitasunMaila);
                adierazlea.setOrdena(j + 1);
                adierazlea.setDeskribapena(adierazleak[j]);
                adierazlea.setDeskribapenaEs(seed.lorpenAdierazleakEs()[i][j]);
                gaitasunMaila.getLorpenAdierazleak().add(adierazlea);
            }

            gaitasuna.getMailak().add(gaitasunMaila);
        }

        gaitasunaRepository.save(gaitasuna);
    }

    private record GaitasunSeed(
            String kodea,
            String izena,
            String deskribapena,
            String[] mailaDeskribapenak,
            String[][] lorpenAdierazleak,
            String izenaEs,
            String deskribapenaEs,
            String[] mailaDeskribapenakEs,
            String[][] lorpenAdierazleakEs) {

        GaitasunSeed {
            if (mailaDeskribapenak.length != 4 || lorpenAdierazleak.length != 4
                    || mailaDeskribapenakEs.length != 4 || lorpenAdierazleakEs.length != 4) {
                throw new IllegalArgumentException(
                    "Zeharkako gaitasun guztiek 4 maila izan behar dituzte: " + kodea
                );
            }
            for (int i = 0; i < 4; i++) {
                if (lorpenAdierazleak[i].length != lorpenAdierazleakEs[i].length) {
                    throw new IllegalArgumentException(
                        "EU/ES lorpen-adierazleen kopuru desberdina: " + kodea + ", maila " + (i + 1)
                    );
                }
            }
        }
    }
}
