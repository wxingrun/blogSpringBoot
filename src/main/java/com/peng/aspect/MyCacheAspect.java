package com.peng.aspect;


import com.peng.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


@Slf4j
@Aspect
@Component
public class MyCacheAspect {
    @Autowired
    private RedisUtil redisUtil;
    
    @Autowired
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @org.aspectj.lang.annotation.After("execution(public * com.peng.service.ICacheService.clearCommentCache(..)) && args(blId)")
    public void clearCommentCache(Long blId) {
        java.util.Set<String> keys1 = redisTemplate.keys("com.peng.service.Impl.CacheServiceImpl.getIndexPage*");
        if (keys1 != null && !keys1.isEmpty()) {
            redisTemplate.delete(keys1);
        }
        redisUtil.del("com.peng.service.Impl.CacheServiceImpl.getCommentNum");
        java.util.Set<String> keys2 = redisTemplate.keys("com.peng.service.Impl.CacheServiceImpl.getPageByType*");
        if (keys2 != null && !keys2.isEmpty()) {
            redisTemplate.delete(keys2);
        }
        java.util.Set<String> keys3 = redisTemplate.keys("com.peng.service.Impl.CacheServiceImpl.getPageByTag*");
        if (keys3 != null && !keys3.isEmpty()) {
            redisTemplate.delete(keys3);
        }
        java.util.Set<String> keys4 = redisTemplate.keys("*-" + blId + "*");
        if (keys4 != null && !keys4.isEmpty()) {
            redisTemplate.delete(keys4);
        }
    }

    private String createCacheKey(ProceedingJoinPoint jp) {
        Signature signature = jp.getSignature();
        String methodName = signature.getName();
        String className = signature.getDeclaringTypeName();
        StringBuffer sbKey = new StringBuffer();
        sbKey.append(className);
        sbKey.append(".");
        sbKey.append(methodName);
        Object[] args = jp.getArgs();//方法参数值
        for (Object object : args) {
            sbKey.append("-");
            sbKey.append(object);
        }
        return sbKey.toString();
    }


    //@Around("@annotation(myCache)")
    @Order(1)
    @Around("execution(public * com.peng.service.Impl..*(..)) && @annotation(myCache)")
    public Object around(ProceedingJoinPoint jp, MyCache myCache) {
//        long startTime = System.currentTimeMillis();
        //生成Redis中的key
        String key = createCacheKey(jp);
        //如果有缓存直接返回，没有正常执行并写入缓存
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
        } finally {
//            log.info("{}  方法执行时间： {}",jp.getSignature().getName(),System.currentTimeMillis()-startTime);
        }
    }

}
