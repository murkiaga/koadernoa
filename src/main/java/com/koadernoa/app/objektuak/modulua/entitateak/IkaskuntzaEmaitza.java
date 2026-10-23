package com.koadernoa.app.objektuak.modulua.entitateak;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ikaskuntza_emaitza", uniqueConstraints = @jakarta.persistence.UniqueConstraint(name="uk_ie_eei_ordena", columnNames={"eei_kodea", "ordena"}))
@Getter
@Setter
@NoArgsConstructor
public class IkaskuntzaEmaitza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="eei_kodea", nullable=false)
    private String eeiKodea;

    @Column(nullable = false)
    private Integer ordena;

    @Column(length = 20)
    private String kodea;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String deskribapena;

    @Column(columnDefinition = "TEXT")
    private String deskribapenaEs;
    @Column(columnDefinition = "TEXT")
    private String deskribapenaEn;

    public String deskribapena(com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza hizkuntza) {
        return com.koadernoa.app.ethazi.service.HizkuntzaTestua.erakutsi(hizkuntza, deskribapena, deskribapenaEs, deskribapenaEn);
    }
}
