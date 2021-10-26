package com.epam.digital.data.platform.restapi.core.audit;

import com.epam.digital.data.platform.restapi.core.exception.AuditException;
import java.util.Arrays;
import java.util.Objects;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;

public interface AuditProcessor<O> {

  Object process(ProceedingJoinPoint joinPoint, O operation) throws Throwable;

  default <T> T getArgumentByType(JoinPoint joinPoint, Class<T> clazz) {
    long numberOfArgumentsOfTheSameType = Arrays.stream(joinPoint.getArgs())
        .filter(Objects::nonNull)
        .filter(x -> x.getClass().equals(clazz))
        .count();

    if (numberOfArgumentsOfTheSameType != 1) {
      throw new AuditException("The number of arguments of the given type is not equal to one");
    }
    return (T) Arrays.stream(joinPoint.getArgs())
        .filter(Objects::nonNull)
        .filter(x -> x.getClass().equals(clazz))
        .findFirst().get();
  }
}
