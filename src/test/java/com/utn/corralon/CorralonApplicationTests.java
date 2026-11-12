package com.utn.corralon;

import com.utn.corralon.features.auth.PermitRepository;
import com.utn.corralon.features.auth.RolesRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
		"spring.docker.compose.enabled=false",
		"app.orders.expiration.enabled=false"
})
class CorralonApplicationTests {

	// Evita que SecurityDataInitializer escriba roles/permisos en la base real
	// durante la carga del contexto de test.
	@MockitoBean
	private PermitRepository permitRepository;

	@MockitoBean
	private RolesRepository rolesRepository;

	@Test
	void contextLoads() {
	}

}
