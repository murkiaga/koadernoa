package com.koadernoa.app.ethazi.entitateak.gaitasunak;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
	1 - Ezagutu
	2 - Finkatu-Ulertu
	3 - Erabili
	4 - Proposatu
 */

@Entity
@Table(
    name = "ethazi_mailakatze_maila",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_ethazi_mailakatze_eredua_ordena",
            columnNames = {"eredua_id", "ordena"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class MailakatzeMaila {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eredua_id", nullable = false)
    private MailakatzeEredua eredua;

    @Column(nullable = false)
    private Integer ordena;

    @Column(nullable = false, length = 100)
    private String izena;
}
