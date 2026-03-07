package com.project.code.Service;

import com.project.code.Model.Customer;
import com.project.code.Model.Inventory;
import com.project.code.Model.OrderDetails;
import com.project.code.Model.OrderItem;
import com.project.code.Model.PlaceOrderRequestDTO;
import com.project.code.Model.PurchaseProductDTO;
import com.project.code.Model.Store;
import com.project.code.Repo.CustomerRepository;
import com.project.code.Repo.InventoryRepository;
import com.project.code.Repo.OrderDetailsRepository;
import com.project.code.Repo.OrderItemRepository;
import com.project.code.Repo.ProductRepository;
import com.project.code.Repo.StoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {
	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private InventoryRepository inventoryRepository;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private StoreRepository storeRepository;

	@Autowired
	private OrderDetailsRepository orderDetailsRepository;

	@Autowired
	private OrderItemRepository orderItemRepository;

	public void saveOrder(PlaceOrderRequestDTO placeOrderRequest) {
		if (placeOrderRequest == null) {
			throw new RuntimeException("Invalid order request");
		}

		Customer customer = customerRepository.findByEmail(placeOrderRequest.getCustomerEmail());
		if (customer == null) {
			customer = new Customer();
			customer.setName(placeOrderRequest.getCustomerName());
			customer.setEmail(placeOrderRequest.getCustomerEmail());
			customer.setPhone(placeOrderRequest.getCustomerPhone());
			customer = customerRepository.save(customer);
		}

		Store store = storeRepository.findById(placeOrderRequest.getStoreId());
		if (store == null) {
			throw new RuntimeException("Store not found");
		}

		OrderDetails orderDetails = new OrderDetails();
		orderDetails.setCustomer(customer);
		orderDetails.setStore(store);
		orderDetails.setTotalPrice(placeOrderRequest.getTotalPrice());
		orderDetails.setDate(LocalDateTime.now());
		orderDetails = orderDetailsRepository.save(orderDetails);

		if (placeOrderRequest.getPurchaseProduct() == null) {
			return;
		}

		for (PurchaseProductDTO purchaseProduct : placeOrderRequest.getPurchaseProduct()) {
			Inventory inventory = inventoryRepository.findByProductIdandStoreId(purchaseProduct.getId(), store.getId());
			if (inventory == null) {
				throw new RuntimeException("Inventory not found for product id: " + purchaseProduct.getId());
			}

			Integer currentStock = inventory.getStockLevel() == null ? 0 : inventory.getStockLevel();
			Integer orderedQty = purchaseProduct.getQuantity() == null ? 0 : purchaseProduct.getQuantity();
			if (currentStock < orderedQty) {
				throw new RuntimeException("Insufficient stock for product id: " + purchaseProduct.getId());
			}

			inventory.setStockLevel(currentStock - orderedQty);
			inventoryRepository.save(inventory);

			Double unitPrice = purchaseProduct.getPrice() != null
					? purchaseProduct.getPrice()
					: inventory.getProduct().getPrice();

			OrderItem orderItem = new OrderItem(orderDetails, inventory.getProduct(), orderedQty, unitPrice);
			orderItemRepository.save(orderItem);
		}
	}
}
