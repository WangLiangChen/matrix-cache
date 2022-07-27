package wang.liangchen.matrix.cache.sdk.spring.boot.starter.test;

import java.io.Serializable;

/**
 * @author Liangchen.Wang 2022-07-27 16:48
 */
public class Staff implements Serializable {
    private Long staffId;
    private String staffName;

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }
}
