/*
 * Copyright 2023 EPAM Systems.
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

package com.epam.digital.data.platform.restapi.core.utils;

import java.util.Optional;

public class PageableUtils {

  public static final int DEFAULT_PAGE_NUMBER = 0;
  public static final int DEFAULT_PAGE_SIZE = 10;

  private PageableUtils() {}

  public static int getTotalPages(Integer pageSize, Integer total) {
    int actualPageSize = Optional.ofNullable(pageSize).orElse(1);
    int actualTotal = Optional.ofNullable(total).orElse(0);
    if (actualPageSize <= 0) {
      return 1;
    } else {
      return (int) Math.ceil((double) actualTotal / actualPageSize);
    }
  }
}
