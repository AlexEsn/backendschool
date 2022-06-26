package backendschool.products.controller;

import backendschool.products.model.OfferAndCategoryModel;
import backendschool.products.entity.OfferAndCategoryEntity;
import backendschool.products.repository.ProductRepository;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;

import static java.lang.Math.abs;

@RestController
@RequestMapping
public class OfferAndCategoryController {
    private final ProductRepository productRepo;

    @Autowired
    public OfferAndCategoryController(ProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    // 1. import: post
    @PostMapping("/imports")
    public ResponseEntity postProduct(@RequestBody Map<String, Object> param) {

        List<OfferAndCategoryModel> outProducts = new LinkedList<>();

        try {
            List<Object> inProducts = null;
            String updateTime = null;

            for (var p : param.values())
                if (p instanceof List) inProducts = (List<Object>) p;
                else updateTime = p.toString();

            for (var product : inProducts) {

                OfferAndCategoryModel inProduct = new OfferAndCategoryModel();
                Map<String, Object> notParseProduct = (Map<String, Object>) product;

                for (var data : notParseProduct.keySet()) {
                    switch (data) {
                        case "id" -> inProduct.setId((String) notParseProduct.get("id"));
                        case "name" -> inProduct.setName((String) notParseProduct.get("name"));
                        case "parentId" -> inProduct.setParentId((String) notParseProduct.get("parentId"));
                        case "price" ->
                                inProduct.setPrice(notParseProduct.get("price") != null ? Long.valueOf((Integer) notParseProduct.get("price")) : null);
                        case "type" ->
                                inProduct.setType(Objects.equals(notParseProduct.get("type"), "OFFER") ? OfferAndCategoryModel.Type.OFFER : OfferAndCategoryModel.Type.CATEGORY);
                    }
                }

                inProduct.setDate(updateTime);

                if (!isProductValid(inProduct))
                    return ResponseEntity.badRequest().body("Validation Failed");

                Optional<OfferAndCategoryModel> optionalProductFromDb = productRepo.findById(inProduct.getId());

                if (optionalProductFromDb.isPresent()) {

                    OfferAndCategoryModel productFromDb = optionalProductFromDb.get();

                    if (!isProductUpdateValid(productFromDb, inProduct))
                        return ResponseEntity.badRequest().body("Validation Failed");


                }

                outProducts.add(inProduct);

            }

        } catch (DateTimeException | NullPointerException exception) {
            return ResponseEntity.badRequest().body("Validation Failed");
        }
        if (!isProductParentAndIdValid(outProducts))
            return ResponseEntity.badRequest().body("Validation Failed");

        productRepo.saveAll(outProducts);
        for (var updated_product : outProducts) {
            updateDates(updated_product);
        }

        for (OfferAndCategoryModel category_product : productRepo.findAll().stream().
                filter(product -> product.getType() == OfferAndCategoryModel.Type.CATEGORY).toList()) {
            Long sum = calcCategory(category_product);
            category_product.setPrice(sum);
            productRepo.save(category_product);
        }

        return ResponseEntity.ok("OK");
    }

    private boolean isProductParentAndIdValid(List<OfferAndCategoryModel> products) {
        for (int i = 0; i < products.size(); i++) {
            for (int j = 0; j < products.size(); j++) {
                if (j == i) break;
                if ((Objects.equals(products.get(i).getParentId(), products.get(j).getId()) && products.get(j).getType() == OfferAndCategoryModel.Type.OFFER)
                        || (Objects.equals(products.get(i).getId(), products.get(j).getId())))
                    return false;
            }
        }
        return true;
    }


    private boolean isProductUpdateValid(OfferAndCategoryModel productFromDb, OfferAndCategoryModel product) {
        return product.getType() == productFromDb.getType();
    }

    private boolean isProductValid(OfferAndCategoryModel product) {

        if (product.getName() == null)
            return false;

        if (product.getType() == OfferAndCategoryModel.Type.CATEGORY)
            return product.getPrice() == null;

        else if (product.getType() == OfferAndCategoryModel.Type.OFFER) {

            if (product.getPrice() == null || product.getPrice() < 0)
                return false;

            if (product.getParentId() != null) {
                Optional<OfferAndCategoryModel> optionalProductFromDb = productRepo.findById(product.getParentId());
                return optionalProductFromDb.isEmpty() || optionalProductFromDb.get().getType() != OfferAndCategoryModel.Type.OFFER;
            }
        }

        return true;
    }

    // 2. delete{id}: delete
    @DeleteMapping("delete/{id}")
    public ResponseEntity deleteById(@PathVariable String id) {
        try {
            if (productRepo.findById(id).isPresent()) {
                productRepo.deleteById(id);
                return ResponseEntity.ok("Удаление прошло успешно.");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Невалидная схема документа или входные данные не верны.");
        }

    }

    // 3. nodes{id}: get
    @GetMapping("nodes/{id}")
    public OfferAndCategoryEntity getNodes(@PathVariable String id) {
        OfferAndCategoryModel new_product;
        try {
            new_product = productRepo.findById(id).get();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Not Found", e);
        }
        try {
            OfferAndCategoryEntity response_product = new OfferAndCategoryEntity(new_product);
            findChildren(response_product);
            return response_product;

        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "BadRequest)", e);
        }
    }


    // 4. sales: get
    @GetMapping("/sales")
    public List<OfferAndCategoryModel> getSalesOn24Hours(@RequestParam("date") String requestTime) {
        try {
            Instant time = Instant.parse(requestTime);

            List<OfferAndCategoryModel> product_list = new LinkedList<>();
            for (var p : productRepo.findAll())
                if (p.getType() == OfferAndCategoryModel.Type.OFFER && abs(Instant.parse(p.getDate()).toEpochMilli() - time.toEpochMilli()) <= 24 * 60 * 60 * 60L)
                    product_list.add(p);
            return product_list;

        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "", exception);
        }
    }

