package com.koadernoa.app.objektuak.zikloak.service;

import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.entitateak.MatrikulaEgoera;
import com.koadernoa.app.objektuak.modulua.repository.IkasleaRepository;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.InportazioTxostena;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.repository.TaldeaRepository;

import org.apache.poi.ss.usermodel.*;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InportazioZerbitzua {

	private final TaldeaRepository taldeaRepo;
    private final KoadernoaRepository koadernoaRepo;
    private final IkasleaRepository ikasleaRepo;
    private final MatrikulaRepository matrikulaRepo;

    private static final String SHEET_NAME = "Zerrenda";

    @Transactional
    public InportazioTxostena inportatuTaldekoXlsx(Long taldeaId, MultipartFile mf) throws IOException {
        if (mf == null || mf.isEmpty()) throw new IllegalArgumentException("Ez da fitxategirik jaso");
        String name = mf.getOriginalFilename() == null ? "" : mf.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".xlsx")) throw new IllegalArgumentException("XLSX fitxategia behar da");

        Taldea taldea = taldeaRepo.findById(taldeaId)
            .orElseThrow(() -> new IllegalArgumentException("Taldea ez da existitzen"));

      
        List<Koadernoa> koadernoak =
        	    koadernoaRepo.findByModuloa_Taldea_Id(taldeaId);

        InportazioTxostena tx = new InportazioTxostena();

        try (XSSFWorkbook wb = new XSSFWorkbook(mf.getInputStream())) {
            Sheet sh = wb.getSheet(SHEET_NAME);
            if (sh == null) throw new IllegalArgumentException("Ez da aurkitu '" + SHEET_NAME + "' orria");

            Map<String, Integer> col = readHeaderFlexible(sh.getRow(0));

            for (int r = 1; r <= sh.getLastRowNum(); r++) {
                Row row = sh.getRow(r);
                if (row == null) continue;

                String hna = normHna(getCellStr(row, col.get("HNA")));
                if (isBlank(hna)) {
                    tx.getOharrak().add("R" + (r + 1) + ": HNA hutsik; lerroa baztertua");
                    tx.setBaztertuak(tx.getBaztertuak() + 1);
                    continue;
                }

                String nan   = norm(getCellStr(row, col.get("NAN")));
                String ab1   = norm(getCellStr(row, col.get("AB1")));
                String ab2   = norm(getCellStr(row, col.get("AB2")));
                String izena = norm(getCellStr(row, col.get("IZENA")));

                // HNA agintzen du
                Ikaslea ik = ikasleaRepo.findByHna(hna).orElseGet(Ikaslea::new);
                boolean berria = (ik.getId() == null);

                if (berria) ik.setHna(hna);            // finkatu behin
                if (!isBlank(nan)) ik.setNan(nan);
                if (!isBlank(izena)) ik.setIzena(izena);
                if (!isBlank(ab1)) ik.setAbizena1(ab1);
                if (!isBlank(ab2)) ik.setAbizena2(ab2);
                ik.setTaldea(taldea);                  // egungo taldea snapshot sinplea

                ikasleaRepo.save(ik);
                if (berria) tx.setSortuak(tx.getSortuak() + 1);
                else tx.setEguneratuak(tx.getEguneratuak() + 1);

                // Matrikulatu TALDEKO KOADERNO GUZTIETAN (duplicaterik ez)
                for (Koadernoa koa : koadernoak) {
                    boolean badago = matrikulaRepo.existsByIkasleaIdAndKoadernoaId(ik.getId(), koa.getId());
                    if (!badago) {
                        Matrikula m = new Matrikula();
                        m.setIkaslea(ik);
                        m.setKoadernoa(koa);
                        m.setEgoera(MatrikulaEgoera.MATRIKULATUA);
                        matrikulaRepo.save(m);
                    }
                }
            }
        }
        return tx;
    }

    // =======================
    //       LAGUNTZAILEAK
    // =======================

    /**
     * Goiburu malgua: lehenik izenez bilatzen du (sinonimoak onartuta),
     * ezin bada, posizio lehenetsietara erortzen da:
     * 0=Zbk (skip), 1=HNA, 2=NAN, 3=Abizena_1, 4=Abizena_2, 5=Izena
     */
    private Map<String, Integer> readHeaderFlexible(Row header) {
        Map<String, Integer> byName = new HashMap<>();
        if (header != null) {
            short last = header.getLastCellNum();
            for (int i = 0; i < last; i++) {
                String key = normHeader(getCellStr(header, i));
                if (key != null) byName.put(key, i);
            }
        }

        Integer iHna = findCol(byName, List.of("HNA DIE","HNA","DIE"));
        Integer iNan = findCol(byName, List.of("NAN DNI","NAN","DNI"));
        Integer iAb1 = findCol(byName, List.of("ABIZENA_1 APELLIDO_1","ABIZENA 1","APELLIDO 1","APELLIDO_1"));
        Integer iAb2 = findCol(byName, List.of("ABIZENA_2 APELLIDO_2","ABIZENA 2","APELLIDO 2","APELLIDO_2"));
        Integer iIze = findCol(byName, List.of("IZENA NOMBRE","IZENA","NOMBRE"));

        // Denak huts? erori posizio lehenetsietara (ordenaren arabera)
        if (iHna == null && iNan == null && iAb1 == null && iAb2 == null && iIze == null) {
            iHna = 1; iNan = 2; iAb1 = 3; iAb2 = 4; iIze = 5;
        }

        Map<String, Integer> out = new HashMap<>();
        out.put("HNA", iHna);
        out.put("NAN", iNan);
        out.put("AB1", iAb1);
        out.put("AB2", iAb2);
        out.put("IZENA", iIze);
        return out;
    }

    private Integer findCol(Map<String, Integer> byName, List<String> candidates) {
        for (String c : candidates) {
            String k = normHeader(c);
            Integer i = byName.get(k);
            if (i != null) return i;
            // bilaketa lausoagoa (substring)
            for (var e : byName.entrySet()) {
                if (e.getKey().contains(k)) return e.getValue();
            }
        }
        return null;
    }

    private String normHeader(String s) {
        if (s == null) return null;
        String t = s.trim().toUpperCase().replace('_', ' ');
        t = t.replaceAll("\\s+", " ");
        // diakritikoak kendu (segurtasunagatik)
        t = Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return t;
    }

    /** Gelaxka → String segurua: zenbakizkoak ez daitezela notazio zientifikora joan */
    private String getCellStr(Row row, Integer idx) {
        if (row == null || idx == null || idx < 0) return null;
        Cell c = row.getCell(idx);
        if (c == null) return null;

        try {
            return switch (c.getCellType()) {
                case STRING -> blankToNull(c.getStringCellValue());
                case NUMERIC -> {
                    double d = c.getNumericCellValue();
                    // zenbaki osoa bada, ez jarri .0
                    if (Math.floor(d) == d) yield Long.toString(Math.round(d));
                    else yield Double.toString(d);
                }
                case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
                case FORMULA -> {
                    try { yield blankToNull(c.getStringCellValue()); }
                    catch (Exception e1) {
                        try {
                            double d = c.getNumericCellValue();
                            if (Math.floor(d) == d) yield Long.toString(Math.round(d));
                            else yield Double.toString(d);
                        } catch (Exception e2) {
                            yield blankToNull(c.toString());
                        }
                    }
                }
                default -> blankToNull(c.toString());
            };
        } catch (Exception ex) {
            return blankToNull(c.toString());
        }
    }

    private String blankToNull(String s) { return (s == null || s.trim().isBlank()) ? null : s.trim(); }
    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private String norm(String s) { return isBlank(s) ? null : s.trim().replaceAll("\\s+", " "); }

    /** HNA normalizazioa: maiuskula + karaktere ez-alfanumerikoak kendu */
    private String normHna(String s) {
        if (isBlank(s)) return null;
        String t = s.trim().toUpperCase();
        return t.replaceAll("[^A-Z0-9]", "");
    }
}
