package com.mo.mediaodyssey.layout.DTO;


public class BoardDTO {

    private Long boardId;
    private String boardName;
    private String boardDescription;
    private String boardType;
    private String ownerUsername;
    private Integer memberCount;
    private String viewerRole;   // role of the requesting user (nullable if not a member)

    public BoardDTO() {}

    public BoardDTO(Long boardId, String boardName, String boardDescription,
                    String boardType, String ownerUsername,
                    Integer memberCount, String viewerRole) {
        this.boardId = boardId;
        this.boardName = boardName;
        this.boardDescription = boardDescription;
        this.boardType = boardType;
        this.ownerUsername = ownerUsername;
        this.memberCount = memberCount;
        this.viewerRole = viewerRole;
    }

    // --- Getters & Setters ---

    public Long getBoardId()                          { return boardId; }
    public void setBoardId(Long boardId)              { this.boardId = boardId; }

    public String getBoardName()                      { return boardName; }
    public void setBoardName(String boardName)        { this.boardName = boardName; }

    public String getBoardDescription()                       { return boardDescription; }
    public void setBoardDescription(String boardDescription)  { this.boardDescription = boardDescription; }

    public String getBoardType()                      { return boardType; }
    public void setBoardType(String boardType)        { this.boardType = boardType; }

    public String getOwnerUsername()                           { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername)          { this.ownerUsername = ownerUsername; }

    public Integer getMemberCount()                           { return memberCount; }
    public void setMemberCount(Integer memberCount)           { this.memberCount = memberCount; }

    public String getViewerRole()                     { return viewerRole; }
    public void setViewerRole(String viewerRole)      { this.viewerRole = viewerRole; }
}