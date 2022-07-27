package wang.liangchen.matrix.cache.sdk.spring.boot.starter.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import wang.liangchen.matrix.cache.sdk.override.EnableMatrixCaching;

/**
 * @author Liangchen.Wang 2022-07-22 12:19
 */
@SpringBootTest
@EnableMatrixCaching
public class CacheTest {
    @Autowired
    private CacheableClass cacheableClass;

    @Test
    public void cache() throws InterruptedException {
        Staff staff=cacheableClass.findStaff();
        System.out.println(staff.getStaffId());
        staff=cacheableClass.findStaff();
        System.out.println(staff.getStaffId());
    }
}
