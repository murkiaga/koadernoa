package com.koadernoa.app.koadernoak.entitateak;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KoadernoaSortuDto {
	private Long moduloaId;
    private List<Long> irakasleIdZerrenda; //gutxienez bat, sortzailea barne
}
