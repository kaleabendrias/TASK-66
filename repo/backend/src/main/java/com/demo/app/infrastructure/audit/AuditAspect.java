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

import com.demo.app.persistence.repository.UserRepository;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;
    private final UserRepository userRepository;

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            Long actorId = null;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                String username = authentication.getName();
                actorId = userRepository.findByUsername(username)
                        .map(u -> u.getId())
                        .orElse(null);
            }

            String ipAddress = null;
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                ipAddress = request.getRemoteAddr();
            }

            Long entityId = extractEntityId(result, joinPoint.getArgs());

            // Extract the response body for logging, not the full ResponseEntity
            Object logValue = result;
            if (logValue instanceof org.springframework.http.ResponseEntity<?> re) {
                logValue = re.getBody();
            }

            auditService.log(
                    audited.entityType(),
                    entityId,
                    audited.action(),
                    actorId,
                    null,
                    logValue,
                    ipAddress
            );
        } catch (Exception e) {
            // Log audit failures but don't fail the original operation
            org.slf4j.LoggerFactory.getLogger(AuditAspect.class).warn("Audit logging failed: {}", e.getMessage());
        }

        return result;
    }

    private Long extractEntityId(Object result, Object[] args) {
        // Unwrap ResponseEntity
        Object body = result;
        if (body != null && body instanceof org.springframework.http.ResponseEntity<?> re) {
            body = re.getBody();
        }

        if (body != null) {
            try {
                Method getIdMethod = body.getClass().getMethod("id");  // records use id() not getId()
                Object id = getIdMethod.invoke(body);
                if (id instanceof Long) return (Long) id;
                if (id instanceof Number) return ((Number) id).longValue();
            } catch (NoSuchMethodException e) {
                try {
                    Method getIdMethod = body.getClass().getMethod("getId");
                    Object id = getIdMethod.invoke(body);
                    if (id instanceof Long) return (Long) id;
                    if (id instanceof Number) return ((Number) id).longValue();
                } catch (Exception ignored) {}
            } catch (Exception ignored) {}
        }

        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }

        return null;
    }
}
