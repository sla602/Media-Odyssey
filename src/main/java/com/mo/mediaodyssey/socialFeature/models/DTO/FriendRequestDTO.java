package com.mo.mediaodyssey.socialFeature.models.DTO;

public class FriendRequestDTO {

    private Long requestId;
    private Long otherUserId;
    private String otherUserEmail;

    public FriendRequestDTO(Long requestId, Long otherUserId, String otherUserEmail) {
        this.requestId = requestId;
        this.otherUserId = otherUserId;
        this.otherUserEmail = otherUserEmail;
    }

    public Long getRequestId() { return requestId; }
    public Long getOtherUserId() { return otherUserId; }
    public String getOtherUserEmail() { return otherUserEmail; }
}