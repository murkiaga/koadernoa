package com.koadernoa.app.security;

public record AdErabiltzailea(
        String identifikatzailea,
        String erabiltzaileIzena,
        String userPrincipalName,
        String izena,
        String abizenak,
        String izenOsoa,
        String emaila) {
}
