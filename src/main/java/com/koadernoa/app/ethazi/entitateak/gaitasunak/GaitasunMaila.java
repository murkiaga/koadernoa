package com.koadernoa.app.ethazi.entitateak.gaitasunak;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
	gaitasuna_id = 14
	maila_id     = 3    // Erabili
	deskribapena = "Sistema eragilea modu autonomoan instalatu, konfiguratu eta mantentzen du."
*/

@Entity
@Table(
    name = "ethazi_gaitasun_maila",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_ethazi_gaitasuna_maila",
            columnNames = {"gaitasuna_id", "maila_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class GaitasunMaila {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gaitasuna_id", nullable = false)
    private Gaitasuna gaitasuna;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maila_id", nullable = false)
    private MailakatzeMaila maila;

    @Column(columnDefinition = "TEXT")
    private String deskribapena;

    @OneToMany(
        mappedBy = "gaitasunMaila",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("ordena ASC")
    private List<LorpenAdierazlea> lorpenAdierazleak = new ArrayList<>();
}
