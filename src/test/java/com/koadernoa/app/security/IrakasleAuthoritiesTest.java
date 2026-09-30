package com.koadernoa.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Baimena;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Rola;

class IrakasleAuthoritiesTest {

    @Test
    void rolaEtaBaimenakAuthoritiesBihurtzenDitu() {
        Irakaslea irakaslea = new Irakaslea();
        irakaslea.setRola(Rola.IRAKASLEA);
        irakaslea.setBaimenak(Set.of(Baimena.IKASLEAK_KONTSULTATU));

        assertThat(IrakasleAuthorities.from(irakaslea))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_IRAKASLEA", "BAIMENA_IKASLEAK_KONTSULTATU");
    }

    @Test
    void baimenikGabeRolaBakarrikSortzenDu() {
        Irakaslea irakaslea = new Irakaslea();
        irakaslea.setRola(Rola.KUDEATZAILEA);

        assertThat(IrakasleAuthorities.from(irakaslea))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_KUDEATZAILEA");
    }
}
