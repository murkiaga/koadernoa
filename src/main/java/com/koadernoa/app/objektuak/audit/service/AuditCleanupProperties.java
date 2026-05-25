package com.koadernoa.app.objektuak.audit.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "audit")
public class AuditCleanupProperties {

    private Cleanup cleanup = new Cleanup();
    private Retention retention = new Retention();

    public Cleanup getCleanup() {
        return cleanup;
    }

    public void setCleanup(Cleanup cleanup) {
        this.cleanup = cleanup;
    }

    public Retention getRetention() {
        return retention;
    }

    public void setRetention(Retention retention) {
        this.retention = retention;
    }

    public static class Cleanup {
        private boolean enabled = true;
        private String cron = "0 30 3 * * SUN";
        private int batchSize = 5000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }

    public static class Retention {
        private int pageViewDays = 365;
        private int actionDays = 365;
        private int loginDays = 365;
        private int errorDays = 365;

        public int getPageViewDays() {
            return pageViewDays;
        }

        public void setPageViewDays(int pageViewDays) {
            this.pageViewDays = pageViewDays;
        }

        public int getActionDays() {
            return actionDays;
        }

        public void setActionDays(int actionDays) {
            this.actionDays = actionDays;
        }

        public int getLoginDays() {
            return loginDays;
        }

        public void setLoginDays(int loginDays) {
            this.loginDays = loginDays;
        }

        public int getErrorDays() {
            return errorDays;
        }

        public void setErrorDays(int errorDays) {
            this.errorDays = errorDays;
        }
    }
}
