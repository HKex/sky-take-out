package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 统计营业额数据
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //存每天
        List<LocalDate> dateList = getDateList(begin, end);

        List<Double> turnoverList = new ArrayList<>();

        for(LocalDate d : dateList){
            //开始结束时间为一天最早/最晚的时候
            LocalDateTime beginTime = LocalDateTime.of(d, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(d, LocalTime.MAX);
            //状态值5为已完成
            Double turnover = orderMapper.sumByMap(Map.of("begin",beginTime,
                    "end",endTime,
                    "status", Orders.COMPLETED));
            turnoverList.add(turnover == null ? 0.0 : turnover);
        }


        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }

    /**
     * 获取指定时间区间内的日期列表
     * @param begin
     * @param end
     * @return
     */
    private List<LocalDate> getDateList(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        if(begin.isAfter(end)){
            throw new OrderBusinessException("开始时间不能大于结束时间");
        }
        for(LocalDate d = begin; !d.isAfter(end); d = d.plusDays(1)){
            dateList.add(d);
        }
        return dateList;
    }

    /**
     * 统计用户数据
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getDateList(begin, end);

        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        for(LocalDate d : dateList){
            //开始结束时间为一天最早/最晚的时候
            LocalDateTime beginTime = LocalDateTime.of(d, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(d, LocalTime.MAX);
            Integer totalUser = userMapper.countByMap(Map.of("end", endTime));
            Integer newUser = userMapper.countByMap(Map.of("begin", beginTime, "end", endTime));

            newUserList.add(newUser);
            totalUserList.add(totalUser);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .build();
    }

    /**
     * 统计指定时间区间内的订单和相关数据
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        //存每天
        List<LocalDate> dateList = getDateList(begin, end);

        List<Integer> dailyOrders = new ArrayList<>();
        List<Integer> dailySucOrders = new ArrayList<>();
        Double orderCompletionRate = 0.0;
        Integer totalOrder = 0;
        Integer validOrder = 0;

        for(LocalDate d : dateList){
            //开始结束时间为一天最早/最晚的时候
            LocalDateTime beginTime = LocalDateTime.of(d, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(d, LocalTime.MAX);

            Integer totalOrderCount = orderMapper.countByMap(Map.of("begin", beginTime, "end", endTime));
            Integer validOrderCount = orderMapper.countByMap(Map.of("begin", beginTime, "end", endTime, "status", Orders.COMPLETED));

            dailyOrders.add(totalOrderCount);
            dailySucOrders.add(validOrderCount);

            totalOrder += totalOrderCount;
            validOrder += validOrderCount;
        }

        if (totalOrder != 0)orderCompletionRate =  validOrder.doubleValue() / totalOrder.doubleValue();


        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCountList(StringUtils.join(dailyOrders,","))
                .validOrderCountList(StringUtils.join(dailySucOrders,","))
                .validOrderCount(validOrder)
                .totalOrderCount(totalOrder)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 统计指定时间区间内的销量排名
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end){
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end,LocalTime.MAX);


        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);

        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");
        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO.builder().nameList(nameList).numberList(numberList).build();
    }
}
