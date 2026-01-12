package com.koadernoa.app.objektuak.koadernoak.entitateak;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KoadernoaSortuDto {
	private Long familiaId;
	private Long moduloaId;
    private List<Long> irakasleIdZerrenda; //gutxienez bat, sortzailea barne
}
