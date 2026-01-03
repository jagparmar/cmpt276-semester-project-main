package com.example.nostalgianest;

import java.util.HashMap;
import java.util.Map;

public class albumObject {

    private String uid; // Unique identifier for the album
    private String passcode; // Security code for the album
    private String description; // Description of the album
    private String albumName; // Name of the album
    private String owner; // UID of the user who created the album
    private Map<String, Boolean> members; // Map of member UIDs (Boolean is used for presence check)

    // No-argument constructor required for Firebase
    public albumObject() {
        this.members = new HashMap<>(); // Initialize members map to prevent null issues
    }

    // Constructor with parameters
    public albumObject(String uid, String passcode, String description, String albumName) {
        this.uid = uid;
        this.passcode = passcode;
        this.description = description;
        this.albumName = albumName;
        this.members = new HashMap<>(); // Initialize members map
    }

    // Getter and Setter methods
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getPasscode() {
        return passcode;
    }

    public void setPasscode(String passcode) {
        this.passcode = passcode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Map<String, Boolean> getMembers() {
        return members;
    }

    public void setMembers(Map<String, Boolean> members) {
        this.members = members;
    }

    // Add a member to the album
    public void addMember(String memberUid) {
        if (memberUid != null && !memberUid.isEmpty()) {
            if (this.members == null) {
                this.members = new HashMap<>();
            }
            this.members.put(memberUid, true);
        }
    }

    // Remove a member from the album
    public void removeMember(String memberUid) {
        if (this.members != null && memberUid != null) {
            this.members.remove(memberUid);
        }
    }

    // Convert object into a map to save to Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("passcode", passcode);
        result.put("description", description);
        result.put("albumName", albumName);
        result.put("owner", owner);
        result.put("members", members != null ? members : new HashMap<>());
        return result;
    }

    // Optional: Override toString for debugging
    @Override
    public String toString() {
        return "albumObject{" +
                "uid='" + uid + '\'' +
                ", passcode='" + passcode + '\'' +
                ", description='" + description + '\'' +
                ", albumName='" + albumName + '\'' +
                ", owner='" + owner + '\'' +
                ", members=" + members +
                '}';
    }
}
