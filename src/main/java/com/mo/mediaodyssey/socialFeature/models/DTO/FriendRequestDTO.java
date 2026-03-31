package com.mo.mediaodyssey.socialFeature.models.DTO;

public class FriendRequestDTO {

    private Long requestId;
    private Long senderId;
    private String senderEmail;

    public FriendRequestDTO(Long requestId, Long senderId, String senderEmail) {
        this.requestId = requestId;
        this.senderId = senderId;
        this.senderEmail = senderEmail;
    }

    public Long getRequestId() { return requestId; }
    public Long getSenderId() { return senderId; }
    public String getSenderEmail() { return senderEmail; }
}