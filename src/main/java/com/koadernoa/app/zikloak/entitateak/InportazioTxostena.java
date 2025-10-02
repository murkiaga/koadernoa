package com.koadernoa.app.zikloak.entitateak;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class InportazioTxostena {
	 private int sortuak;
	  private int eguneratuak;
	  private int baztertuak;
	  private List<String> oharrak = new ArrayList<>();
}
