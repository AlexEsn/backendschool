package backendschool.products.repository;

import backendschool.products.model.OfferAndCategoryModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<OfferAndCategoryModel, String> { }