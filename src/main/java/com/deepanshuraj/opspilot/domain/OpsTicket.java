package com.deepanshuraj.opspilot.domain;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="ops_tickets") public class OpsTicket { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; private String orderId; @Column(length=240) private String summary; private String priority; private Instant createdAt; protected OpsTicket(){} public OpsTicket(String o,String s,String p,Instant c){orderId=o;summary=s;priority=p;createdAt=c;} public Long getId(){return id;} }
