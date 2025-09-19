package com.azguards.webhook.handler.impl;

import com.azguards.common.config.EventProducer;
import com.azguards.wab.model.webhook.Value;
import com.azguards.wab.model.webhook.WebhookPayload;
import com.azguards.webhook.dto.PayloadDto;
import com.azguards.webhook.dto.QualityUpdateDto;
import com.azguards.webhook.dto.TemplateInfoDto;
import com.azguards.webhook.dto.WebhookEventDto;
import com.azguards.webhook.enums.EventType;
import com.azguards.webhook.handler.EventHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component("message_template_quality_update")
@Slf4j
@RequiredArgsConstructor
public class MessageTemplateQualityUpdateHandler implements EventHandler {

	@org.springframework.beans.factory.annotation.Value("${aws.sqs.message.template.quality.update.queue.name}")
	private String queueName;

	private final EventProducer eventProducer;
	private final ObjectMapper objectMapper;

	@Override
	public void handle(PayloadDto payload) {
		try {
			// Deserialize full webhook payload
			WebhookPayload wrapper = objectMapper.readValue(payload.getPayload(), WebhookPayload.class);

			// Process entries and changes, similar to BusinessCapabilityUpdateHandler
			wrapper.getEntry().stream()
					.flatMap(entry -> entry.getChanges().stream().map(change -> buildEventDto(entry, change, payload)))
					.forEach(this::sendToQueue);

		} catch (Exception e) {
			log.error("Failed to process MESSAGE_TEMPLATE_QUALITY_UPDATE webhook", e);
			throw new RuntimeException("Failed to process webhook payload", e);
		}
	}

	private WebhookEventDto buildEventDto(com.azguards.wab.model.webhook.Entry entry,
			com.azguards.wab.model.webhook.Change change, PayloadDto payload) {
		Value value = change.getValue();

		// Build QualityUpdateDto with available fields
		QualityUpdateDto qualityUpdate = QualityUpdateDto.builder()
				.previousQualityScore(value.getPreviousQualityScore()).newQualityScore(value.getNewQualityScore())
				.build();

		// Build TemplateInfoDto with required fields
		TemplateInfoDto templateInfoDto = TemplateInfoDto.builder().templateId(value.getMessageTemplateId())
				.templateName(value.getMessageTemplateName()).templateLanguage(value.getMessageTemplateLanguage())
				.qualityUpdate(qualityUpdate).build();

		// Build WebhookEventDto
		return WebhookEventDto.builder().wabaId(entry.getId())
				.webhookTriggeredTimestamp(Optional.ofNullable(payload.getTimestamp()).map(String::valueOf)
						.orElse(String.valueOf(entry.getTime())))
				.type(EventType.MESSAGE_TEMPLATE_QUALITY_UPDATE).templateInfo(templateInfoDto).build();
	}

	private void sendToQueue(WebhookEventDto eventDto) {

		try {
			String messageBody = objectMapper.writeValueAsString(eventDto);
			eventProducer.sendMessageToQueue(messageBody, queueName);

			log.info(
					"Queued MESSAGE_TEMPLATE_QUALITY_UPDATE -> wabaId={}, templateId={}, previousQualityScore={}, newQualityScore={}",
					eventDto.getWabaId(),
					Optional.ofNullable(eventDto.getTemplateInfo()).map(TemplateInfoDto::getTemplateId).orElse(null),
					Optional.ofNullable(eventDto.getTemplateInfo()).map(TemplateInfoDto::getQualityUpdate)
							.map(QualityUpdateDto::getPreviousQualityScore).orElse(null),
					Optional.ofNullable(eventDto.getTemplateInfo()).map(TemplateInfoDto::getQualityUpdate)
							.map(QualityUpdateDto::getNewQualityScore).orElse(null));
		} catch (Exception e) {
			log.error("Failed to send MESSAGE_TEMPLATE_QUALITY_UPDATE to queue for wabaId={}", eventDto.getWabaId(), e);
			throw new RuntimeException("Failed to queue MESSAGE_TEMPLATE_QUALITY_UPDATE", e);
		}
	}
}