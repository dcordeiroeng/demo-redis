package com.redis.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.redis.demo"])
class DemoApplication


fun main(args: Array<String>) {
	runApplication<DemoApplication>(*args)
}
