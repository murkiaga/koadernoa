package com.koadernoa.app.funtzionalitateak.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.koadernoa.app.objektuak.audit.entitateak.AuditAtala;
import com.koadernoa.app.objektuak.audit.entitateak.AuditEkintza;
import com.koadernoa.app.objektuak.audit.entitateak.AuditEventMota;
import com.koadernoa.app.objektuak.audit.repository.AuditEventRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AuditEventRepository auditEventRepository;

    @GetMapping
    public String index(@RequestParam(required = false) LocalDate hasieraData,
                        @RequestParam(required = false) LocalDate bukaeraData,
                        @RequestParam(required = false) AuditEventMota mota,
                        @RequestParam(required = false) AuditAtala atala,
                        @RequestParam(required = false) AuditEkintza ekintza,
                        @RequestParam(required = false) String erabiltzaileEmaila,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "25") int size,
                        Model model) {

        LocalDate today = LocalDate.now();
        LocalDate hasiera = hasieraData != null ? hasieraData : today.minusDays(30);
        LocalDate bukaera = bukaeraData != null ? bukaeraData : today;

        LocalDateTime from = hasiera.atStartOfDay();
        LocalDateTime toExclusive = bukaera.plusDays(1).atStartOfDay();
        String emailQ = (erabiltzaileEmaila != null && !erabiltzaileEmaila.isBlank()) ? erabiltzaileEmaila.trim() : null;

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "dataOrdua"));

        var eventsPage = auditEventRepository.findRecentWithFilters(from, toExclusive, mota, atala, ekintza, emailQ, pageable);

        model.addAttribute("loginOkCount", auditEventRepository.countByMotaWithFilters(from, toExclusive, mota, atala, ekintza, emailQ, AuditEventMota.LOGIN_OK));
        model.addAttribute("loginFailCount", auditEventRepository.countByMotaWithFilters(from, toExclusive, mota, atala, ekintza, emailQ, AuditEventMota.LOGIN_FAIL));
        model.addAttribute("pageViewCount", auditEventRepository.countByMotaWithFilters(from, toExclusive, mota, atala, ekintza, emailQ, AuditEventMota.PAGE_VIEW));
        model.addAttribute("actionCount", auditEventRepository.countByMotaWithFilters(from, toExclusive, mota, atala, ekintza, emailQ, AuditEventMota.ACTION));
        model.addAttribute("erabiltzaileAktiboak", auditEventRepository.countDistinctEmails(from, toExclusive, mota, atala, ekintza, emailQ));

        model.addAttribute("atalTop", auditEventRepository.groupByAtala(from, toExclusive, mota, atala, ekintza, emailQ));
        model.addAttribute("ekintzaTop", auditEventRepository.groupByEkintza(from, toExclusive, mota, atala, ekintza, emailQ));
        model.addAttribute("loginFailTop", auditEventRepository.topLoginFailByEmail(from, toExclusive, emailQ, PageRequest.of(0, 10)));

        model.addAttribute("eventsPage", eventsPage);
        model.addAttribute("motaGuztiak", AuditEventMota.values());
        model.addAttribute("atalaGuztiak", AuditAtala.values());
        model.addAttribute("ekintzaGuztiak", AuditEkintza.values());

        model.addAttribute("hasieraData", hasiera);
        model.addAttribute("bukaeraData", bukaera);
        model.addAttribute("mota", mota);
        model.addAttribute("atala", atala);
        model.addAttribute("ekintza", ekintza);
        model.addAttribute("erabiltzaileEmaila", erabiltzaileEmaila);
        model.addAttribute("size", size);

        return "admin/audit";
    }
}
