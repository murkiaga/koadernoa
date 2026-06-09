package com.koadernoa.app.objektuak.jokabidea.entitateak;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ikasle_egun_oharra",
    uniqueConstraints = @UniqueConstraint(name = "uk_ikasle_egun_oharra", columnNames = {"ikaslea_id", "koadernoa_id", "data"}),
    indexes = @Index(name = "ix_ikasle_egun_oharra_koaderno_data", columnList = "koadernoa_id, data"))
public class IkasleEgunOharra {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "ikaslea_id", nullable = false)
    private Ikaslea ikaslea;
    @ManyToOne(optional = false) @JoinColumn(name = "koadernoa_id", nullable = false)
    private Koadernoa koadernoa;
    @Column(nullable = false)
    private LocalDate data;
    @ManyToOne(optional = false) @JoinColumn(name = "irakaslea_id", nullable = false)
    private Irakaslea irakaslea;
    @Column(nullable = false, length = 5000)
    private String testua;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    @PrePersist void sortzean() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void eguneratzean() { updatedAt = LocalDateTime.now(); }
}
