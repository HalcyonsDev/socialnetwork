package com.halcyon.userservice.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Aspect
@Slf4j
public class LoggingAspect {
    @Before("execution(* com.halcyon.userservice.service.*.*(..))")
    public void logServiceMethodCall(JoinPoint joinPoint) {
        log.info(
                "Method {} called with arguments {}",
                joinPoint.getSignature().getName(),
                Arrays.toString(joinPoint.getArgs())
        );
    }

    @AfterReturning(pointcut = "execution(* com.halcyon.userservice.service.*.*(..))", returning = "returnedValue")
    public void logServiceMethodReturn(JoinPoint joinPoint, Object returnedValue) {
        log.info(
                "Method {} returned {}",
                joinPoint.getSignature().getName(),
                returnedValue
        );
    }

    @AfterThrowing(pointcut = "execution(* com.halcyon.userservice.service.*.*(..))", throwing = "thrownException")
    public void logServiceMethodThrow(JoinPoint joinPoint, Exception thrownException) {
        log.info(
                "Method {} threw exception {}",
                joinPoint.getSignature().getName(),
                thrownException.getMessage()
        );
    }
}
