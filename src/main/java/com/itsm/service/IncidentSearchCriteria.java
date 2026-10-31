package com.itsm.service;

import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.priority.Priority;
import com.itsm.domain.sla.SlaStatus;

import java.time.LocalDate;

/**
 * The search form: every field is optional. Nine criteria are supported:
 * incident number, keyword, status, priority, category, service, technician,
 * SLA status and a created-date range (plus "open only").
 */
public class IncidentSearchCriteria {

    private String number;
    private String keyword;
    private IncidentStatus status;
    private Priority priority;
    private Long categoryId;
    private Long serviceId;
    /** Technician id, or -1 for "unassigned". */
    private Long technicianId;
    private SlaStatus slaStatus;
    private LocalDate from;
    private LocalDate to;
    private boolean openOnly;

    public boolean isEmpty() {
        return blank(number) && blank(keyword) && status == null && priority == null && categoryId == null
                && serviceId == null && technicianId == null && slaStatus == null && from == null && to == null && !openOnly;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public Long getTechnicianId() { return technicianId; }
    public void setTechnicianId(Long technicianId) { this.technicianId = technicianId; }
    public SlaStatus getSlaStatus() { return slaStatus; }
    public void setSlaStatus(SlaStatus slaStatus) { this.slaStatus = slaStatus; }
    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate from) { this.from = from; }
    public LocalDate getTo() { return to; }
    public void setTo(LocalDate to) { this.to = to; }
    public boolean isOpenOnly() { return openOnly; }
    public void setOpenOnly(boolean openOnly) { this.openOnly = openOnly; }
}
