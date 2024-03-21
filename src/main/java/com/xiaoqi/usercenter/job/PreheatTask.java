package com.xiaoqi.usercenter.job;


import com.xiaoqi.usercenter.service.UserService;
import com.xiaoqi.usercenter.contant.Preheat;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class PreheatTask {
    @Resource
    private UserService userService;
    @Resource
    private RedissonClient redissonClient;


    //要预热的用户id

    List<Long> listId = Arrays.asList(2L);

    @SneakyThrows
        @Scheduled(cron = "0 29 21 18 8 ?   ")
    public void preheatTask() {
        //1获取锁
        RLock lock = redissonClient.getLock(Preheat.PREHEAT_LOCK);
        try {
            if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                userService.preMessage(listId);
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUser err", e);
        } finally {
            //释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }

        }


    }

}
