package com.koadernoa.app.funtzionalitateak.irakasle.baimenak.ikasleakkontsultatu;

import java.util.Map;
import java.util.Set;

import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IkaslearenMatrikulaLaburpena {
    private final Matrikula matrikula;
    private final Set<String> ebaluazioMomentuKodeak;
    private final Map<String, String> kalifikazioak;
    private final double hutsegitePortzentaia;
}
