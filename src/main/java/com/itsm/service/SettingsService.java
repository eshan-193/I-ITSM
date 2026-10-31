package com.itsm.service;

import com.itsm.domain.settings.SystemSetting;
import com.itsm.exception.ValidationException;
import com.itsm.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

/** Reads and writes runtime settings, falling back to defaults when unset. */
@Service
public class SettingsService {

    private final SystemSettingRepository repository;

    public SettingsService(SystemSettingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public String get(SettingKey key) {
        return repository.findById(key.name()).map(SystemSetting::getValue).orElse(key.getDefaultValue());
    }

    public int getInt(SettingKey key) {
        try {
            return Integer.parseInt(get(key));
        } catch (NumberFormatException e) {
            return Integer.parseInt(key.getDefaultValue());
        }
    }

    public double getDouble(SettingKey key) {
        try {
            return Double.parseDouble(get(key));
        } catch (NumberFormatException e) {
            return Double.parseDouble(key.getDefaultValue());
        }
    }

    public boolean getBoolean(SettingKey key) {
        return Boolean.parseBoolean(get(key));
    }

    @Transactional(readOnly = true)
    public Map<SettingKey, String> all() {
        Map<SettingKey, String> values = new EnumMap<>(SettingKey.class);
        for (SettingKey key : SettingKey.values()) {
            values.put(key, get(key));
        }
        return values;
    }

    @Transactional
    public void set(SettingKey key, String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(key + " cannot be empty");
        }
        validate(key, value.trim());
        SystemSetting setting = repository.findById(key.name()).orElseGet(() -> new SystemSetting(key.name(), value.trim()));
        setting.setValue(value.trim());
        repository.save(setting);
    }

    private void validate(SettingKey key, String value) {
        switch (key) {
            case SLA_WARNING_PERCENT -> requireRange(key, value, 10, 95);
            case CLUSTER_SIZE -> requireRange(key, value, 2, 20);
            case CLUSTER_WINDOW_MINUTES -> requireRange(key, value, 5, 1440);
            case DUPLICATE_THRESHOLD -> {
                try {
                    double d = Double.parseDouble(value);
                    if (d <= 0 || d >= 1) throw new ValidationException("Duplicate threshold must be between 0 and 1");
                } catch (NumberFormatException e) {
                    throw new ValidationException("Duplicate threshold must be a number");
                }
            }
            default -> { /* free text or boolean */ }
        }
    }

    private void requireRange(SettingKey key, String value, int min, int max) {
        try {
            int v = Integer.parseInt(value);
            if (v < min || v > max) {
                throw new ValidationException(key + " must be between " + min + " and " + max);
            }
        } catch (NumberFormatException e) {
            throw new ValidationException(key + " must be a whole number");
        }
    }
}
