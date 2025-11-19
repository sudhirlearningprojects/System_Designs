package org.sudhir512kj.ticketbooking.dto;

import java.util.List;

public class AddFoodItemsRequest {
    private List<FoodItemRequest> items;
    
    public AddFoodItemsRequest() {}
    
    public List<FoodItemRequest> getItems() { return items; }
    public void setItems(List<FoodItemRequest> items) { this.items = items; }
    
    public static class FoodItemRequest {
        private Long foodId;
        private Integer quantity;
        
        public FoodItemRequest() {}
        
        public Long getFoodId() { return foodId; }
        public void setFoodId(Long foodId) { this.foodId = foodId; }
        
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}