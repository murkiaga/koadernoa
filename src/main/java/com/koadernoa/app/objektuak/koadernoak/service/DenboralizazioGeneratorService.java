package com.koadernoa.app.objektuak.koadernoak.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunBerezi;
import com.koadernoa.app.objektuak.egutegia.entitateak.EgunMota;
import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Ebaluaketa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Jarduera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoOrdutegiBlokea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Programazioa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.UnitateDidaktikoa;
import com.koadernoa.app.objektuak.koadernoak.repository.JardueraRepository;

import lombok.RequiredArgsConstructor;


/**
* Denboralizaziora bolkatzeko logika.
* Programazioan definitutako UD + Azpijarduerak -> Jarduera (datarekin) sorkuntza,
* Koadernoaren egutegia eta ordutegia errespetatuz.
*/
@Service
@RequiredArgsConstructor
public class DenboralizazioGeneratorService {

	private final JardueraRepository jardueraRepository;
	
	/** Aurreikuspena egiteko DTO sinplea */
	public record PreviewItem(LocalDate data, String titulua, float orduak, String mota, String azalpena) {}
	
	/**
	* Oinarrizko API:
	* - preview=true -> ez du DB-an idazten; sortuko liratekeen jarduerak itzultzen ditu.
	* - replaceExisting=true -> existitzen diren PLANIFIKATUA motako jarduerak ezabatzen ditu (koaderno + tartean).
	*/
	
	@Transactional
	public List<PreviewItem> generateFromProgramazioa(
	        Koadernoa koadernoa,
	        Programazioa programazioa,
	        boolean preview,
	        boolean replaceExisting)
	{
	    Objects.requireNonNull(koadernoa, "koadernoa");
	    Objects.requireNonNull(programazioa, "programazioa");
	    Egutegia egutegia = Objects.requireNonNull(koadernoa.getEgutegia(), "Koadernoak ez du Egutegirik");

	    // 1) Eraiki egutegi-ordutegi slotak
	    List<SessionSlot> slots = buildSessionSlots(koadernoa);
	    if (slots.isEmpty()) return List.of();

	    LocalDate globalFirst = slots.get(0).date;
	    LocalDate globalLast  = slots.get(slots.size() - 1).date;

	    // === EB BAKARRERAKO KASUA: tartea mugatu EB horren datetara ===
	    LocalDate tmpFrom = globalFirst;
	    LocalDate tmpTo   = globalLast;

	    if (programazioa.getEbaluaketak() != null
	            && programazioa.getEbaluaketak().size() == 1) {

	        Ebaluaketa eb = programazioa.getEbaluaketak().get(0);
	        if (eb.getHasieraData() != null && eb.getBukaeraData() != null) {
	            LocalDate ebFrom = eb.getHasieraData();
	            LocalDate ebTo   = eb.getBukaeraData();
	            if (ebFrom.isAfter(ebTo)) {
	                LocalDate swap = ebFrom;
	                ebFrom = ebTo;
	                ebTo   = swap;
	            }
	            // Ikasturtearekin intersekzioa
	            tmpFrom = ebFrom.isAfter(globalFirst) ? ebFrom : globalFirst;
	            tmpTo   = ebTo.isBefore(globalLast)   ? ebTo   : globalLast;
	        }
	    }

	    // Orain bai: lambda-rako aldagaiak FINAL
	    final LocalDate rangeFrom = tmpFrom;
	    final LocalDate rangeTo   = tmpTo;

	    List<SessionSlot> effectiveSlots = slots.stream()
	            .filter(s -> !s.date.isBefore(rangeFrom) && !s.date.isAfter(rangeTo))
	            .toList();

	    if (effectiveSlots.isEmpty()) {
	        return List.of();
	    }

	    LocalDate first = effectiveSlots.get(0).date;
	    LocalDate last  = effectiveSlots.get(effectiveSlots.size() - 1).date;

	    // 2) Programaziotik chunk zerrenda
	    List<PlannedChunk> chunks = flattenProgramazioa(programazioa);
	    if (chunks.isEmpty()) return List.of();

	    // 3) PLANIFIKATUA jarduerak ezabatu tarte horretan (EB bakarra bada, EB tartea; bestela ikasturte osoa)
	    if (!preview && replaceExisting) {
	        jardueraRepository.deleteByKoadernoaAndDataBetweenAndMota(
	                koadernoa,
	                first,
	                last,
	                "planifikatua"
	        );
	    }

	    // 4) Slot -> chunk esleipena (effectiveSlots erabilita)
	    List<PreviewItem> generated = allocate(chunks, effectiveSlots).stream()
	            .map(a -> new PreviewItem(a.date, a.title, a.hours, "planifikatua", a.description))
	            .toList();

	    // 5) DB-an idatzi
	    if (!preview) {
	        List<Jarduera> entities = generated.stream().map(p -> {
	            Jarduera j = new Jarduera();
	            j.setKoadernoa(koadernoa);
	            j.setData(p.data());
	            j.setTitulua(p.titulua());
	            j.setDeskribapena(p.azalpena());
	            j.setOrduak(p.orduak());
	            j.setMota(p.mota());
	            return j;
	        }).toList();
	        jardueraRepository.saveAll(entities);
	    }

	    return generated;
	}
	
