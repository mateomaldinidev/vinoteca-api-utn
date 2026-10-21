package com.utn.vinoteca.support;

import com.utn.vinoteca.security.entity.CredentialsEntity;
import com.utn.vinoteca.security.repository.CredentialsRepository;
import com.utn.vinoteca.security.entity.RoleEntity;
import com.utn.vinoteca.security.enums.Roles;
import com.utn.vinoteca.security.repository.RolesRepository;
import com.utn.vinoteca.features.cart.dto.CartRequestDTO;
import com.utn.vinoteca.features.cart.repository.CartRepository;
import com.utn.vinoteca.features.cart.service.ICartService;
import com.utn.vinoteca.features.cartItem.dto.CartItemRequestDTO;
import com.utn.vinoteca.features.distributor.entity.DistributorEntity;
import com.utn.vinoteca.features.distributor.repository.DistributorRepository;
import com.utn.vinoteca.features.notifications.service.IEmailService;
import com.utn.vinoteca.features.order.dto.OrderResponseDTO;
import com.utn.vinoteca.features.order.enums.DeliveryType;
import com.utn.vinoteca.features.order.repository.OrderRepository;
import com.utn.vinoteca.features.order.service.IOrderService;
import com.utn.vinoteca.features.payment.repository.PaymentRepository;
import com.utn.vinoteca.features.payment.service.IPaymentService;
import com.utn.vinoteca.features.stockMovement.entity.StockMovementEntity;
import com.utn.vinoteca.features.stockMovement.repository.StockMovementRepository;
import com.utn.vinoteca.features.user.entity.UserEntity;
import com.utn.vinoteca.features.user.repository.UserRepository;
import com.utn.vinoteca.features.wine.entity.WineEntity;
import com.utn.vinoteca.features.wine.repository.WineRepository;
import com.utn.vinoteca.features.wineType.entity.WineTypeEntity;
import com.utn.vinoteca.features.wineType.repository.WineTypeRepository;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import com.utn.vinoteca.features.wineVariant.repository.WineVariantRepository;
import com.utn.vinoteca.features.winery.entity.WineryEntity;
import com.utn.vinoteca.features.winery.repository.WineryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class AbstractIntegrationTest {

    @MockitoBean
    protected IEmailService emailService;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected CredentialsRepository credentialsRepository;

    @Autowired
    protected RolesRepository rolesRepository;

    @Autowired
    protected WineryRepository wineryRepository;

    @Autowired
    protected WineTypeRepository wineTypeRepository;

    @Autowired
    protected DistributorRepository distributorRepository;

    @Autowired
    protected WineRepository wineRepository;

    @Autowired
    protected WineVariantRepository wineVariantRepository;

    @Autowired
    protected CartRepository cartRepository;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected PaymentRepository paymentRepository;

    @Autowired
    protected StockMovementRepository stockMovementRepository;

    @Autowired
    protected ICartService cartService;

    @Autowired
    protected IOrderService orderService;

    @Autowired
    protected IPaymentService paymentService;

    protected UserEntity createCustomer() {
        UserEntity user = createUser();
        RoleEntity role = rolesRepository.findByRole(Roles.ROLE_CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("ROLE_CUSTOMER was not seeded"));

        credentialsRepository.save(CredentialsEntity.builder()
                .username(user.getEmail())
                .password("test-password")
                .enabled(true)
                .usuario(user)
                .roles(new HashSet<>(Set.of(role)))
                .build());

        return user;
    }

    protected UserEntity createUser() {
        String suffix = uniqueSuffix();

        return userRepository.save(UserEntity.builder()
                .email("user-" + suffix + "@vinoteca.test")
                .name("Test")
                .lastName("User")
                .password("test-password")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());
    }

    protected WineVariantEntity createVariant(BigDecimal price,
                                              int stock,
                                              BigDecimal wholesalePrice,
                                              Integer wholesaleMinQty) {
        String suffix = uniqueSuffix();

        WineryEntity winery = wineryRepository.save(WineryEntity.builder()
                .name("Winery " + suffix)
                .active(true)
                .build());

        WineTypeEntity wineType = wineTypeRepository.save(WineTypeEntity.builder()
                .name("Wine type " + suffix)
                .active(true)
                .build());

        DistributorEntity distributor = distributorRepository.save(DistributorEntity.builder()
                .name("Distributor " + suffix)
                .contact("distributor-" + suffix + "@vinoteca.test")
                .active(true)
                .build());

        WineEntity wine = wineRepository.save(WineEntity.builder()
                .name("Wine " + suffix)
                .active(true)
                .winery(winery)
                .wineType(wineType)
                .distributor(distributor)
                .build());

        return wineVariantRepository.save(WineVariantEntity.builder()
                .attribute("Botella 750 ml")
                .price(price)
                .stock(stock)
                .wholesalePrice(wholesalePrice)
                .wholesaleMinQty(wholesaleMinQty)
                .active(true)
                .wine(wine)
                .build());
    }

    protected void addToCart(UserEntity user, WineVariantEntity variant, int quantity) {
        cartService.createOrUpdateCart(CartRequestDTO.builder()
                .userId(user.getExternalId())
                .items(List.of(CartItemRequestDTO.builder()
                        .wineVariantId(variant.getExternalId())
                        .quantity(quantity)
                        .build()))
                .build());
    }

    protected OrderResponseDTO checkout(UserEntity user) {
        return cartService.checkout(user.getExternalId(), null, DeliveryType.PICKUP);
    }

    protected List<StockMovementEntity> movementsOf(WineVariantEntity variant) {
        return stockMovementRepository.findByVariantExternalId(variant.getExternalId());
    }

    protected static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private static String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
