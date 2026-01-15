package com.koadernoa.app.objektuak.koadernoak.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.koadernoa.app.funtzionalitateak.kudeatzaile.EstatistikaDashboard.EstatistikakFiltroa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.EstatistikaEbaluazioan;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public class EstatistikaSpecs {
	public static Specification<EstatistikaEbaluazioan> byFiltroa(EstatistikakFiltroa f) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            // JOIN path-ak (zure eremuen izenak egokitu)
            Join<Object, Object> k = root.join("koadernoa");
            Join<Object, Object> m = k.join("moduloa");
            Join<Object, Object> t = m.join("taldea");
            Join<Object, Object> z = t.join("zikloa");
            Join<Object, Object> fa = z.join("familia");

            Join<Object, Object> e = k.join("egutegia", JoinType.LEFT);
            Join<Object, Object> maila = e.join("maila", JoinType.LEFT);

            if (f.getFamiliaId() != null) ps.add(cb.equal(fa.get("id"), f.getFamiliaId()));
            if (f.getZikloaId() != null) ps.add(cb.equal(z.get("id"), f.getZikloaId()));
            if (f.getTaldeaId() != null) ps.add(cb.equal(t.get("id"), f.getTaldeaId()));
            if (f.getMailaId() != null) ps.add(cb.equal(maila.get("id"), f.getMailaId()));

            if (f.getEbaluazioKodea() != null && !f.getEbaluazioKodea().isBlank()) {
                Join<Object, Object> em = root.join("ebaluazioMomentua");
                ps.add(cb.equal(em.get("kodea"), f.getEbaluazioKodea()));
            }

            if (f.getKalkulatua() != null) ps.add(cb.equal(root.get("kalkulatua"), f.getKalkulatua()));
            if (f.getGainditua() != null) ps.add(cb.equal(root.get("gainditua"), f.getGainditua()));

            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    public static Specification<EstatistikaEbaluazioan> kalkulatua(boolean v) {
        return (r, q, cb) -> cb.equal(r.get("kalkulatua"), v);
    }

    public static Specification<EstatistikaEbaluazioan> gainditua(boolean v) {
        return (r, q, cb) -> cb.equal(r.get("gainditua"), v);
    }
}
