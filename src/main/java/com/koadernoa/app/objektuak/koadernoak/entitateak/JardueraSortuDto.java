package com.koadernoa.app.objektuak.koadernoak.entitateak;
import java.time.LocalDate;
import lombok.Getter; import lombok.Setter;

@Getter
@Setter
public class JardueraSortuDto {
	private String titulua;
    private String deskribapena;
    private LocalDate data;
    private boolean eginda;
    private float orduak;
    private String mota; // "planifikatua", "eginda", "ebaluazioa", ...
}
