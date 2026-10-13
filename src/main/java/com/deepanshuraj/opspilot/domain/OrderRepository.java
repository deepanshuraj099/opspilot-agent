package com.deepanshuraj.opspilot.domain;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface OrderRepository extends JpaRepository<CustomerOrder,String>{ List<CustomerOrder> findByRegion(String region); List<CustomerOrder> findByStatus(String status); }
