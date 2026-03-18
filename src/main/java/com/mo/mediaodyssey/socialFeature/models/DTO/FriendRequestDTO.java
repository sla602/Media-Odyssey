package com.mo.mediaodyssey.socialFeature.models.DTO;

public class FriendRequestDTO {

    private Integer requestId;
    private Long senderId;
    private String senderUsername;

    public FriendRequestDTO(Integer requestId, Long senderId, String senderUsername) {
        this.requestId = requestId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
    }

    public Integer getRequestId() { return requestId; }
    public Long getSenderId() { return senderId; }
    public String getSenderUsername() { return senderUsername; }
}