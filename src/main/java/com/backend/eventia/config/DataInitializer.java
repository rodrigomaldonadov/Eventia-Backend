package com.backend.eventia.config;

import com.backend.eventia.model.Category;
import com.backend.eventia.model.Event;
import com.backend.eventia.model.Order;
import com.backend.eventia.model.OrderDetail;
import com.backend.eventia.model.Ticket;
import com.backend.eventia.model.TicketType;
import com.backend.eventia.model.User;
import com.backend.eventia.model.enums.EventStatus;
import com.backend.eventia.model.enums.OrderStatus;
import com.backend.eventia.model.enums.TicketStatus;
import com.backend.eventia.model.enums.UserRole;
import com.backend.eventia.repository.CategoryRepository;
import com.backend.eventia.repository.EventRepository;
import com.backend.eventia.repository.OrderRepository;
import com.backend.eventia.repository.TicketRepository;
import com.backend.eventia.repository.TicketTypeRepository;
import com.backend.eventia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Conditional(DataInitializer.SeedEnabledCondition.class)
public class DataInitializer {

    /** Banners por título: se guardan en la columna banner_url de la tabla events. */
    private static final Map<String, String> BANNERS_BY_TITLE = Map.of(
            "Illapu: 50 Años de Memoria",
            "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=1200&auto=format&fit=crop",
            "Sunsets Sessions Costa Verde",
            "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=1200&auto=format&fit=crop",
            "La Ciudad y los Perros (Adaptación)",
            "https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?q=80&w=1200&auto=format&fit=crop",
            "Expo Sabor & Coctelería Andina",
            "https://images.unsplash.com/photo-1510812431401-41d2bd2722f3?q=80&w=1200&auto=format&fit=crop",
            "Trap & Latin Flow Live Tour",
            "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?q=80&w=1200&auto=format&fit=crop",
            "Peru Tech Summit & Fintech 2025",
            "https://images.unsplash.com/photo-1540575467063-178a50c2df87?q=80&w=1200&auto=format&fit=crop"
    );

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initData() {
        return args -> {
            createUserIfMissing("Organizador", "Uno", "20000002", "organizer1@eventia.com", "987654321", UserRole.ORGANIZER);
            createUserIfMissing("Organizador", "Dos", "20000003", "organizer2@eventia.com", "987654322", UserRole.ORGANIZER);
            createUserIfMissing("Cliente", "Uno", "30000001", "cliente1@eventia.com", "911111111", UserRole.USER);
            createUserIfMissing("Cliente", "Dos", "30000002", "cliente2@eventia.com", "922222222", UserRole.USER);
            createUserIfMissing("Cliente", "Tres", "30000003", "cliente3@eventia.com", "933333333", UserRole.USER);
            createUserIfMissing("Cliente", "Inactivo", "30000004", "cliente4@eventia.com", "944444444", UserRole.USER, false);

            seedCategories();

            if (eventRepository.count() == 0) {
                log.info("[DataInitializer] Creando eventos y tipos de entrada de ejemplo");
                seedCatalog();
            }

            backfillEventBanners();

            if (orderRepository.count() == 0) {
                seedPurchases();
            }
        };
    }

    private void createUserIfMissing(String firstName, String lastName, String dni, String email,
                                     String cellphone, UserRole role) {
        createUserIfMissing(firstName, lastName, dni, email, cellphone, role, true);
    }

