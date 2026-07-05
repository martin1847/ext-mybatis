package tech.krpc.ext.it.dto;

/**
 * Result DTO for the native-it mapper. Plain POJO with getters/setters — MyBatis populates it
 * via reflection at runtime. This class is registered for native reflection by the ext-mybatis
 * deployment processor (it walks the mapper method return type List&lt;Widget&gt;); if that
 * registration is ever dropped, native runtime populates nothing and the {@code id}/{@code name}
 * assertions in {@code NativeItMain} fail — that is the point of the DTO round-trip guard.
 */
public class Widget {

    private Integer id;
    private String  name;
    private Integer qty;

    public Widget() {
    }

    public Widget(Integer id, String name, Integer qty) {
        this.id = id;
        this.name = name;
        this.qty = qty;
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

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }

    @Override
    public String toString() {
        return "Widget{id=" + id + ", name='" + name + "', qty=" + qty + '}';
    }
}
