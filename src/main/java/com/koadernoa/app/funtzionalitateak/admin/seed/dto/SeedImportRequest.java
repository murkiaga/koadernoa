package com.koadernoa.app.funtzionalitateak.admin.seed.dto;

import java.util.ArrayList;
import java.util.List;

public class SeedImportRequest {
    private List<String> zikloak = new ArrayList<>();
    private List<String> taldeak = new ArrayList<>();

    public List<String> getZikloak() {
        return zikloak;
    }

    public void setZikloak(List<String> zikloak) {
        this.zikloak = zikloak != null ? zikloak : new ArrayList<>();
    }

    public List<String> getTaldeak() {
        return taldeak;
    }

    public void setTaldeak(List<String> taldeak) {
        this.taldeak = taldeak != null ? taldeak : new ArrayList<>();
    }
}
