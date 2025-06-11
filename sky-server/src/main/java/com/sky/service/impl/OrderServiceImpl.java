package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private WebSocketServer webSocketServer;

    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.baidu.ak}")
    private String ak;

    private Orders orders;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //处理业务异常（地址空，购物车空)
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //checkOutOfRange(addressBook.getCityName()+ addressBook.getDistrictName() +  addressBook.getDetail());

        Long id = BaseContext.getCurrentId();
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(ShoppingCart.builder()
                .userId(id)
                .build());
        if (shoppingCartList == null || shoppingCartList.size() == 0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //订单表插入一条
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,order);
        order.setOrderTime(LocalDateTime.now());
        order.setPayStatus(Orders.UN_PAID);
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setPhone(addressBook.getPhone());
        order.setConsignee(addressBook.getConsignee());
        order.setUserId(id);
        order.setAddress(addressBook.getDetail());
        this.orders = order;

        orderMapper.insert(order);

        List<OrderDetail> orderDetailList = new ArrayList<>();
        //订单明细表插入多条
        for  (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);

        //清空购物车
        shoppingCartMapper.deleteByUserId(id);

        //封装VO返回
        return OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderTime(order.getOrderTime())
                .orderAmount(order.getAmount())
                .build();
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code","ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        Integer OrderPaidStatus = Orders.PAID;//支付状态，已支付
        Integer OrderStatus = Orders.TO_BE_CONFIRMED;  //订单状态，待接单
        LocalDateTime check_out_time = LocalDateTime.now();//更新支付时间
        orderMapper.updateStatus(OrderStatus, OrderPaidStatus, check_out_time, this.orders.getId());

        Map map = new HashMap();
        map.put("type",1);//1来单提醒，2客户催单
        map.put("orderId", this.orders.getId());
        map.put("content","订单号：" + this.orders.getNumber());

        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);


        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

