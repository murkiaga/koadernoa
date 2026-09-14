package com.koadernoa.app.ethazi.entitateak.gaitasunak;

import java.util.ArrayList;
import java.util.List;

import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Table(
    name = "ethazi_mailakatze_eredua",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_ethazi_mailakatzea_zikloa_mota",
            columnNames = {"zikloa_id", "mota"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class MailakatzeEredua {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zikloa_id", nullable = false)
    private Zikloa zikloa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GaitasunMota mota;

    @Column(nullable = false, length = 150)
    private String izena;

    @OneToMany(
        mappedBy = "eredua",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("ordena ASC")
    private List<MailakatzeMaila> mailak = new ArrayList<>();
}
