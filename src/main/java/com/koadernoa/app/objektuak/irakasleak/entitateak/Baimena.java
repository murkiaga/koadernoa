package com.koadernoa.app.objektuak.irakasleak.entitateak;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Baimena {
    IKASLEAK_KONTSULTATU("Ikasle guztien informazioa kontsultatu");

    private final String etiketa;

    public String getAuthority() {
        return "BAIMENA_" + name();
    }
}
