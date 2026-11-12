package com.backend.eventia.tickets;

import com.backend.eventia.model.Category;
import com.backend.eventia.model.Event;
import com.backend.eventia.model.TicketType;
import com.backend.eventia.model.User;
import com.backend.eventia.model.enums.EventStatus;
import com.backend.eventia.model.enums.OrderStatus;
import com.backend.eventia.model.enums.UserRole;
import com.backend.eventia.repository.CategoryRepository;
import com.backend.eventia.repository.EventRepository;
import com.backend.eventia.repository.TicketTypeRepository;
import com.backend.eventia.repository.UserRepository;
import com.backend.eventia.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TicketsAndOrdersPurchaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User robertoUser;
    private TicketType ticketType;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.save(
                Category.builder()
                        .name("Conciertos Test")
                        .description("Categoria test")
                        .build()
        );

        User organizer = userRepository.save(
                User.builder()
                        .email("organizer-t6@test.com")
                        .password(passwordEncoder.encode("Secret123!"))
                        .firstName("Organizer")
                        .lastName("Test")
                        .dni("12345678")
                        .role(UserRole.ORGANIZER)
                        .active(true)
                        .build()
        );

        Event event = eventRepository.save(
                Event.builder()
                        .title("Concierto Integrante 6")
                        .description("Evento de prueba compras")
                        .location("Lima Arena")
                        .date(LocalDate.now().plusDays(10))
                        .startTime(LocalTime.of(20, 0))
                        .endTime(LocalTime.of(23, 0))
                        .category(category)
                        .organizer(organizer)
                        .capacity(100)
                        .status(EventStatus.PUBLISHED)
                        .active(true)
                        .build()
        );

        ticketType = ticketTypeRepository.save(
                TicketType.builder()
                        .name("General Pass")
                        .zone("General")
                        .price(100.0)
                        .totalCapacity(50)
                        .availableCapacity(50)
                        .saleStartDate(LocalDate.now().minusDays(1))
                        .saleEndDate(LocalDate.now().plusDays(5))
                        .maxPerPurchase(5)
                        .active(true)
                        .event(event)
                        .build()
        );

        // Usuario llamado "Roberto" (en la versión anterior recibía 15% de descuento por nombre)
        robertoUser = userRepository.save(
                User.builder()
                        .email("roberto.compra@test.com")
                        .password(passwordEncoder.encode("Secret123!"))
                        .firstName("Roberto")
                        .lastName("Gomez")
                        .dni("87654321")
                        .role(UserRole.USER)
                        .active(true)
                        .build()
        );
    }

    @Test
    void purchase_ShouldNotApplyNameDiscount_AndShouldSaveNominationAndQrCode() throws Exception {
        String token = jwtUtils.generateToken(robertoUser);

        // Compra de 2 entradas con nominación personalizada
        String requestJson = """
                {
                    "paymentMethod": "CARD",
                    "items": [
                        {
                            "ticketTypeId": %d,
                            "quantity": 2,
                            "attendees": [
                                { "nombre": "Carlos Perez", "dni": "11223344" },
                                { "nombre": "Ana Diaz", "dni": "55667788" }
                            ]
                        }
                    ]
                }
                """.formatted(ticketType.getId());

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                // El total debe ser 200.0 (100 * 2), descuento 0.0 (ya NO hay descuento por nombre 'Roberto')
                .andExpect(jsonPath("$.totalAmount", is(200.0)))
                .andExpect(jsonPath("$.discountAmount", is(0.0)))
                .andExpect(jsonPath("$.status", is("PAID")))
                .andExpect(jsonPath("$.details[0].tickets", hasSize(2)))
                .andExpect(jsonPath("$.details[0].tickets[0].nombre", is("Carlos Perez")))
                .andExpect(jsonPath("$.details[0].tickets[0].dni", is("11223344")))
                .andExpect(jsonPath("$.details[0].tickets[0].qrCode", notNullValue()))
                .andExpect(jsonPath("$.details[0].tickets[1].nombre", is("Ana Diaz")))
                .andExpect(jsonPath("$.details[0].tickets[1].dni", is("55667788")))
                .andExpect(jsonPath("$.details[0].tickets[1].qrCode", notNullValue()));
    }

    @Test
    void findOrders_WithFilters_ShouldReturnFilteredOrders() throws Exception {
        String token = jwtUtils.generateToken(robertoUser);

        // Realizamos una compra
        String requestJson = """
                {
                    "paymentMethod": "CARD",
                    "items": [
                        {
                            "ticketTypeId": %d,
                            "quantity": 1
                        }
                    ]
                }
                """.formatted(ticketType.getId());

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        // Consultamos /api/orders con filtro de status PAID
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "PAID")
                        .param("fromDate", LocalDate.now().minusDays(1).toString())
                        .param("toDate", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.content[0].status", is("PAID")));
    }

    @Test
    void nominateTicket_ShouldUpdateNameAndDni() throws Exception {
        String token = jwtUtils.generateToken(robertoUser);

        String requestJson = """
                {
                    "paymentMethod": "CARD",
                    "items": [
                        {
                            "ticketTypeId": %d,
                            "quantity": 1
                        }
                    ]
                }
                """.formatted(ticketType.getId());

        String response = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Extraer id del ticket generado
        // Por defecto tomo el comprador
        mockMvc.perform(get("/api/tickets/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre", is("Roberto Gomez")))
                .andExpect(jsonPath("$[0].dni", is("87654321")));

        // Renombrar ticket a otro titular
        String nominateJson = """
                {
                    "nombre": "Mariana Lopez",
                    "dni": "99887766"
                }
                """;

        // Obtener el primer ticket id desde findMyTickets
        // Como sabemos que hay 1 ticket creado, hacemos PATCH
        // Obtenemos los tickets de roberto
        var tickets = mockMvc.perform(get("/api/tickets/my").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        // Buscar el id en el json sencillo: "id":<num>
        long ticketId = Long.parseLong(tickets.split("\"id\":")[1].split(",")[0]);

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/nominate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nominateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre", is("Mariana Lopez")))
                .andExpect(jsonPath("$.dni", is("99887766")));
    }
}
