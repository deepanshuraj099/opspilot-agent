package com.deepanshuraj.opspilot.domain;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="customer_orders") public class CustomerOrder {
 @Id @Column(name="order_id",length=32) private String orderId; private String customer; private String product; private String region; private String status; private Instant createdAt;
 protected CustomerOrder(){} public CustomerOrder(String id,String c,String p,String r,String s,Instant at){orderId=id;customer=c;product=p;region=r;status=s;createdAt=at;}
 public String getOrderId(){return orderId;} public String getCustomer(){return customer;} public String getProduct(){return product;} public String getRegion(){return region;} public String getStatus(){return status;} public Instant getCreatedAt(){return createdAt;}
}
