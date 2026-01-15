package com.koadernoa.app.funtzionalitateak.kudeatzaile.EstatistikaDashboard;

public class EstatistikakFiltroa {
	private Long familiaId;
    private Long zikloaId;
    private Long taldeaId;
    private Long mailaId;

    private String ebaluazioKodea; // adib: 1_EBAL, 2_EBAL...
    private Boolean kalkulatua;
    private Boolean gainditua;     // null=denak, true/false

    // getters/setters
    public Long getFamiliaId() { return familiaId; }
    public void setFamiliaId(Long familiaId) { this.familiaId = familiaId; }
    public Long getZikloaId() { return zikloaId; }
    public void setZikloaId(Long zikloaId) { this.zikloaId = zikloaId; }
    public Long getTaldeaId() { return taldeaId; }
    public void setTaldeaId(Long taldeaId) { this.taldeaId = taldeaId; }
    public Long getMailaId() { return mailaId; }
    public void setMailaId(Long mailaId) { this.mailaId = mailaId; }
    public String getEbaluazioKodea() { return ebaluazioKodea; }
    public void setEbaluazioKodea(String ebaluazioKodea) { this.ebaluazioKodea = ebaluazioKodea; }
    public Boolean getKalkulatua() { return kalkulatua; }
    public void setKalkulatua(Boolean kalkulatua) { this.kalkulatua = kalkulatua; }
    public Boolean getGainditua() { return gainditua; }
    public void setGainditua(Boolean gainditua) { this.gainditua = gainditua; }
}