	// ==== 1) Egutegi + Ordutegi -> session slot zerrenda ====
	
	private record SessionSlot(LocalDate date, int slots) {}
	
	private List<SessionSlot> buildSessionSlots(Koadernoa k) {
	    Egutegia e = k.getEgutegia();
	    if (e == null || e.getHasieraData() == null || e.getBukaeraData() == null) return List.of();

	    // 1) Egun berezien map-ak
	    //    - data -> EgunMota (LEKTIBOA, EZ_LEKTIBOA, JAIEGUNA, ORDEZKATUA)
	    //    - data -> Astegunak ordezkaria (ORDEZKATUA denean soilik)
	    Map<LocalDate, EgunMota> egunMota = Optional.ofNullable(e.getEgunBereziak()).orElse(List.of()).stream()
	        .filter(eb -> eb.getData() != null && eb.getMota() != null)
	        .collect(Collectors.toMap(EgunBerezi::getData, EgunBerezi::getMota, (a, b) -> a));

	    Map<LocalDate, Astegunak> ordezkapenak = Optional.ofNullable(e.getEgunBereziak()).orElse(List.of()).stream()
	        .filter(eb -> eb.getData() != null && eb.getMota() == EgunMota.ORDEZKATUA && eb.getOrdezkatua() != null)
	        .collect(Collectors.toMap(EgunBerezi::getData, EgunBerezi::getOrdezkatua, (a, b) -> a));

	    // 2) Koadernoaren ordutegia: astegun bakoitzean zenbat slot (ordu) dauden
	    Map<Astegunak, Integer> slotsByWeekday = Optional.ofNullable(k.getOrdutegiak()).orElse(List.of()).stream()
	        .collect(Collectors.groupingBy(KoadernoOrdutegiBlokea::getAsteguna,
	            Collectors.summingInt(KoadernoOrdutegiBlokea::getIraupenaSlot)));

	    if (slotsByWeekday.isEmpty()) return List.of();

	    // 3) Egun bakoitzerako slot eraginkorrak sortu
	    List<SessionSlot> out = new ArrayList<>();
	    LocalDate d = e.getHasieraData();
	    while (!d.isAfter(e.getBukaeraData())) {
	        // a) Aste egun nominala (egutegi naturala)
	        Astegunak nominal = toAstegunak(d.getDayOfWeek());
	        // b) Ordezkapena badago (ORDEZKATUA), erabili ordezko asteguna
	        Astegunak effective = ordezkapenak.getOrDefault(d, nominal);

	        // c) Egun horretan zenbat slot dauden (astegun eraginkorraren arabera)
	        int dailySlots = slotsByWeekday.getOrDefault(effective, 0);

	        if (dailySlots > 0) {
	            // d) Egun hori lectiboa den ala ez
	            EgunMota mota = egunMota.get(d);
	            boolean lectivo = (mota == null) || switch (mota) {
	                case LEKTIBOA, ORDEZKATUA -> true;   // ORDEZKATUA -> lektibo
	                case EZ_LEKTIBOA, JAIEGUNA -> false;
	            };

	            if (lectivo) {
	                out.add(new SessionSlot(d, dailySlots));
	            }
	        }
	        d = d.plusDays(1);
	    }
	    return out;
	}
	
	private static Astegunak toAstegunak(DayOfWeek d) {
		return switch (d) {
		case MONDAY -> Astegunak.ASTELEHENA;
		case TUESDAY -> Astegunak.ASTEARTEA;
		case WEDNESDAY -> Astegunak.ASTEAZKENA;
		case THURSDAY -> Astegunak.OSTEGUNA;
		case FRIDAY -> Astegunak.OSTIRALA;
		case SATURDAY -> Astegunak.LARUNBATA;
		case SUNDAY -> Astegunak.IGANDEA;
		};
	}
	
