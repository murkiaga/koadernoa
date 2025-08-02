package com.koadernoa.app.koadernoak.entitateak;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class EstatistikaEbaluazioan {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private EbaluazioMota ebaluazioMota;

    private int unitateakEmanda;
    private int unitateakAurreikusiak;

    private int orduakEmanda;
    private int orduakAurreikusiak;

    private int ebaluatuak;
    private int aprobatuak;
    
    //private int ikasleKopurua; Ez da behar? ebaluatuak-ekin badakigu zenbat ikasle dauden
    private int hutsegiteOrduak;

    @ManyToOne
    private Koadernoa koadernoa;
}

