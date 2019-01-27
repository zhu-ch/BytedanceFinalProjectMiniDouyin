package myself.zch.minidouyin.JavaBeans;

import java.util.Date;

public class Entry {
    public final long id;
    private String userName;
    private String studentId;
    private String imageUrl;
    private String videoUrl;
    private Date date;

    public Entry(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    public Date getDate() {
        return date;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }


    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
