package com.peng.aspect;


import com.peng.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;


@Slf4j
@Aspect
@Component
public class MyCacheAspect {
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private String createCacheKey(ProceedingJoinPoint jp) {
        Signature signature = jp.getSignature();
        return createCacheKey(signature.getDeclaringTypeName(), signature.getName(), jp.getArgs());
    }

    public String createCacheKey(Class<?> targetClass, String methodName, Object... args) {
        return createCacheKey(targetClass.getName(), methodName, args);
    }

    public void deleteCache(Class<?> targetClass, String methodName, Object... args) {
        redisUtil.del(createCacheKey(targetClass, methodName, args));
    }

    public void deleteCacheByPrefix(Class<?> targetClass, String methodName) {
        String prefix = targetClass.getName() + "." + methodName;
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private String createCacheKey(String className, String methodName, Object... args) {
        StringBuilder sbKey = new StringBuilder();
        sbKey.append(className);
        sbKey.append(".");
        sbKey.append(methodName);
        if (args != null) {
            for (Object object : args) {
                sbKey.append("-");
                sbKey.append(object);
            }
        }
        return sbKey.toString();
    }

    @Order(1)
    @Around("execution(public * com.peng.service.Impl..*(..)) && @annotation(myCache)")
    public Object around(ProceedingJoinPoint jp, MyCache myCache) {
        String key = createCacheKey(jp);
        try {
            if (redisUtil.hasKey(key)) {
                return redisUtil.get(key);
            } else {
                Object result = jp.proceed(jp.getArgs());
                redisUtil.set(key, result, myCache.overTime());
                return result;
            }
        } catch (Throwable t) {
            log.error(t.toString());
            return null;
        }
    }

}
