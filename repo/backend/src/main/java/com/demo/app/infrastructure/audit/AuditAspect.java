package com.demo.app.infrastructure.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            Long actorId = null;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                Object principal = authentication.getPrincipal();
                try {
                    Method getIdMethod = principal.getClass().getMethod("getId");
                    Object id = getIdMethod.invoke(principal);
                    if (id instanceof Long) {
                        actorId = (Long) id;
                    }
                } catch (NoSuchMethodException ignored) {
                }
            }

            String ipAddress = null;
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                ipAddress = request.getRemoteAddr();
            }

            Long entityId = extractEntityId(result, joinPoint.getArgs());

            auditService.log(
                    audited.entityType(),
                    entityId,
                    audited.action(),
                    actorId,
                    null,
                    result,
                    ipAddress
            );
        } catch (Exception ignored) {
            // Do not fail the original operation due to audit logging failure
        }

        return result;
    }

    private Long extractEntityId(Object result, Object[] args) {
        if (result != null) {
            try {
                Method getIdMethod = result.getClass().getMethod("getId");
                Object id = getIdMethod.invoke(result);
                if (id instanceof Long) {
                    return (Long) id;
                }
            } catch (Exception ignored) {
            }
        }

        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }

        return null;
    }
}
