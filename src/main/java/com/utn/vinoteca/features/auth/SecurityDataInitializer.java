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
                            Permits.WINE_READ,
                            Permits.WINE_LIST,
                            Permits.WINE_VARIANT_READ,
                            Permits.WINE_VARIANT_LIST,
                            Permits.WINE_TYPE_READ,
                            Permits.WINE_TYPE_LIST,
                            Permits.WINERY_READ,
                            Permits.WINERY_LIST,
                            Permits.OFFER_READ,
                            Permits.OFFER_LIST
                    )
            );

            // CUSTOMER
            createRoleIfNotExists(
                    Roles.ROLE_CUSTOMER,
                    List.of(
                            Permits.WINE_READ,
                            Permits.WINE_LIST,
                            Permits.WINE_VARIANT_READ,
                            Permits.WINE_VARIANT_LIST,
                            Permits.WINE_TYPE_READ,
                            Permits.WINE_TYPE_LIST,
                            Permits.WINERY_READ,
                            Permits.WINERY_LIST,
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
                            Permits.WINE_CREATE,
                            Permits.WINE_UPDATE,
                            Permits.WINE_DELETE,
                            Permits.WINE_ACTIVATE,

                            Permits.WINE_VARIANT_CREATE,
                            Permits.WINE_VARIANT_UPDATE,
                            Permits.WINE_VARIANT_DELETE,
                            Permits.WINE_VARIANT_ACTIVATE,

                            Permits.STOCK_READ,
                            Permits.STOCK_ENTRY,
                            Permits.STOCK_ADJUSTMENT,

                            Permits.STOCK_MOVEMENT_READ,
                            Permits.STOCK_MOVEMENT_LIST_BY_VARIANT,

                            Permits.WINE_TYPE_CREATE,
                            Permits.WINE_TYPE_UPDATE,
                            Permits.WINE_TYPE_DELETE,
                            Permits.WINE_TYPE_ACTIVATE,

                            Permits.WINERY_CREATE,
                            Permits.WINERY_UPDATE,
                            Permits.WINERY_DELETE,
                            Permits.WINERY_ACTIVATE,

                            Permits.DISTRIBUTOR_CREATE,
                            Permits.DISTRIBUTOR_UPDATE,
                            Permits.DISTRIBUTOR_DELETE,
                            Permits.DISTRIBUTOR_ACTIVATE,

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
