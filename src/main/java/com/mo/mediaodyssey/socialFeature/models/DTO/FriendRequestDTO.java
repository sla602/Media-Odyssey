package com.mo.mediaodyssey.socialFeature.models.DTO;

public class FriendRequestDTO {

    private Integer requestId;
    private Long senderId;
    private String senderEmail;

    public FriendRequestDTO(Integer requestId, Long senderId, String senderEmail) {
        this.requestId = requestId;
        this.senderId = senderId;
        this.senderEmail = senderEmail;
    }

    public Integer getRequestId() { return requestId; }
    public Long getSenderId() { return senderId; }
    public String getSenderEmail() { return senderEmail; }
}