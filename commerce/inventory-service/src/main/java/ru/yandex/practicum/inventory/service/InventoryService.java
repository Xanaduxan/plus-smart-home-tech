package ru.yandex.practicum.inventory.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.entity.Inventory;
import ru.yandex.practicum.inventory.exception.InsufficientStockException;
import ru.yandex.practicum.inventory.exception.NotFoundException;
import ru.yandex.practicum.inventory.mapper.InventoryMapper;
import ru.yandex.practicum.inventory.repository.InventoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    public List<InventoryDto> getAllInventory() {
        return inventoryRepository.findAll().stream()
                .map(inventoryMapper::toDto)
                .toList();
    }

    public InventoryDto getByProductId(Long productId) {
        return inventoryMapper.toDto(findInventory(productId));
    }

    @Transactional
    public InventoryDto createInventory(UpdateInventoryRequest request) {
        if (inventoryRepository.existsByProductId(request.productId())) {
            throw new IllegalArgumentException(
                    "Складская запись для товара с id "
                            + request.productId() + " уже существует"
            );
        }

        Inventory inventory = inventoryMapper.toEntity(request);
        Inventory savedInventory = inventoryRepository.save(inventory);
        return inventoryMapper.toDto(savedInventory);
    }

    @Transactional
    public InventoryDto updateInventory(UpdateInventoryRequest request) {
        Inventory inventory = findInventory(request.productId());

        if (request.quantity() < inventory.getReservedQuantity()) {
            throw new IllegalArgumentException(
                    "Общее количество не может быть меньше зарезервированного: "
                            + inventory.getReservedQuantity()
            );
        }

        inventory.setQuantity(request.quantity());
        Inventory savedInventory = inventoryRepository.save(inventory);
        return inventoryMapper.toDto(savedInventory);
    }

    @Transactional
    public ReserveResponse reserveStock(ReserveRequest request) {
        Inventory inventory = findInventory(request.productId());

        if (request.quantity() > inventory.getAvailableQuantity()) {
            throw new InsufficientStockException(
                    "Недостаточно товара с id " + request.productId()
                            + ": доступно " + inventory.getAvailableQuantity()
                            + ", запрошено " + request.quantity()
            );
        }

        inventory.setReservedQuantity(
                inventory.getReservedQuantity() + request.quantity()
        );

        return new ReserveResponse(
                true,
                inventory.getAvailableQuantity(),
                "Товар успешно зарезервирован"
        );
    }

    private Inventory findInventory(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new NotFoundException(
                        "Складская запись для товара с id "
                                + productId + " не найдена"
                ));
    }
}