    private void createUserIfMissing(String firstName, String lastName, String dni, String email,
                                     String cellphone, UserRole role, boolean active) {
        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }
        userRepository.save(User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .dni(dni)
                .email(email)
                .cellphone(cellphone)
                .password(passwordEncoder.encode("Eventia123*"))
                .role(role)
                .active(active)
                .deactivatedAt(active ? null : LocalDateTime.now())
                .build());
        log.info("[DataInitializer] Usuario de prueba creado email='{}' rol={} active={}", email, role, active);
    }

    private void seedCategories() {
        List<String[]> initialCategories = List.of(
                new String[] { "Conciertos", "Presentaciones musicales en vivo, festivales y recitales." },
                new String[] { "Festivales", "Grandes encuentros artísticos, gastronómicos y temáticos." },
                new String[] { "Teatro & Artes", "Obras teatrales, comedia, artes escénicas y musicales." },
                new String[] { "Tecnología & Startups",
                        "Conferencias, hackathons, foros tecnológicos y networking." },
                new String[] { "Gastronomía & Ferias",
                        "Ferias culinarias representativas y catas gastronómicas." });

        for (String[] category : initialCategories) {
            getOrCreateCategory(category[0], category[1]);
        }
    }

    private void seedCatalog() {
        User organizer = userRepository.findByEmail("organizer1@eventia.com").orElseThrow();

        Category conciertos = getOrCreateCategory("Conciertos", "Presentaciones musicales en vivo, festivales y recitales.");
        Category festivales = getOrCreateCategory("Festivales", "Grandes encuentros artísticos, gastronómicos y temáticos.");
        Category teatro = getOrCreateCategory("Teatro & Artes", "Obras teatrales, comedia, artes escénicas y musicales.");
        Category gastronomia = getOrCreateCategory("Gastronomía & Ferias", "Ferias culinarias representativas y catas gastronómicas.");
        Category tecnologia = getOrCreateCategory("Tecnología & Startups", "Conferencias, hackathons, foros tecnológicos y networking.");

        saveEvent("Illapu: 50 Años de Memoria", "Concierto por los 50 años de Illapu",
                LocalDate.of(2026, 11, 22), "Estadio Nacional, Lima", 45000, conciertos, organizer, 120.0,
                "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=1200&auto=format&fit=crop");
        saveEvent("Sunsets Sessions Costa Verde", "Festival al atardecer frente al mar",
                LocalDate.of(2026, 12, 6), "Arena 1, San Miguel", 12000, festivales, organizer, 85.0,
                "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=1200&auto=format&fit=crop");
        saveEvent("La Ciudad y los Perros (Adaptación)", "Adaptación teatral de la novela",
                LocalDate.of(2026, 11, 28), "Teatro Peruano Japonés", 800, teatro, organizer, 60.0,
                "https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?q=80&w=1200&auto=format&fit=crop");
        saveEvent("Expo Sabor & Coctelería Andina", "Feria gastronómica con coctelería andina",
                LocalDate.of(2026, 12, 14), "Jockey Club del Perú", 5000, gastronomia, organizer, 45.0,
                "https://images.unsplash.com/photo-1510812431401-41d2bd2722f3?q=80&w=1200&auto=format&fit=crop");
        saveEvent("Trap & Latin Flow Live Tour", "Gira de trap y latin flow",
                LocalDate.of(2027, 1, 18), "Arena 1, Costa Verde", 15000, conciertos, organizer, 140.0,
                "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?q=80&w=1200&auto=format&fit=crop");
        saveEvent("Peru Tech Summit & Fintech 2025", "Cumbre de tecnología y fintech",
                LocalDate.of(2027, 1, 25), "Centro de Convenciones 27 de Enero", 3000, tecnologia, organizer, 210.0,
                "https://images.unsplash.com/photo-1540575467063-178a50c2df87?q=80&w=1200&auto=format&fit=crop");

        log.info("[DataInitializer] Eventos de ejemplo creados correctamente");
    }

    /**
     * Las bases de datos ya sembradas no tienen banners. Este metodo completa
     * la columna banner_url de los eventos existentes sin duplicarlos.
     */
    private void backfillEventBanners() {
        List<Event> events = eventRepository.findAll();
        int updated = 0;

        for (Event event : events) {
            if (event.getBannerUrl() != null && !event.getBannerUrl().isBlank()) {
                continue;
            }

            String bannerUrl = BANNERS_BY_TITLE.get(event.getTitle());
            if (bannerUrl == null) {
                continue;
            }

            event.setBannerUrl(bannerUrl);
            eventRepository.save(event);
            updated++;
        }

        if (updated > 0) {
            log.info("[DataInitializer] Banners agregados a {} eventos existentes", updated);
        }
    }

    private void seedPurchases() {
        User customer = userRepository.findByEmail("cliente1@eventia.com").orElseThrow();
        User organizer = userRepository.findByEmail("organizer1@eventia.com").orElseThrow();

        Event event = eventRepository.findByOrganizerIdAndActiveTrue(organizer.getId()).stream()
                .findFirst()
                .orElse(null);
        if (event == null) {
            return;
        }

        TicketType ticketType = ticketTypeRepository.findByEventIdAndActiveTrue(event.getId()).stream()
                .findFirst()
                .orElse(null);
        if (ticketType == null) {
            return;
        }

        int quantity = 2;
        double unitPrice = ticketType.getPrice();
        double subtotal = unitPrice * quantity;

        Order order = Order.builder()
                .totalAmount(subtotal)
                .paymentMethod("CARD")
                .paymentStatus(OrderStatus.PAID)
                .user(customer)
                .build();
        OrderDetail detail = OrderDetail.builder()
                .quantity(quantity)
                .unitPrice(unitPrice)
                .subtotal(subtotal)
                .order(order)
                .ticketType(ticketType)
                .build();
        order.getOrderDetails().add(detail);
        orderRepository.save(order);

        ticketType.setAvailableCapacity(Math.max(0, ticketType.getAvailableCapacity() - quantity));
        ticketTypeRepository.save(ticketType);

        for (int i = 0; i < quantity; i++) {
            ticketRepository.save(Ticket.builder()
                    .orderDetail(detail)
                    .status(TicketStatus.VALID)
                    .nombre(customer.getFirstName() + " " + customer.getLastName())
                    .dni(customer.getDni())
                    .qrCode(java.util.UUID.randomUUID().toString())
                    .build());
        }

        log.info("[DataInitializer] Orden de prueba creada para '{}' con {} entradas en '{}'",
                customer.getEmail(), quantity, event.getTitle());
    }

    private Category getOrCreateCategory(String name, String description) {
        return categoryRepository.findAll().stream()
                .filter(category -> category.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name(name)
                        .description(description)
                        .active(true)
                        .build()));
    }

    private void saveEvent(String title, String description, LocalDate date, String location,
                           int capacity, Category category, User organizer, double price, String bannerUrl) {
        Event event = eventRepository.save(Event.builder()
                .title(title)
                .description(description)
                .date(date)
                .startTime(LocalTime.of(20, 0))
                .endTime(LocalTime.of(23, 0))
                .location(location)
                .capacity(capacity)
                .bannerUrl(bannerUrl)
                .status(EventStatus.PUBLISHED)
                .active(true)
                .category(category)
                .organizer(organizer)
                .build());

        // Varias tarifas por evento para que el detalle muestre opciones reales
        int generalCapacity = (int) Math.round(capacity * 0.50);
        int preferentialCapacity = (int) Math.round(capacity * 0.30);
        int vipCapacity = capacity - generalCapacity - preferentialCapacity;

        saveTicketType(event, "General", price, generalCapacity, 0.75);
        saveTicketType(event, "Preferencial", Math.round(price * 1.6 * 100) / 100.0, preferentialCapacity, 0.85);
        saveTicketType(event, "VIP", Math.round(price * 2.4 * 100) / 100.0, vipCapacity, 0.90);
    }

    private void saveTicketType(Event event, String name, double price, int totalCapacity, double availableRatio) {
        ticketTypeRepository.save(TicketType.builder()
                .name(name)
                .price(price)
                .totalCapacity(totalCapacity)
                .availableCapacity((int) Math.round(totalCapacity * availableRatio))
                .maxPerPurchase(TicketType.DEFAULT_MAX_PER_PURCHASE)
                .active(true)
                .event(event)
                .build());
    }

    public static class SeedEnabledCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Environment environment = context.getEnvironment();
            String[] activeProfiles = environment.getActiveProfiles();

            boolean devProfileActive = Arrays.asList(activeProfiles).contains("dev")
                    || (activeProfiles.length == 0
                    && ("dev".equals(environment.getProperty("spring.profiles.default"))
                    || Arrays.asList(environment.getDefaultProfiles()).contains("dev")));

            return devProfileActive
                    || environment.getProperty("app.seed.enabled", Boolean.class, false);
        }
    }
}
