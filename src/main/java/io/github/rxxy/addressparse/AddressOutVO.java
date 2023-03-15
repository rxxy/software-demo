package io.github.rxxy.addressparse;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class AddressOutVO implements Serializable {


    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 区县
     */
    private String county;

    /**
     * 乡镇
     */
    // private String town;

    /**
     * 详细地址
     */
    private String detail;

    /**
     * 手机号码
     */
    private String phone;

    /**
     * 收货人姓名
     */
    private String receivingName;

    /**
     * 邮编
     */
    // private String postCode;

}