	// ==== 2) Programazioa -> chunk sekuentzia ====
	
	private record PlannedChunk(String title, String description, int hours, int udOrder) {}


	private List<PlannedChunk> flattenProgramazioa(Programazioa p) {
	    // 1) EBAL → UD flatten (EBAL.ordena → UD.posizioa → UD.id)
	    List<UnitateDidaktikoa> uds =
	        java.util.Optional.ofNullable(p.getEbaluaketak()).orElse(java.util.List.of()).stream()
	            .sorted(java.util.Comparator.comparing(
	                (com.koadernoa.app.objektuak.koadernoak.entitateak.Ebaluaketa e) ->
	                    java.util.Optional.ofNullable(e.getOrdena()).orElse(0)
	            ))
	            .flatMap(e ->
	                java.util.Optional.ofNullable(e.getUnitateak()).orElse(java.util.List.of()).stream()
	                    .sorted(java.util.Comparator
	                        .comparingInt(com.koadernoa.app.objektuak.koadernoak.entitateak.UnitateDidaktikoa::getPosizioa)
	                        .thenComparing(com.koadernoa.app.objektuak.koadernoak.entitateak.UnitateDidaktikoa::getId))
	            )
	            .collect(java.util.stream.Collectors.toList());

	    // 2) UD bakoitza → (azpijarduerak lehenik, gero UD-ren “gainerakoa”)
	    java.util.List<PlannedChunk> out = new java.util.ArrayList<>();
	    int udOrder = 0;

	    for (com.koadernoa.app.objektuak.koadernoak.entitateak.UnitateDidaktikoa ud : uds) {
	        udOrder++;

	        java.util.List<com.koadernoa.app.objektuak.koadernoak.entitateak.JardueraPlanifikatua> azpi =
	            java.util.Optional.ofNullable(ud.getAzpiJarduerak()).orElse(java.util.List.of());

	        int consumed = 0;

	        if (!azpi.isEmpty()) {
	            for (com.koadernoa.app.objektuak.koadernoak.entitateak.JardueraPlanifikatua jp : azpi) {
	                String title = jp.getIzenburua();
	                int h = java.lang.Math.max(0, java.util.Optional.ofNullable(jp.getOrduak()).orElse(0));
	                if (h > 0) {
	                    out.add(new PlannedChunk(
	                        title,
	                        ud.getKodea() + " — " + ud.getIzenburua(),
	                        h,
	                        udOrder
	                    ));
	                    consumed += h;
	                }
	            }
	        }

	        int udTotal = java.lang.Math.max(0, java.util.Optional.ofNullable(ud.getOrduak()).orElse(0));
	        int effective = java.lang.Math.max(ud.getOrduakEfektiboak(), udTotal);
	        int remaining = java.lang.Math.max(0, effective - consumed);

	        if (remaining > 0) {
	            out.add(new PlannedChunk(
	                ud.getIzenburua(),
	                ud.getKodea() + " — (UD orokorra)",
	                remaining,
	                udOrder
	            ));
	        }
	    }
	    return out;
	}
	
	// ==== 4) Esleipena: slots -> chunks ====
	
	private record Alloc(LocalDate date, String title, String description, float hours) {}


	private List<Alloc> allocate(List<PlannedChunk> chunks, List<SessionSlot> slots) {
		List<Alloc> out = new ArrayList<>();
		
		int chunkIdx = 0;
		int chunkRemaining = chunks.get(0).hours();
		PlannedChunk current = chunks.get(0);


		for (SessionSlot s : slots) {
			int capacity = s.slots(); // slot bakoitza = 1 ordu
			while (capacity > 0 && chunkIdx < chunks.size()) {
				int take = Math.min(capacity, chunkRemaining);
				out.add(new Alloc(s.date(), current.title(), current.description(), take));
				capacity -= take;
				chunkRemaining -= take;
				if (chunkRemaining == 0) {
					chunkIdx++;
					if (chunkIdx < chunks.size()) {
						current = chunks.get(chunkIdx);
						chunkRemaining = current.hours();
					}
				}
			}
			if (chunkIdx >= chunks.size()) break; // dena esleituta
		}
		return out;
	}
}