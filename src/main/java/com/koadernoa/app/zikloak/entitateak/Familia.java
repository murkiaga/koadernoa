package com.koadernoa.app.zikloak.entitateak;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "familia", uniqueConstraints = {
    @UniqueConstraint(name = "uk_familia_izena", columnNames = "izena")
})
public class Familia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String izena;

    @Column(length = 120)
    private String slug; // aukerazkoa, URLetarako baliagarria

    @Column(nullable = false)
    private boolean aktibo = true;
}