    // 5. node/{id}/statistic: get
    @GetMapping("node/{id}/statistic")
    public OfferAndCategoryModel getStatistic(@PathVariable String id, @RequestParam("dateStart") String date1, @RequestParam("dateEnd") String date2) {
        try {
            Instant time1 = Instant.parse(date1);
            Instant time2 = Instant.parse(date2);
            try {
                try {
                    OfferAndCategoryModel response_product = new OfferAndCategoryModel(productRepo.getReferenceById(id));
                    if (response_product.getType() != OfferAndCategoryModel.Type.OFFER) {
                        PriceAndNumber state = new PriceAndNumber(0L, 0);
                        PriceAndNumber category_data = calcCategoryPrice(response_product, state, time1, time2);
                        Long new_price = category_data.calcAverage();
                        response_product.setPrice(new_price);
                    }
                    return response_product;
                } catch (Exception exception) {
                    throw new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Not Found", exception);
                }

            } catch (Exception exception) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Validation Failed", exception);
            }
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Validation Failed", exception);
        }
    }

    private static class PriceAndNumber {
        private Long price;
        private Integer number;

        public PriceAndNumber(Long price, Integer number) {
            this.price = price;
            this.number = number;
        }

        public void addPrice(Long price) {
            this.price += price;
            this.addNumber();
        }
        private void addNumber() {
            this.number++;
        }

        private Long calcAverage() {
            if (number == 0) {
                return null;
            } else {
                return price / number;
            }
        }

    }

    private boolean timeInInterval(Instant time, Instant startTime, Instant endTime) {
        return time.toEpochMilli() >= startTime.toEpochMilli() &&
                time.toEpochMilli() < endTime.toEpochMilli();
    }

    private PriceAndNumber calcCategoryPrice(OfferAndCategoryModel current_product,
                                             PriceAndNumber current_state,
                                             Instant startTime,
                                             Instant endTime) {
        for (OfferAndCategoryModel product : productRepo.findAll().stream().toList()) {
            if (timeInInterval(Instant.parse(product.getDate()), startTime, endTime)) {
                if (current_product.getId().equals(product.getParentId())) {
                    if (product.getType() == OfferAndCategoryModel.Type.CATEGORY) {
                        calcCategoryPrice(product, current_state, startTime, endTime);
                    } else {
                        current_state.addPrice(product.getPrice());
                    }
                }
            }
        }
        return current_state;
    }

    private void updateDates(OfferAndCategoryModel updated_product) {
        for (var product: productRepo.findAll().stream().toList()) {
            if (updated_product.getParentId() != null) {
                if (product.getId().equals(updated_product.getParentId()) ) {
                    product.setDate(updated_product.getDate());
                    productRepo.save(product);
                    if (product.getParentId() != null) updateDates(product);
                }
            }
        }
    }

    private PriceAndNumber sumCategoryPrice(OfferAndCategoryModel category_product, PriceAndNumber category_data) {
        for (OfferAndCategoryModel product : productRepo.findAll().stream().toList()) {
            if (product.getParentId() != null) {
                if (product.getParentId().equals(category_product.getId())) {
                    if (product.getType() == OfferAndCategoryModel.Type.CATEGORY) {
                        category_data = sumCategoryPrice(product, category_data);
                    } else {
                        category_data.addPrice(product.getPrice());
                    }
                }
            }
        }
        return category_data;
    }

    private Long calcCategory(OfferAndCategoryModel category_product) {
        PriceAndNumber category_data = new PriceAndNumber(0L, 0);
        category_data = sumCategoryPrice(category_product, category_data);
        return category_data.calcAverage();
    }

    private void findChildren(OfferAndCategoryEntity current_product_entity) {
        String current_id = current_product_entity.getId();
        List<OfferAndCategoryModel> children_list = productRepo.findAll().stream()
                .filter(product -> Objects.equals(product.getParentId(), current_id))
                .toList();

        if (children_list.isEmpty()) {
            current_product_entity.setChildren(null);
            return;
        }
        for (OfferAndCategoryModel child : children_list) {
            current_product_entity.addChildren(child);
        }
        for (var child_product_entity : current_product_entity.getChildren()) {
            findChildren(child_product_entity);
        }
    }


}
