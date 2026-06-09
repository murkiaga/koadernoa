package com.koadernoa.app.objektuak.jokabidea.entitateak;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "jokabide_desegokia", indexes = {
    @Index(name = "ix_jokabide_desegokia_koaderno_data", columnList = "koadernoa_id, data"),
    @Index(name = "ix_jokabide_desegokia_ikasle_data", columnList = "ikaslea_id, data")
})
public class JokabideDesegokia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "ikaslea_id", nullable = false)
    private Ikaslea ikaslea;
    @ManyToOne(optional = false) @JoinColumn(name = "koadernoa_id", nullable = false)
    private Koadernoa koadernoa;
    @ManyToOne(optional = false) @JoinColumn(name = "irakaslea_id", nullable = false)
    private Irakaslea irakaslea;
    @ManyToOne(optional = false) @JoinColumn(name = "moduloa_id", nullable = false)
    private Moduloa moduloa;
    @Column(nullable = false)
    private LocalDate data;
    @ManyToOne(optional = false) @JoinColumn(name = "portaera_arrazoia_id", nullable = false)
    private PortaeraArrazoia portaeraArrazoia;
    @ManyToOne(optional = false) @JoinColumn(name = "neurri_zuzentzailea_id", nullable = false)
    private NeurriZuzentzailea neurriZuzentzailea;
    @Column(nullable = false, length = 10000)
    private String deskribapenZehatza;
    @Column(length = 2000)
    private String pdfPath;
    @Column(length = 255)
    private String pdfFilename;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @PrePersist void sortzean() { createdAt = LocalDateTime.now(); }
}
