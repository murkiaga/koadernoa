package com.koadernoa.app.objektuak.modulua.entitateak;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Reads the former database name while writing the current enum name. */
@Converter(autoApply = true)
public class HizkuntzaConverter implements AttributeConverter<Hizkuntza, String> {
    @Override
    public String convertToDatabaseColumn(Hizkuntza hizkuntza) {
        return hizkuntza == null ? null : hizkuntza.name();
    }

    @Override
    public Hizkuntza convertToEntityAttribute(String value) {
        if (value == null) return null;
        return "GAZTELERA".equals(value) ? Hizkuntza.GAZTELERA : Hizkuntza.valueOf(value);
    }
}
