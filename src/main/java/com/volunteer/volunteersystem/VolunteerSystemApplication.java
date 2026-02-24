package com.volunteer.volunteersystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.volunteer.volunteersystem.mapper")
public class VolunteerSystemApplication {
	public static void main(String[] args) {
		SpringApplication.run(VolunteerSystemApplication.class, args);
		System.out.println("===================================");
		System.out.println("志愿者管理系统启动成功！");
		System.out.println("访问地址: http://localhost:8080/login.html");
		System.out.println("===================================");
	}
}