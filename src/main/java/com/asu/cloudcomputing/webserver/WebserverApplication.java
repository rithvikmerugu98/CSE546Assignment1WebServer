package com.asu.cloudcomputing.webserver;

import com.asu.cloudcomputing.awsclients.AWSClientProvider;
import com.asu.cloudcomputing.awsclients.SQSAWSClient;
import com.asu.cloudcomputing.service.ServerHandler;
import com.asu.cloudcomputing.utility.PropertiesReader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.io.*;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SpringBootApplication
@RestController
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

		String requestId = handler.publishImageToSQSQueue(messageBody, multipartFile.getName());

		//TODO invoke a app server(Scaling).


		return handler.getClassifiedImageResult(requestId);
	}

}
