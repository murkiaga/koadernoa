package com.koadernoa.app.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.naming.directory.SearchControls;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;

@ExtendWith(MockitoExtension.class)
class ActiveDirectoryBilaketaServiceTest {

    @Mock LdapTemplate ldapTemplate;

    @Test
    void gutxienekoLuzeraBalioztatzenDuLdapKontsultarikEginGabe() {
        ActiveDirectoryBilaketaService service = service();
        assertThatThrownBy(() -> service.bilatu("a")).isInstanceOf(IllegalArgumentException.class);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    void ldapInjectionKaraktereakIhesEgitenDitu() {
        when(ldapTemplate.search(anyString(), anyString(), any(SearchControls.class), any(AttributesMapper.class)))
                .thenReturn(List.of());
        ActiveDirectoryBilaketaService service = service();

        service.bilatu("*)(objectClass=*)");

        ArgumentCaptor<String> filter = ArgumentCaptor.forClass(String.class);
        verify(ldapTemplate).search(eq("OU=Irakasleak"), filter.capture(),
                any(SearchControls.class), any(AttributesMapper.class));
        org.assertj.core.api.Assertions.assertThat(filter.getValue())
                .doesNotContain("*)(objectClass=*)")
                .contains("\\29\\28objectClass=*\\29");
    }

    private ActiveDirectoryBilaketaService service() {
        LdapSettings settings = new LdapSettings();
        settings.setUserSearchBase("OU=Irakasleak");
        return new ActiveDirectoryBilaketaService(ldapTemplate, settings);
    }
}
