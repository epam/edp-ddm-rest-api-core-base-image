/*
 * Copyright 2021 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.restapi.core.audit;

import com.epam.digital.data.platform.starter.audit.model.AuditSourceInfo;
import com.epam.digital.data.platform.starter.audit.model.AuditUserInfo;
import java.time.LocalDateTime;

public final class AuditEventUtils {

  public static final String APP_NAME = "application";
  public static final String REQUEST_ID = "1";
  public static final String SOURCE_SYSTEM = "system";
  public static final String SOURCE_APPLICATION = "source_app";
  public static final String BUSINESS_PROCESS = "bp";
  public static final String BUSINESS_PROCESS_DEFINITION_ID = "bp_def_id";
  public static final String BUSINESS_PROCESS_INSTANCE_ID = "bp_id";
  public static final String BUSINESS_ACTIVITY = "act";
  public static final String BUSINESS_ACTIVITY_INSTANCE_ID = "bp_act";
  public static final String METHOD_NAME = "method";
  public static final String ACTION = "CREATE";
  public static final String STEP = "BEFORE";
  public static final String USER_DRFO = "1010101014";
  public static final String USER_KEYCLOAK_ID = "496fd2fd-3497-4391-9ead-41410522d06f";
  public static final String USER_NAME = "Сидоренко Василь Леонідович";
  public static final String RESULT = "RESULT";

  public static final LocalDateTime CURR_TIME = LocalDateTime.of(2021, 4, 1, 11, 50);

  private AuditEventUtils() {}

  public static AuditSourceInfo createSourceInfo() {
    return AuditSourceInfo.AuditSourceInfoBuilder.anAuditSourceInfo()
            .system(SOURCE_SYSTEM)
            .application(SOURCE_APPLICATION)
            .businessProcess(BUSINESS_PROCESS)
            .businessProcessDefinitionId(BUSINESS_PROCESS_DEFINITION_ID)
            .businessProcessInstanceId(BUSINESS_PROCESS_INSTANCE_ID)
            .businessActivity(BUSINESS_ACTIVITY)
            .businessActivityInstanceId(BUSINESS_ACTIVITY_INSTANCE_ID)
            .build();
  }

  public static AuditUserInfo createUserInfo() {
    return new AuditUserInfo(USER_NAME, USER_KEYCLOAK_ID, USER_DRFO);
  }
}
