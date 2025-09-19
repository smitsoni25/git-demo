package com.azguards.webhook.handler.impl;

import com.azguards.internal.model.webhook.AudioDto;
import com.azguards.internal.model.webhook.ButtonDto;
import com.azguards.internal.model.webhook.ContactDto;
import com.azguards.internal.model.webhook.ContextMetaDataDto;
import com.azguards.internal.model.webhook.DocumentDto;
import com.azguards.internal.model.webhook.ErrorDto;
import com.azguards.internal.model.webhook.ImageDto;
import com.azguards.internal.model.webhook.InteractiveDto;
import com.azguards.internal.model.webhook.LocationDto;
import com.azguards.internal.model.webhook.MessageDto;
import com.azguards.internal.model.webhook.MessagesDetailsDto;
import com.azguards.internal.model.webhook.OrderDto;
import com.azguards.internal.model.webhook.ReferralDto;
import com.azguards.internal.model.webhook.StickerDto;
import com.azguards.internal.model.webhook.TypeEnum;
import com.azguards.internal.model.webhook.VideoDto;
import com.azguards.internal.model.webhook.WebhookEventDto;
import com.azguards.webhook.dto.PayloadDto;
import com.azguards.webhook.handler.EventHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("messages")
@Slf4j
public class MessagesHandler implements EventHandler {

    private final ObjectMapper objectMapper;

    public MessagesHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(PayloadDto payload) {
        log.info("Processing messages update: {}", payload);
        try {
            // Map PayloadDto to WebhookEventDto
            WebhookEventDto webhookEvent = mapToWebhookEventDto(payload);
            if (webhookEvent == null || webhookEvent.getType() != TypeEnum.MESSAGES) {
                log.error("Invalid payload or type is not MESSAGES: {}", payload);
                return;
            }

            MessagesDetailsDto messagesDetails = webhookEvent.getMessagesDetails();
            if (messagesDetails == null || messagesDetails.getMessages() == null) {
                log.warn("No messages details found in payload: {}", payload);
                return;
            }

            // Process each message
            for (MessageDto message : messagesDetails.getMessages()) {
                if (message == null || message.getType() == null) {
                    log.warn("Skipping null or invalid message: {}", message);
                    continue;
                }
                processMessage(message, messagesDetails.getBusinessPhoneNumberId());
            }
        } catch (Exception e) {
            log.error("Error processing messages payload: {}", payload, e);
        }
    }

    private WebhookEventDto mapToWebhookEventDto(PayloadDto payload) {
        try {
            // Assuming PayloadDto.getPayload() contains the JSON string
            String jsonPayload = payload.getPayload();
            if (jsonPayload == null) {
                log.error("Payload content is null");
                return null;
            }
            // Deserialize JSON to WebhookEventDto
            WebhookEventDto webhookEvent = objectMapper.readValue(jsonPayload, WebhookEventDto.class);
            // Set timestamp from PayloadDto if available
            if (payload.getTimestamp() != null) {
                webhookEvent.setWebhookTriggeredTimestamp(String.valueOf(payload.getTimestamp()));
            }
            return webhookEvent;
        } catch (Exception e) {
            log.error("Failed to map PayloadDto to WebhookEventDto: {}", payload, e);
            return null;
        }
    }

    private void processMessage(MessageDto message, String businessPhoneNumberId) {
        log.info("Processing message ID: {} with type: {} for business phone: {}", 
                 message.getMessageId(), message.getType(), businessPhoneNumberId);
        
        switch (message.getType()) {
            case REACTION:
                log.info("Reaction message - Emoji: {}, User: {}", 
                         message.getEmoji(), message.getUserPhoneNumber());
                break;
            case TEXT:
                log.info("Text message - Content: {}, User: {}", 
                         message.getText(), message.getUserPhoneNumber());
                break;
            case AUDIO:
                AudioDto audio = message.getAudio();
                log.info("Audio message - ID: {}, Caption: {}, MimeType: {}, IsVoice: {}", 
                         audio != null ? audio.getId() : null, 
                         audio != null ? audio.getCaption() : null, 
                         audio != null ? audio.getMimeType() : null, 
                         audio != null ? audio.getIsVoiceRecording() : null);
                break;
            case IMAGE:
                ImageDto image = message.getImage();
                log.info("Image message - ID: {}, Caption: {}, MimeType: {}, SHA256: {}", 
                         image != null ? image.getId() : null, 
                         image != null ? image.getCaption() : null, 
                         image != null ? image.getMimeType() : null, 
                         image != null ? image.getSha256() : null);
                break;
            case DOCUMENT:
                DocumentDto document = message.getDocument();
                log.info("Document message - ID: {}, Caption: {}, MimeType: {}, SHA256: {}, FileName: {}", 
                         document != null ? document.getId() : null, 
                         document != null ? document.getCaption() : null, 
                         document != null ? document.getMimeType() : null, 
                         document != null ? document.getSha256() : null, 
                         document != null ? document.getFileName() : null);
                break;
            case VIDEO:
                VideoDto video = message.getVideo();
                log.info("Video message - ID: {}, Caption: {}, MimeType: {}, SHA256: {}", 
                         video != null ? video.getId() : null, 
                         video != null ? video.getCaption() : null, 
                         video != null ? video.getMimeType() : null, 
                         video != null ? video.getSha256() : null);
                break;
            case STICKER:
                StickerDto sticker = message.getSticker();
                log.info("Sticker message - ID: {}, MimeType: {}, SHA256: {}, IsAnimated: {}", 
                         sticker != null ? sticker.getId() : null, 
                         sticker != null ? sticker.getMimeType() : null, 
                         sticker != null ? sticker.getSha256() : null, 
                         sticker != null ? sticker.getIsAnimated() : null);
                break;
            case UNSUPPORTED:
                List<ErrorDto> errors = message.getErrors();
                log.info("Unsupported message - Errors: {}", errors);
                break;
            case ORDER:
                OrderDto order = message.getOrder();
                log.info("Order message - CatalogID: {}, Products: {}", 
                         order != null ? order.getCatalogId() : null, 
                         order != null ? order.getProducts() : null);
                break;
            case LOCATION:
                LocationDto location = message.getLocation();
                log.info("Location message - Latitude: {}, Longitude: {}, Address: {}", 
                         location != null ? location.getLatitude() : null, 
                         location != null ? location.getLongitude() : null, 
                         location != null ? location.getAddress() : null);
                break;
            case BUTTON:
                ButtonDto button = message.getButton();
                log.info("Button message - Payload: {}, Text: {}", 
                         button != null ? button.getPayload() : null, 
                         button != null ? button.getText() : null);
                break;
            case INTERACTIVE:
                InteractiveDto interactive = message.getInteractive();
                log.info("Interactive message - Type: {}, ListReply: {}, ButtonReply: {}", 
                         interactive != null ? interactive.getType() : null, 
                         interactive != null ? interactive.getListReply() : null, 
                         interactive != null ? interactive.getButtonReply() : null);
                break;
            case CONTACTS:
                List<ContactDto> contacts = message.getContact();
                log.info("Contacts message - Contacts: {}", contacts);
                break;
            default:
                log.warn("Unknown message type: {}", message.getType());
                break;
        }

}