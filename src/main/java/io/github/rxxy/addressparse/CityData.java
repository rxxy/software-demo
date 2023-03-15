package io.github.rxxy.addressparse;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CityData {

    private final static String dataUrl = "city/pca-code.json";

    /**
     * 直辖市
     */
    public final static List<String> MUNICIPALITY = Arrays.asList("北京市", "天津市", "上海市", "重庆市");
    private static final List<City> data;

    static {
        URL url = ResourceUtil.getResource(dataUrl);
        try {
            String json = IoUtil.readUtf8(url.openStream());
            data = JSONUtil.toBean(json, new TypeReference<List<City>>() {
            }, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<City> getData() {
        return data;
    }

    /**
     * 获取省
     */
    public List<City> getProvinces() {
        return data;
    }

    /**
     * 获取市
     */
    public List<City> getCities() {
        // 直辖市特殊处理
        List<City> citiesOfProvinces = data.stream().filter(city -> !MUNICIPALITY.contains(city.getName()))
                .flatMap(city -> city.getChildren().stream()).collect(Collectors.toList());
        List<City> municipality = data.stream().filter(city -> MUNICIPALITY.contains(city.getName())).collect(Collectors.toList());
        ArrayList<City> cities = new ArrayList<>(citiesOfProvinces);
        cities.addAll(municipality);
        return cities;
    }

    /**
     * 获取区/县
     */
    public List<City> getCounties() {
        return data.stream().flatMap(city -> city.getChildren().stream())
                .flatMap(city -> city.getChildren().stream())
                .collect(Collectors.toList());
    }

    /**
     * 推断省份
     */
    public City inferProvince(String text) {
        List<ScoreCity> provincesPossible = new ArrayList<>();
        for (City province : getProvinces()) {
            int score = calcScore(text, province.getName());
            if (score > 0){
                provincesPossible.add(new ScoreCity(score, province));
            }
        }
        provincesPossible.sort(Comparator.comparing(ScoreCity::getScore).reversed());
        return provincesPossible.size()>0?provincesPossible.get(0).getCity():null;
    }


    /**
     * 推断城市
     */
    public List<City> inferCity(String text) {
        List<ScoreCity> citiesPossible = new ArrayList<>();
        for (City city : getCities()) {
            int score = calcScore(text, city.getName());
            if (score > 0) {
                citiesPossible.add(new ScoreCity(score, city));
            }
        }
        citiesPossible.sort(Comparator.comparing(ScoreCity::getScore).reversed());
        return citiesPossible.stream().map(ScoreCity::getCity).collect(Collectors.toList());
    }

    /**
     * 推断区县
     */
    public List<City> inferCounty(String text) {
        List<ScoreCity> countiesPossible = new ArrayList<>();
        for (City county : getCounties()) {
            int score = calcScore(text, county.getName());
            if (score > 0) {
                countiesPossible.add(new ScoreCity(score, county));
            }
        }
        countiesPossible.sort(Comparator.comparing(ScoreCity::getScore).reversed());
        return countiesPossible.stream().map(ScoreCity::getCity).collect(Collectors.toList());
    }

    /**
     * 推断详细地址
     * @param text 文本
     */
    public String inferDetail(String text, String lastItem) {
        String chinese = "\u2E80-\u2EFF\u2F00-\u2FDF\u31C0-\u31EF\u3400-\u4DBF\u4E00-\u9FFF\uF900-\uFAFF\uD840\uDC00-\uD869\uDEDF\uD869\uDF00-\uD86D\uDF3F\uD86D\uDF40-\uD86E\uDC1F\uD86E\uDC20-\uD873\uDEAF\uD87E\uDC00-\uD87E\uDE1F";
        Pattern pattern = Pattern.compile(lastItem + "([" + chinese + "\\w()（）]+)");
        Matcher matcher = pattern.matcher(text);
        String detail = null;
        if (matcher.find()) {
            detail = matcher.group(1);
        }else {
            if (lastItem.length() > 2) {
                lastItem = lastItem + "?";
                Matcher matcher1 = Pattern.compile(lastItem + "([" + chinese + "\\w()（）]+)").matcher(text);
                if (matcher1.find()) {
                    detail = matcher1.group(1);
                }
            }
        }
        return detail;
    }

    // 特殊分隔字符
    private String specialChar = "[~!@#\\$\\^&*=':;',\\\\.<>/?~！@#￥……&*‘；：”“’。，、？\\-\n \t]";
    /**
     * 推断姓名
     * @param text 文本
     */
    public String inferReceivingName(String text) {
        String[] split = text.split(specialChar);
        if (split.length == 1){
            if (calcScoreForNickName(split[0]) > 0) {
                return split[0];
            }
        }
        List<String> names = Arrays.stream(split).filter(n -> calcScoreForNickName(n) >= 0)
                .sorted(Comparator.comparing(this::calcScoreForNickName).reversed()).collect(Collectors.toList());
        if (names.size() > 0){
            return names.get(0);
        }
        return null;
    }

    private int calcScore(String text, String city) {
        if (text.contains(city)) {
            return 10;
        }
        if (city.length() > 1){
            String substring = city.substring(0, city.length() - 1);
            if (text.contains(substring)) {
                return 9;
            }
        }
        // 还可接入其他推断方法
        return 0;
    }


    // 地名，用于判断一个字符串是一个地址的可能程度
    List<String> placeList = Arrays.asList(
            "广场", "大街", "对面", "..路"
    );

    /**
     * 给一个收货人姓名打分
     */
    private int calcScoreForNickName(String nickName) {
        int score = 0;
        if (Validator.isChineseName(nickName) && nickName.length() < 4) {
            score = 100;
            return score;
        }
        Pattern pattern = Pattern.compile("[省市县区乡镇村]");
        Matcher matcher = pattern.matcher(nickName);
        if (matcher.find()) {
            score -= 5;
        }
        if (StrUtil.isNumeric(nickName)) {
            score -= 1;
        }
        if (nickName.length() > 7) {
            score -= 1;
        }
        return score;
    }

    /**
     * 有相似评分的城市
     */
    @Getter
    @Setter
    @AllArgsConstructor
    static class ScoreCity implements Comparable<ScoreCity>{
        private int score;
        private City city;

        @Override
        public int compareTo(ScoreCity o) {
            return o.getScore() - this.score;
        }
    }

}
