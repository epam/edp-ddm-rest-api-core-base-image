package com.epam.digital.data.platform.restapi.core.audit;

import com.epam.digital.data.platform.starter.audit.model.AuditSourceInfo;

public interface AuditSourceInfoProvider {
    AuditSourceInfo getAuditSourceInfo();
}
