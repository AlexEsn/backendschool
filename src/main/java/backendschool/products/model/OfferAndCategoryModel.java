package backendschool.products.model;

import javax.persistence.*;

@Entity
@Table
public class OfferAndCategoryModel {
    public OfferAndCategoryModel() {

    }
    public OfferAndCategoryModel(OfferAndCategoryModel product) {
        this.setId(product.getId());
        this.setName(product.getName());
        this.setType(product.getType());
        this.setPrice(product.getPrice());
        this.setDate(product.getDate());
        this.setParentId(product.getParentId());
    }

    public enum Type {
        CATEGORY, OFFER
    }

    @Id
    private String id;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "type", nullable = false)
    private Type type;
    @Column(name = "price")
    private Long price;
    private String date;
    @Column(name = "parent")

    private String parentId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String time) {
        this.date = time;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
