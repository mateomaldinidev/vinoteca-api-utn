package com.utn.vinoteca.features.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityDataInitializer {
    private final PermitRepository permitRepository;
    private final RolesRepository roleRepository;

    @Bean
    CommandLineRunner init() {

        return args -> {

            // Crear permisos
            for (Permits permit : Permits.values()) {

                if (permitRepository.findByPermit(permit).isEmpty()) {

                    permitRepository.save(
                            PermitEntity.builder()
                                    .permit(permit)
                                    .build()
                    );
                }
            }

            // ADMIN
            RoleEntity admin = roleRepository.findByRole(Roles.ROLE_ADMIN)
                    .orElseGet(() -> new RoleEntity(Roles.ROLE_ADMIN));

            admin.getPermits().clear();
            admin.getPermits().addAll(
                    permitRepository.findAll()
            );

            roleRepository.save(admin);

            // GUEST
            createRoleIfNotExists(
                    Roles.ROLE_GUEST,
                    List.of(
                            Permits.PRODUCT_READ,
                            Permits.PRODUCT_LIST,
                            Permits.PRODUCT_VARIANT_READ,
                            Permits.PRODUCT_VARIANT_LIST,
                            Permits.CATEGORY_READ,
                            Permits.CATEGORY_LIST,
                            Permits.BRAND_READ,
                            Permits.BRAND_LIST,
                            Permits.OFFER_READ,
                            Permits.OFFER_LIST
                    )
            );

            // CUSTOMER
            createRoleIfNotExists(
                    Roles.ROLE_CUSTOMER,
                    List.of(
                            Permits.PRODUCT_READ,
                            Permits.PRODUCT_LIST,
                            Permits.PRODUCT_VARIANT_READ,
                            Permits.PRODUCT_VARIANT_LIST,
                            Permits.CATEGORY_READ,
                            Permits.CATEGORY_LIST,
                            Permits.BRAND_READ,
                            Permits.BRAND_LIST,
                            Permits.OFFER_READ,
                            Permits.OFFER_LIST,

                            Permits.ADDRESS_CREATE,
                            Permits.ADDRESS_READ,
                            Permits.ADDRESS_LIST_BY_USER,
                            Permits.ADDRESS_UPDATE,
                            Permits.ADDRESS_DELETE,

                            Permits.CART_CREATE_OR_UPDATE,
                            Permits.CART_READ,
                            Permits.CART_UPDATE_ITEM_QUANTITY,
                            Permits.CART_CLEAR,
                            Permits.CART_CHECKOUT,

                            Permits.ORDER_READ,
                            Permits.ORDER_READ_BY_USER,
                            Permits.ORDER_CANCEL_OWN,

                            Permits.PAYMENT_CREATE,
                            Permits.PAYMENT_READ,

                            Permits.NOTIFICATION_READ_BY_USER,
                            Permits.NOTIFICATION_MARK_AS_READ,
                            Permits.NOTIFICATION_MARK_ALL_AS_READ
                    )
            );

            // EMPLOYEE
            createRoleIfNotExists(
                    Roles.ROLE_EMPLOYEE,
                    List.of(
                            Permits.PRODUCT_CREATE,
                            Permits.PRODUCT_UPDATE,
                            Permits.PRODUCT_DELETE,
                            Permits.PRODUCT_ACTIVATE,

                            Permits.PRODUCT_VARIANT_CREATE,
                            Permits.PRODUCT_VARIANT_UPDATE,
                            Permits.PRODUCT_VARIANT_DELETE,
                            Permits.PRODUCT_VARIANT_ACTIVATE,

                            Permits.STOCK_READ,
                            Permits.STOCK_ENTRY,
                            Permits.STOCK_ADJUSTMENT,

                            Permits.STOCK_MOVEMENT_READ,
                            Permits.STOCK_MOVEMENT_LIST_BY_VARIANT,

                            Permits.CATEGORY_CREATE,
                            Permits.CATEGORY_UPDATE,
                            Permits.CATEGORY_DELETE,
                            Permits.CATEGORY_ACTIVATE,

                            Permits.BRAND_CREATE,
                            Permits.BRAND_UPDATE,
                            Permits.BRAND_DELETE,
                            Permits.BRAND_ACTIVATE,

                            Permits.SUPPLIER_CREATE,
                            Permits.SUPPLIER_UPDATE,
                            Permits.SUPPLIER_DELETE,
                            Permits.SUPPLIER_ACTIVATE,

                            Permits.OFFER_CREATE,
                            Permits.OFFER_ADD_PRODUCT,
                            Permits.OFFER_REMOVE_PRODUCT,
                            Permits.OFFER_ACTIVATE,
                            Permits.OFFER_DEACTIVATE,

                            Permits.ORDER_READ_ALL,
                            Permits.ORDER_CANCEL_ANY,
                            Permits.ORDER_MANAGE_STATUS
                    )
            );

            System.out.println("Cantidad de permisos: "
                    + permitRepository.count());

            System.out.println("Roles existentes: "
                    + roleRepository.count());

            roleRepository.findByRole(Roles.ROLE_ADMIN)
                    .ifPresent(r -> System.out.println(
                            "Permisos ADMIN: "
                                    + r.getPermits().size()
                    ));
        };
    }

    private void createRoleIfNotExists(
            Roles role,
            List<Permits> permits
    ) {

        if (roleRepository.findByRole(role).isPresent()) {
            return;
        }

        RoleEntity roleEntity = new RoleEntity(role);

        permits.forEach(permission ->
                permitRepository.findByPermit(permission)
                        .ifPresent(roleEntity::addPermit)
        );

        roleRepository.save(roleEntity);
    }

}
