package com.bilibili.common.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class ServiceLogAspect {

    private static final Logger log = LoggerFactory.getLogger(ServiceLogAspect.class);

    @Around("execution(public * com.bilibili..service.impl..*(..))")
    public Object aroundService(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String method = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        String argsSummary = summarizeArgs(joinPoint.getArgs());

        try {
            Object result = joinPoint.proceed();
            long costMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            log.info("service success method={} costMs={} args={} result={}",
                    method, costMs, argsSummary, summarizeResult(result));
            return result;
        } catch (IllegalArgumentException ex) {
            long costMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            log.warn("service fail method={} costMs={} args={} message={}",
                    method, costMs, argsSummary, ex.getMessage());
            throw ex;
        } catch (Throwable ex) {
            long costMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            log.error("service error method={} costMs={} args={}", method, costMs, argsSummary, ex);
            throw ex;
        }
    }

    private static String summarizeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(summarizeValue(args[i]));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String summarizeValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof MultipartFile) {
            MultipartFile file = (MultipartFile) value;
            return "MultipartFile(name=" + file.getOriginalFilename()
                    + ", size=" + file.getSize()
                    + ", contentType=" + file.getContentType() + ")";
        }
        if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Collection) {
            return value.getClass().getSimpleName() + "(size=" + ((Collection<?>) value).size() + ")";
        }
        if (value instanceof Map) {
            return value.getClass().getSimpleName() + "(size=" + ((Map<?, ?>) value).size() + ")";
        }
        return value.getClass().getSimpleName();
    }

    private static String summarizeResult(Object result) {
        if (result == null) {
            return "void/null";
        }
        if (result instanceof Collection) {
            return result.getClass().getSimpleName() + "(size=" + ((Collection<?>) result).size() + ")";
        }
        if (result instanceof Map) {
            return result.getClass().getSimpleName() + "(size=" + ((Map<?, ?>) result).size() + ")";
        }
        return result.getClass().getSimpleName();
    }
}
