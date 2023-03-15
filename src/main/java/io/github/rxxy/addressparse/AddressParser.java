package io.github.rxxy.addressparse;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 地址解析器
 */
public class AddressParser {

    private final Pattern PHONE_PATTERN = Pattern.compile("1[3456789]\\d{9}");

    public static void main(String[] args) {
        AddressParser parser = new AddressParser();
        // 省宝安区盘山路28号幸福茶庄,张三，17777777777
        System.out.println(parser.parse("内蒙古自治区呼和浩特市赛罕 213r 李莲英"));
        System.out.println(parser.parse("广东深圳东大街11号 213r 李莲英"));
        System.out.println(parser.parse("广东深圳东大街11号 小苏不爱213r 小苏不爱吃肉肉"));
        System.out.println(parser.parse("北京市东城区前门东大街11号(天安门广场人民英雄纪念碑南面)毛主席纪念堂 若兮相言"));
        System.out.println(parser.parse("北京市南山区盘山路28号幸福茶庄@张三】13956232345"));
        System.out.println(parser.parse("深圳市宝安区盘山路28号幸福茶庄,张三，13956232345"));
        System.out.println(parser.parse("广东省宝安区盘山路28号幸福茶庄  张三，13956232345"));
        System.out.println(parser.parse("山西省阳高县盘山路28号幸福茶庄   张三^13956232345"));
        System.out.println(parser.parse("阳高县安区盘山路28号幸福茶庄， 张三 13956232345"));
        System.out.println(parser.parse("天津市宝安区盘山路28号幸福茶庄,张三，17777777777"));
    }

    /**
     * 解析文本中的手机号，如果有多个手机号，输出最后一个
     * @param text
     * @return
     */
    private String parserPhone(String text) {
        List<String> phone = new ArrayList<>();
        Matcher matcher = PHONE_PATTERN.matcher(text);
        while (matcher.find()) {
            phone.add(matcher.group(0));
        }
        return phone.size()>0?phone.get(phone.size()-1):"";
    }


    /**
     * 解析地址
     * @param text 地址文本
     */
    public AddressOutVO parse(String text) {
        CityData data = new CityData();
        AddressOutVO addressOutVO = new AddressOutVO();
        addressOutVO.setPhone(parserPhone(text));

        City province;
        City city = null;
        City county = null;

        province = data.inferProvince(text);
        List<City> cities = data.inferCity(text);
        List<City> counties = data.inferCounty(text);

        // 1.正向缩减
        // 省->市
        if (province != null) {
            // 省缩减市
            city = reduceCity(province, cities);
        }
        // 如果没有缩减成功，那就取分数最高的第一个进行缩减(可能和省市不匹配)
        if (city == null) {
            // 没有省，优先按准确的市来推断
            if (province == null && cities.size()==1) {
                city = cities.get(0);
            }
            // city = cities.size()>0?cities.get(0):null;
        }
        // 市->区/县
        if (city != null){
            county = reduceCounty(city, counties);
        }
        // 如果没有缩减成功
        if (county == null && city == null) {
            if (province != null) {
                for (City c : counties) {
                    if (province.isChild(c)) {
                        county = c;
                    }
                }
            } else {
                // 省市县都为空
                county = counties.get(0);
            }
            // county = counties.size()>0?counties.get(0):null;
        }

        // 2.反向推断 (主要是补null)
        // 补城市
        if (county != null) {
            city = county.getParent();
        }
        //  补省份
        if (city != null){
            province = city.getParent();
        }

        addressOutVO.setProvince(Optional.ofNullable(province).map(City::getName).orElse(null));
        addressOutVO.setCity(Optional.ofNullable(city).map(City::getName).orElse(null));
        addressOutVO.setCounty(Optional.ofNullable(county).map(City::getName).orElse(null));

        String lastItem = null;
        if (county != null) {
            lastItem = county.getName();
        }
        if (lastItem == null && city != null) {
            lastItem = city.getGoodName();
        }
        if (lastItem == null && province != null){
            if (CityData.MUNICIPALITY.contains(province.getName())) {
                lastItem = province.getName();
            }else {
                lastItem = province.getName();
            }
        }
        if (lastItem == null) {
            lastItem = "";
        }

        addressOutVO.setDetail(data.inferDetail(text, lastItem));
        addressOutVO.setReceivingName(data.inferReceivingName(text));
        // address.setPostCode();
        return addressOutVO;
    }

    /**
     * 省缩减市
     * @param province 省
     * @param cities   可能的市
     * @return 缩减后的市，可能为空
     */
    private static City reduceCity(City province, List<City> cities) {
        City city;
        if (CityData.MUNICIPALITY.contains(province.getName())) {
            // 直辖市特殊处理 省这一级中，没有和4个直辖市重复的省，所以一定会有值，否则数据错误
            city = province.getChildren().get(0);
        }else {
            City finalProvince = province;
            Optional<City> first = cities.stream().filter(c -> finalProvince.equals(c.getParent())).findFirst();
            city = first.orElse(null);
        }
        return city;
    }

    /**
     * 通过市来缩减区/县
     * @param city 准确的市信息 不能为空
     * @param counties 可能的区/县
     * @return 缩减后的区/县
     */
    private static City reduceCounty(@NonNull City city, List<City> counties) {
        // 市缩减区/县
        City finalCity = city;
        Optional<City> first = counties.stream().filter(c -> finalCity.equals(c.getParent())).findFirst();
        return first.orElse(null);
    }

}
