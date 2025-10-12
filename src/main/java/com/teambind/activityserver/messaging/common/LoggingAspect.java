package com.teambind.activityserver.messaging.common;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LoggingAspect {
	
	/**
	 * @annotation를 통해 @KafkaListener가 붙은 메서드만 타겟으로 함.
	 * 메서드의 첫번째 인자를 메시지(String)로 가정하고 수신/성공/실패 로그를 중앙에서 처리함.
	 * <p>
	 * 기존 코드가 예외를 swallow(정상적으로 잡아서 로그만 남김)하던 동작을 유지하기 위해
	 * 이 Advice도 예외를 잡아서 로그만 남기고 호출자에게 예외를 던지지 않는다.
	 */
	@Around("@annotation(kafkaListener)")
	public Object aroundKafkaListener(ProceedingJoinPoint pjp, KafkaListener kafkaListener) throws Throwable {
		Object[] args = pjp.getArgs();
		String message = (args != null && args.length > 0 && args[0] != null) ? String.valueOf(args[0]) : "<no-message>";
		String methodName = pjp.getSignature().toShortString();
		String topics = (kafkaListener != null) ? Arrays.toString(kafkaListener.topics()) : "[]";
		
		log.info("Received message from topics {}: {}", topics, message);
		try {
			Object result = pjp.proceed();
			log.info("Successfully processed message. method={}, topics={}, messageSummary={}", methodName, topics, summarise(message));
			return result;
		} catch (Throwable t) {
			log.error("Failed to process message. method={}, topics={}, message={}", methodName, topics, message, t);
			// 기존 소비자들은 예외를 잡아 로그만 찍었으므로 동일하게 예외를 전파하지 않고 swallow 한다.
			return null;
		}
	}
	
	private String summarise(String message) {
		if (message == null) return "<null>";
		int max = 200;
		if (message.length() <= max) return message;
		return message.substring(0, max) + "...(truncated)";
	}
}
