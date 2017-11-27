package com.company.project.core.service.dubbo;


/**
 * 必须保证此接口所在的包与provider一致
 * 城市业务 Dubbo 服务层
 *
 * Created by bysocket on 28/02/2017.
 */
public interface CityDubboService {

    /**
     * 根据城市名称，查询城市信息
     * @param cityName
     */
    City findCityByName(String cityName);
}
