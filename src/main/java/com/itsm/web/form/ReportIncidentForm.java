package com.itsm.web.form;

import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Urgency;
import com.itsm.service.ReportIncidentCommand;
import jakarta.validation.constraints.*;

/** The "report an incident" form, validated with Bean Validation before reaching the service. */
public class ReportIncidentForm {

    @NotBlank(message = "Please give the incident a short title")
    @Size(min = 5, max = 150, message = "Title must be 5–150 characters")
    private String title;

    @NotBlank(message = "Please describe the problem")
    @Size(min = 10, max = 4000, message = "Description must be 10–4000 characters")
    private String description;

    @NotNull(message = "Choose the affected service")
    private Long serviceId;

    private Long categoryId;

    @NotNull(message = "Choose the impact")
    private Impact impact = Impact.LOW;

    @NotNull(message = "Choose the urgency")
    private Urgency urgency = Urgency.MEDIUM;

    @Min(value = 1, message = "At least one user is affected")
    @Max(value = 100000, message = "That is more users than the organisation has")
    private int affectedUsers = 1;

    private boolean confirmedNotDuplicate;

    public ReportIncidentCommand toCommand() {
        return new ReportIncidentCommand(title, description, serviceId, categoryId, impact, urgency, affectedUsers, confirmedNotDuplicate);
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Impact getImpact() { return impact; }
    public void setImpact(Impact impact) { this.impact = impact; }
    public Urgency getUrgency() { return urgency; }
    public void setUrgency(Urgency urgency) { this.urgency = urgency; }
    public int getAffectedUsers() { return affectedUsers; }
    public void setAffectedUsers(int affectedUsers) { this.affectedUsers = affectedUsers; }
    public boolean isConfirmedNotDuplicate() { return confirmedNotDuplicate; }
    public void setConfirmedNotDuplicate(boolean confirmedNotDuplicate) { this.confirmedNotDuplicate = confirmedNotDuplicate; }
}
