package demo.model;

import java.io.Serializable;
import java.util.Date;

/**
 * @author noear 2021/10/14 created
 */
public class UserDo implements Serializable {
    public long id;
    public String name;
    public double create_lng;
    public double create_lat;
    public Date create_time;

    @Override
    public String toString() {
        return "UserDo{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", create_lng=" + create_lng +
                ", create_lat=" + create_lat +
                ", create_time=" + create_time +
                '}';
    }
}
