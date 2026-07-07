package springprojects.auctionssystem.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import auction.CancelAuctionUseCase;
import auction.CloseAuctionUseCase;
import auction.CreateAuctionUseCase;
import auction.GetAuctionUseCase;
import auction.ListAuctionsUseCase;
import auction.PublishAuctionUseCase;
import auction.input.ListAuctionsInput;
import auction.output.AuctionSummary;
import auction.output.ListAuctionsResult;
import controller.AuctionController;
import domain.auction.AuctionStatus;
import domain.shared.PageResult;
import domain.user.TokenGenerator;
import exception.AuctionExceptionHandler;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import security.JwtAuthenticationFilter;
import springprojects.auctionssystem.config.SecurityConfig;

// Slice test con las piezas reales de app:spring: SecurityConfig (filtro JWT + reglas
// de autorizacion) y AuctionExceptionHandler se importan explicitamente. No se usa
// AuctionsSystemApplication como fuente de configuracion porque su @ComponentScan
// completo arrastraria TestcontainersConfiguration (los beans @TestConfiguration no
// se filtran en los slices) e intentaria levantar un Postgres real via Docker.
@WebMvcTest(controllers = AuctionController.class)
class AuctionControllerWebTest {

  @SpringBootConfiguration
  @Import({
    AuctionController.class,
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    AuctionExceptionHandler.class
  })
  static class TestConfig {}

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TokenGenerator tokenGenerator;

  @MockitoBean private CreateAuctionUseCase createAuctionUseCase;
  @MockitoBean private GetAuctionUseCase getAuctionUseCase;
  @MockitoBean private PublishAuctionUseCase publishAuctionUseCase;
  @MockitoBean private CancelAuctionUseCase cancelAuctionUseCase;
  @MockitoBean private CloseAuctionUseCase closeAuctionUseCase;
  @MockitoBean private ListAuctionsUseCase listAuctionsUseCase;

  private AuctionSummary buildSummary(AuctionStatus status, UUID categoryId) {
    return AuctionSummary.builder()
        .id(UUID.randomUUID())
        .title("Subasta test")
        .description("Descripcion")
        .categoryId(categoryId)
        .sellerId(UUID.randomUUID())
        .currentPrice(BigDecimal.TEN)
        .status(status)
        .startsAt(Instant.now())
        .endsAt(Instant.now().plusSeconds(3600))
        .build();
  }

  @Test
  void listAuctions_publicRoute_isAccessibleWithoutAuthentication() throws Exception {
    PageResult<AuctionSummary> page =
        PageResult.<AuctionSummary>builder()
            .content(List.of(buildSummary(AuctionStatus.ACTIVE, UUID.randomUUID())))
            .totalElements(1L)
            .totalPages(1)
            .currentPage(0)
            .pageSize(10)
            .build();

    when(listAuctionsUseCase.run(any())).thenReturn(new ListAuctionsResult(page));

    // sin header Authorization: /auctions es publica segun SecurityConfig (permitAll en GET)
    mockMvc
        .perform(get("/auctions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.content", hasSize(1)))
        .andExpect(jsonPath("$.page.totalElements").value(1));

    ArgumentCaptor<ListAuctionsInput> captor = ArgumentCaptor.forClass(ListAuctionsInput.class);
    verify(listAuctionsUseCase).run(captor.capture());
    assertThat(captor.getValue().status()).isEmpty();
    assertThat(captor.getValue().categoryId()).isEmpty();
  }

  @Test
  void listAuctions_withFilters_forwardsThemToUseCase() throws Exception {
    UUID categoryId = UUID.randomUUID();
    PageResult<AuctionSummary> page =
        PageResult.<AuctionSummary>builder()
            .content(List.of())
            .totalElements(0L)
            .totalPages(0)
            .currentPage(1)
            .pageSize(5)
            .build();

    when(listAuctionsUseCase.run(any())).thenReturn(new ListAuctionsResult(page));

    mockMvc
        .perform(
            get("/auctions")
                .param("status", "ACTIVE")
                .param("categoryId", categoryId.toString())
                .param("page", "1")
                .param("size", "5"))
        .andExpect(status().isOk());

    ArgumentCaptor<ListAuctionsInput> captor = ArgumentCaptor.forClass(ListAuctionsInput.class);
    verify(listAuctionsUseCase).run(captor.capture());
    assertThat(captor.getValue().status()).contains(AuctionStatus.ACTIVE);
    assertThat(captor.getValue().categoryId()).contains(categoryId);
    assertThat(captor.getValue().page()).isEqualTo(1);
    assertThat(captor.getValue().size()).isEqualTo(5);
  }

  @Test
  void create_withoutAuthentication_isRejectedByRealSecurityChain() throws Exception {
    // create() requiere SELLER o ADMIN (@PreAuthorize) segun AuctionController;
    // sin Authorization header, SecurityConfig exige autenticacion (anyRequest().authenticated()).
    // Sin formLogin/httpBasic habilitados, Spring Security responde 403
    // (Http403ForbiddenEntryPoint)
    // en vez de 401 para peticiones no autenticadas.
    mockMvc
        .perform(post("/auctions").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isForbidden());
  }
}
