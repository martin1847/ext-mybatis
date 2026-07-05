package tech.krpc.mybatis.it;

/** Minimal DTO for the connection-leak regression IT. */
public class Widget {
    private Integer id;
    private String name;

    public Widget() {
    }

    public Widget(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
