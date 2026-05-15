package com.koadernoa.app.objektuak.koadernoak.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Rola;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KoadernoJabeEsleipenService {

    private static final String[] HEADERS = {
            "koaderno_id",
            "ikasturtea",
            "familia",
            "zikloa",
            "taldea",
            "maila",
            "modulo_id",
            "eei_kodea",
            "modulo_kodea",
            "modulo_izena",
            "jabe_emaila",
            "irakasle_emailak"
    };

    private final KoadernoaRepository koadernoaRepository;
    private final IrakasleaRepository irakasleaRepository;

    public boolean badagoJabeGabekoKoadernorik() {
        return koadernoaRepository.existsByJabeaIsNull();
    }

    public List<Koadernoa> bilatuJabeGabekoKoadernoak() {
        return koadernoaRepository.findJabeGabekoakWithRelations();
    }

    public byte[] exportatuJabeGabekoKoadernoak() throws IOException {
        List<Koadernoa> koadernoak = bilatuJabeGabekoKoadernoak();
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("jabe-gabeak");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }

            int rowIdx = 1;
            for (Koadernoa koadernoa : koadernoak) {
                Row row = sheet.createRow(rowIdx++);
                Moduloa moduloa = koadernoa.getModuloa();
                Egutegia egutegia = koadernoa.getEgutegia();
                Taldea taldea = moduloa != null ? moduloa.getTaldea() : null;
                Zikloa zikloa = taldea != null ? taldea.getZikloa() : null;
                Familia familia = zikloa != null ? zikloa.getFamilia() : null;

                row.createCell(0).setCellValue(koadernoa.getId() != null ? koadernoa.getId() : 0L);
                row.createCell(1).setCellValue(egutegia != null && egutegia.getIkasturtea() != null ? balioa(egutegia.getIkasturtea().getIzena()) : "");
                row.createCell(2).setCellValue(familia != null ? balioa(familia.getIzena()) : "");
                row.createCell(3).setCellValue(zikloa != null ? balioa(zikloa.getIzena()) : "");
                row.createCell(4).setCellValue(taldea != null ? balioa(taldea.getIzena()) : "");
                row.createCell(5).setCellValue(egutegia != null && egutegia.getMaila() != null ? balioa(egutegia.getMaila().getIzena()) : "");
                row.createCell(6).setCellValue(moduloa != null && moduloa.getId() != null ? moduloa.getId() : 0L);
                row.createCell(7).setCellValue(moduloa != null ? balioa(moduloa.getEeiKodea()) : "");
                row.createCell(8).setCellValue(moduloa != null ? balioa(moduloa.getKodea()) : "");
                row.createCell(9).setCellValue(moduloa != null ? balioa(moduloa.getIzena()) : "");
                row.createCell(10).setCellValue("");
                row.createCell(11).setCellValue("");
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    @Transactional
    public KoadernoJabeInportazioEmaitza inportatuJabeEsleipenak(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Ez da fitxategirik jaso.");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) {
            throw new IllegalArgumentException("Excel fitxategia behar da (.xlsx edo .xls).");
        }

        KoadernoJabeInportazioEmaitza emaitza = new KoadernoJabeInportazioEmaitza();
        DataFormatter formatter = new DataFormatter();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IllegalArgumentException("Excel fitxategiak ez du orririk.");
            }

            Map<String, Integer> col = readHeader(sheet.getRow(0), formatter);
            for (String header : List.of("koaderno_id", "jabe_emaila")) {
                if (!col.containsKey(header)) {
                    throw new IllegalArgumentException("Excel fitxategian '" + header + "' zutabea falta da.");
                }
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || rowHutsa(row, formatter)) {
                    continue;
                }
                emaitza.setProzesatutakoLerroak(emaitza.getProzesatutakoLerroak() + 1);
                try {
                    prozesatuLerroa(row, r + 1, col, formatter, emaitza);
                } catch (Exception ex) {
                    emaitza.gehituErrorea("L" + (r + 1) + ": ustekabeko errorea: " + ex.getMessage());
                }
            }
        }
        return emaitza;
    }

    private void prozesatuLerroa(Row row, int excelLerroa, Map<String, Integer> col, DataFormatter formatter,
                                 KoadernoJabeInportazioEmaitza emaitza) {
        String koadernoIdRaw = getCellStr(row, col.get("koaderno_id"), formatter);
        Long koadernoId = parseLong(koadernoIdRaw);
        if (koadernoId == null) {
            emaitza.gehituErrorea("L" + excelLerroa + ": koaderno_id baliogabea ('" + koadernoIdRaw + "').");
            return;
        }

        String jabeEmaila = getCellStr(row, col.get("jabe_emaila"), formatter).trim();
        if (jabeEmaila.isBlank()) {
            emaitza.gehituErrorea("L" + excelLerroa + " (koaderno_id=" + koadernoId + "): jabe_emaila hutsik dago.");
            return;
        }

        Koadernoa koadernoa = koadernoaRepository.findByIdWithJabeaEtaIrakasleak(koadernoId)
                .orElse(null);
        if (koadernoa == null) {
            emaitza.gehituErrorea("L" + excelLerroa + " (koaderno_id=" + koadernoId + "): koadernoa ez da existitzen.");
            return;
        }
        if (koadernoa.getJabea() != null) {
            emaitza.gehituErrorea("L" + excelLerroa + " (koaderno_id=" + koadernoId + "): Koaderno honek jabea dauka jada.");
            return;
        }

        Irakaslea jabea = bilatuIrakaslea(jabeEmaila, excelLerroa, koadernoId, emaitza);
        if (jabea == null) {
            return;
        }

        koadernoa.setJabea(jabea);
        gehituIrakasleaBeharBada(koadernoa, jabea);

        String irakasleEmailak = getCellStr(row, col.get("irakasle_emailak"), formatter);
        if (!irakasleEmailak.isBlank()) {
            for (String email : irakasleEmailak.split(";")) {
                String garbia = email.trim();
                if (garbia.isBlank()) {
                    continue;
                }
                Irakaslea irakaslea = bilatuIrakaslea(garbia, excelLerroa, koadernoId, emaitza);
                if (irakaslea != null) {
                    gehituIrakasleaBeharBada(koadernoa, irakaslea);
                }
            }
        }

        koadernoaRepository.save(koadernoa);
        emaitza.setEsleitutakoKoadernoak(emaitza.getEsleitutakoKoadernoak() + 1);
    }

    private Irakaslea bilatuIrakaslea(String emaila, int excelLerroa, Long koadernoId,
                                      KoadernoJabeInportazioEmaitza emaitza) {
        Irakaslea irakaslea = irakasleaRepository.findByEmailaIgnoreCase(emaila).orElse(null);
        if (irakaslea == null) {
            emaitza.gehituErrorea("L" + excelLerroa + " (koaderno_id=" + koadernoId + "): ez da irakaslerik aurkitu email honekin: " + emaila + ".");
            return null;
        }
        if (irakaslea.getRola() != Rola.IRAKASLEA) {
            emaitza.gehituErrorea("L" + excelLerroa + " (koaderno_id=" + koadernoId + "): '" + emaila + "' ez da IRAKASLEA rola duen erabiltzailea.");
            return null;
        }
        return irakaslea;
    }

    private void gehituIrakasleaBeharBada(Koadernoa koadernoa, Irakaslea irakaslea) {
        if (koadernoa.getIrakasleak() == null) {
            koadernoa.setIrakasleak(new ArrayList<>());
        } else if (!(koadernoa.getIrakasleak() instanceof ArrayList)) {
            koadernoa.setIrakasleak(new ArrayList<>(koadernoa.getIrakasleak()));
        }
        boolean badago = koadernoa.getIrakasleak().stream()
                .anyMatch(i -> i.getId() != null && i.getId().equals(irakaslea.getId()));
        if (!badago) {
            koadernoa.getIrakasleak().add(irakaslea);
        }
    }

    private Map<String, Integer> readHeader(Row row, DataFormatter formatter) {
        Map<String, Integer> col = new HashMap<>();
        if (row == null) {
            return col;
        }
        for (Cell cell : row) {
            String value = formatter.formatCellValue(cell);
            if (value != null && !value.isBlank()) {
                col.put(value.trim().toLowerCase(Locale.ROOT), cell.getColumnIndex());
            }
        }
        return col;
    }

    private boolean rowHutsa(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (!formatter.formatCellValue(cell).trim().isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String getCellStr(Row row, Integer column, DataFormatter formatter) {
        if (row == null || column == null) {
            return "";
        }
        Cell cell = row.getCell(column);
        return cell == null ? "" : formatter.formatCellValue(cell);
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String normalized = value.trim();
            if (normalized.endsWith(".0")) {
                normalized = normalized.substring(0, normalized.length() - 2);
            }
            return Long.parseLong(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String balioa(String value) {
        return value == null ? "" : value;
    }
}
