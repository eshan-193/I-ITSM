package com.itsm.domain.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A key/value configuration entry the administrator can change at runtime. */
@Entity
@Table(name = "system_settings")
public class SystemSetting {

    @Id
    @Column(name = "setting_key", length = 60)
    private String key;

    @Column(name = "setting_value", nullable = false, length = 200)
    private String value;

    protected SystemSetting() {
        // for JPA
    }

    public SystemSetting(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() { return key; }
    public String getValue() { return value; }

    public void setValue(String value) {
        this.value = value;
    }
}