//        //向商家端推送消息 type orderId content
//        Map map = new HashMap();
//        map.put("type",1);//1来单提醒，2客户催单
//        map.put("orderId", ordersDB.getId());
//        map.put("content","订单号：" + outTradeNo);
//
//        String json = JSON.toJSONString(map);
//        webSocketServer.sendToAllClient(json);
    }

    /**
     * 历史订单查询
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    public PageResult pageQuery(int pageNum, int pageSize, Integer status) {
        PageHelper.startPage(pageNum,pageSize);
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        //查询Order
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> list = getOrderDetails(page);



        //通过Order查询Detail
        return new PageResult(page.getTotal(),list);
    }

    /**
     * 通过Order得到OrderDetail封装到VO
     * @param page
     * @return
     */
    private List<OrderVO> getOrderDetails(Page<Orders> page) {
        List<OrderVO> list = new ArrayList<>();

        if(page != null && page.size() > 0) {
            for (Orders orders : page) {
                Long orderId = orders.getId();

                List<OrderDetail> details = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getDishesStr(details);
                orderVO.setOrderDishes(orderDishes);
                orderVO.setOrderDetailList(details);

                list.add(orderVO);
            }
        }

        return list;
    }


    /**
     * 获取订单详情
     * @param id
     * @return
     */
    public OrderVO getDetails(Long id) {
        //获取订单
        Orders orders = orderMapper.getById(id);
        //获取细节
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders.getId());


        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);
        String orderDishes = getDishesStr(orderDetails);
        orderVO.setOrderDishes(orderDishes);
        orderVO.setOrderDetailList(orderDetails);
        return orderVO;
    }

    /**
     * 获取菜品信息
     * @param orderDetails
     * @return
     */
    private String getDishesStr(List<OrderDetail> orderDetails) {
        StringBuilder dishes = new StringBuilder();
        //遍历菜品形成字符串
        for(OrderDetail orderDetail : orderDetails){
            //格式：宫保鸡丁*3
            dishes.append(orderDetail.getName());
            dishes.append(" * ");
            dishes.append(orderDetail.getNumber());
            dishes.append(", ");
        }


        return dishes.toString();
    }

    /**
     * 取消订单
     * @param id
     */
    public void cancelOrder(Long id) {
        Orders order = orderMapper.getById(id);

        if(order == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //待支付和待接单状态下，用户可直接取消订单
        //-商家已接单状态下，用户取消订单需电话沟通商家
        //- 派送中状态下，用户取消订单需电话沟通商家
        if(order.getStatus() > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders nOrder = new Orders();
        nOrder.setId(id);

        //- 如果在待接单状态下取消订单，需要给用户退款
        if(order.getStatus().equals(Orders.PENDING_PAYMENT)){
            //weChatPayUtil.refund(
            //                    ordersDB.getNumber(), //商户订单号
            //                    ordersDB.getNumber(), //商户退款单号
            //                    new BigDecimal(0.01),//退款金额，单位 元
            //                    new BigDecimal(0.01));//原订单金额
            nOrder.setPayStatus(Orders.REFUND);
        }
        // 更新订单状态、取消原因、取消时间
        //- 取消订单后需要将订单状态修改为“已取消”
        nOrder.setStatus(Orders.CANCELLED);
        nOrder.setCancelReason("用户取消");
        nOrder.setCancelTime(LocalDateTime.now());
        orderMapper.update(nOrder);
    }

    /**
     * 再来一单
     * @param id
     */
    public void oneMoreOrder(Long id) {
        //再来一单就是将原订单中的商品重新加入到购物车中
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);
        List<ShoppingCart> shoppingCartList = new ArrayList<>();

        if(orderDetails != null && orderDetails.size() > 0){
            for(OrderDetail orderDetail : orderDetails){
                ShoppingCart shoppingCart = new ShoppingCart();
                BeanUtils.copyProperties(orderDetail,shoppingCart);
                shoppingCart.setUserId(BaseContext.getCurrentId());
                shoppingCart.setCreateTime(LocalDateTime.now());
                shoppingCartList.add(shoppingCart);
            }
            shoppingCartMapper.insertBatch(shoppingCartList);
        }
    }

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = getOrderDetails(page);

        return new PageResult(page.getTotal(),list);
    }

    /**
     * 统计订单数据
     * @return
     */
    public OrderStatisticsVO statistics() {
        Integer confirmed = orderMapper.countByStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS);
        Integer toBeConfirmed = orderMapper.countByStatus(Orders.TO_BE_CONFIRMED);

        OrderStatisticsVO vo = new OrderStatisticsVO();
        vo.setConfirmed(confirmed);
        vo.setDeliveryInProgress(deliveryInProgress);
        vo.setToBeConfirmed(toBeConfirmed);

        return vo;
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                        .id(ordersConfirmDTO.getId())
                        .status(Orders.CONFIRMED)
                        .build();
        orderMapper.update(orders);
    }

    /**
     * 拒单
     * @param ordersCancelDTO
     */
    public void rejection(OrdersCancelDTO ordersCancelDTO) {

        //- 商家拒单时需要指定拒单原因
        //- 商家拒单时，如果用户已经完成了支付，需要为用户退款只有订单处于“待接单”状态时可以执行拒单操作
        Long id = ordersCancelDTO.getId();
        Orders orders = orderMapper.getById(id);
        //只有订单处于“待接单”状态时可以执行拒单操作
        if(orders == null || !orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //退款
        Integer payStatus = orders.getPayStatus();
//        if(payStatus.equals(Orders.PAID)){
//            String refund = weChatPayUtil.refund(
//                    orders.getNumber(),
//                    orders.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
//            log.info("申请退款：{}", refund);
//        }

        Orders resOrder = Orders
                                .builder()
                                .id(id)
                                .status(Orders.CANCELLED)
                                .rejectionReason(ordersCancelDTO.getCancelReason())
                                .cancelTime(LocalDateTime.now())
                                .build();

        orderMapper.update(resOrder);
    }

    /**
     * 取消订单
     * @param ordersCancelDTO
     */
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        //- 取消订单其实就是将订单状态修改为“已取消”
        //- 商家取消订单时需要指定取消原因
        //- 商家取消订单时，如果用户已经完成了支付，需要为用户退款
        Long id = ordersCancelDTO.getId();
        Orders orders = orderMapper.getById(id);

        Integer payStatus = orders.getStatus();
//        if(payStatus.equals(Orders.PAID)){
//            String refund = weChatPayUtil.refund(
//                    orders.getNumber(),
//                    orders.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
//            log.info("申请退款：{}", refund);
//        }

        // 管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
        Orders resOrder = Orders
                .builder()
                .id(id)
                .status(Orders.CANCELLED)
                .rejectionReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();

        orderMapper.update(resOrder);
    }

    /**
     * 派送订单
     * @param id
     */
    public void delivery(Long id) {
        //- 派送订单其实就是将订单状态修改为“派送中”
        //获取订单
        Orders orders = orderMapper.getById(id);

        //- 只有状态为“待派送”的订单可以执行派送订单操作
        Integer status = orders.getStatus();
        if(!status.equals(Orders.CONFIRMED) || orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders resOrder = Orders
                .builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();

        orderMapper.update(resOrder);
    }

    /**
     * 完成订单
     * @param id
     */
    public void complete(Long id) {
        Orders orders = orderMapper.getById(id);
        //只有配送中的订单可以完成
        if(orders == null || !orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders resOrder = Orders
                .builder()
                .id(orders.getId())
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();

        orderMapper.update(resOrder);
    }

    /**
     * 检查客户的收货地址是否超出配送范围
     * @param address
     */
    private void checkOutOfRange(String address){
        Map map = new HashMap();
        map.put("address",shopAddress);
        map.put("output","json");
        map.put("ak",ak);

        //获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        //店铺经纬度坐标
        String shopLngLat = lat + "," + lng;

        map.put("address",address);
        //获取用户收货地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(userCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("收货地址解析失败");
        }

        //数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        //用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;

        map.put("origin",shopLngLat);
        map.put("destination",userLngLat);
        map.put("steps_info","0");

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if(distance > 5000){
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }
    }
}
