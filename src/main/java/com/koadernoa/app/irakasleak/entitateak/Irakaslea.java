package com.koadernoa.app.irakasleak.entitateak;

import java.util.List;

import com.koadernoa.app.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.zikloak.entitateak.Familia;
import com.koadernoa.app.zikloak.entitateak.Taldea;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Irakaslea {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String izena;
    private String emaila;
    private String pasahitza;
    private String kontu_mota;
    
    @Enumerated(EnumType.STRING)
    private Familia mintegia;
    
    @ManyToMany(mappedBy = "irakasleak")
    private List<Koadernoa> koadernoak;
    
    @Enumerated(EnumType.STRING)
    private Rola rola;
    
    @OneToOne(mappedBy = "tutorea", fetch = FetchType.LAZY)
    private Taldea tutoreTaldea;

}
