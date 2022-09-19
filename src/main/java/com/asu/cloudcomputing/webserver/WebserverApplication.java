package com.asu.cloudcomputing.webserver;

import com.asu.cloudcomputing.service.ServerHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;


@SpringBootApplication
@RestController
@EnableScheduling
public class WebserverApplication {

	static ServerHandler handler;

	public static void main(String[] args) {
		handler = new ServerHandler();
		SpringApplication.run(WebserverApplication.class, args);
	}

//	@GetMapping("/getImage")
//	public String getImage() {
//		String queueURL = props.getProperty("amazon.sqs.request-queue");
//		return queueURL;
//	}

	@PostMapping("/classifyImage")
	public String classifyImage(@RequestParam(value = "myfile") MultipartFile multipartFile) {

		String messageBody = "";
		try {
			messageBody = Base64.getEncoder().encodeToString(multipartFile.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
			return "An issue has occurred while trying to encode Image.";
		}

		String requestId = handler.publishImageToSQSQueue(messageBody);

		//TODO invoke a app server(Scaling).


		return handler.getClassifiedImageResult(requestId);
	}

	@Scheduled(fixedRate = 5000)
	public void callLoadBalancer() {
		handler.loadBalancing();
	}

	@Scheduled(fixedRate = 5000)
	public void processResponseQueue() {
		handler.processResponseSQS();
	}

}
