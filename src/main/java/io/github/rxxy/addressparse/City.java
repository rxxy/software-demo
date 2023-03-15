package io.github.rxxy.addressparse;

import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class City {

    private String code;

    private String name;

    private List<City> children;

    private City parent;

    public void setChildren(List<City> children) {
        for (City child : children) {
            child.setParent(this);
        }
        this.children = children;
    }

    /**
     * 如果是直辖市，则返回直辖市的名称
     * @return
     */
    public String getGoodName() {
        if (CityData.MUNICIPALITY.contains(getParent().getName())) {
            return getParent().name;
        }
        return name;
    }

    /**
     * 判断给定的城市是否是当前城市的子级
     * 当前city为广东省，传入深圳市，则返回true
     * 当前city为广东省，传入南山区，则返回true
     */
    public boolean isChild(City city) {
        if (children == null) {
            return false;
        }
        // 直接后代
        for (City child : children) {
            if (child.equals(city)){
                return true;
            }
        }
        // 判断隔代
        for (City child : children) {
            if (child.isChild(city)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof City)) {
            return false;
        }
        if (this == obj){
            return true;
        }
        City c = (City)obj;
        return c.getCode().equals(this.getCode());
    }
}
