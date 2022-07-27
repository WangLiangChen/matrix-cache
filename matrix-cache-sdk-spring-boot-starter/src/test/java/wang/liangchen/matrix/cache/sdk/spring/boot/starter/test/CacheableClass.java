package wang.liangchen.matrix.cache.sdk.spring.boot.starter.test;

import org.springframework.stereotype.Component;
import wang.liangchen.matrix.cache.sdk.annotation.Cacheable;

import java.util.Random;

/**
 * @author Liangchen.Wang 2022-07-27 16:46
 */
@Component
@Cacheable("Staffs")
public class CacheableClass {

    public Staff findStaff() {
        System.out.println("----------unhit----------");
        Staff staff = new Staff();
        staff.setStaffId(new Random().nextLong());
        staff.setStaffName("name_" + staff.getStaffId());
        return staff;
    }
}
