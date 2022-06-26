package backendschool.products.entity;

import backendschool.products.model.OfferAndCategoryModel;

import java.util.ArrayList;
import java.util.List;

public class OfferAndCategoryEntity extends OfferAndCategoryModel {

    private List<OfferAndCategoryEntity> children = null;


    public void setChildren(List<OfferAndCategoryEntity> list_children) { this.children = list_children; }

    public List<OfferAndCategoryEntity> getChildren() { return children; }

    public void addChildren(OfferAndCategoryModel product) {
        if (product != null) {
            if (this.children == null) this.children = new ArrayList<>();
            this.children.add(new OfferAndCategoryEntity(product));
        }
    }

    public OfferAndCategoryEntity(OfferAndCategoryModel product) {
        this.setId(product.getId());
        this.setName(product.getName());
        this.setType(product.getType());
        this.setPrice(product.getPrice());
        this.setDate(product.getDate());
        this.setParentId(product.getParentId());
        this.setChildren(null);
    }

}
