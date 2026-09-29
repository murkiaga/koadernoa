package com.koadernoa.app.ethazi.entitateak;
import jakarta.persistence.*;
import lombok.Getter;
@Entity @Table(name="ethazi_erronka_bertsio_taldea") @Getter
public class ErronkaBertsioTaldea {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
}
