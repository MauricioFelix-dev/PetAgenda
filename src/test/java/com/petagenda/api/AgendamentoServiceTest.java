package com.petagenda.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.dao.DataAccessException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Testes da camada de serviço com um banco H2 em memória (não precisa do MySQL).
 * Rode com:  mvnw test -Dtest=AgendamentoServiceTest
 *
 * Cobrem: a TRANSAÇÃO (tudo ou nada), o antichoque por profissional, os
 * bloqueios de agenda e o serviço desativado.
 */
@SpringJUnitConfig(AgendamentoServiceTest.Config.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AgendamentoServiceTest {

    @Configuration
    @EnableTransactionManagement
    static class Config {
        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2)
                    .generateUniqueName(true)
                    .addScript("schema-teste.sql")
                    .build();
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource ds) {
            return new JdbcTemplate(ds);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource ds) {
            return new DataSourceTransactionManager(ds);
        }

        @Bean
        AgendamentoService agendamentoService(JdbcTemplate jdbc) {
            return new AgendamentoService(jdbc, 1, "America/Sao_Paulo");
        }
    }

    @Autowired
    AgendamentoService service;

    @Autowired
    JdbcTemplate jdbc;

    LocalDate dia;

    @BeforeEach
    void escolherDia() {
        dia = LocalDate.now().plusDays(10);
    }

    private Map<String, String> pedido(String telefone, String pet, int servico, String hora, String obs) {
        Map<String, String> d = new HashMap<>();
        d.put("tutor", "Tutor Teste");
        d.put("telefone", telefone);
        d.put("pet", pet);
        d.put("especie", "Cão");
        d.put("servico", String.valueOf(servico));
        d.put("data", dia.toString());
        d.put("horario", hora);
        d.put("obs", obs);
        return d;
    }

    private int contar(String tabela) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela, Integer.class);
    }

    @Test
    void sv01_agendamentoValidoGravaTudoComResponsavelECodigo() {
        Map<String, Object> r = service.criar(pedido("99988887777", "Rex", 1, "10:00", ""));
        assertEquals("Ana", r.get("responsavel"));
        assertEquals(6, ((String) r.get("codigo")).length());
        assertEquals(1, contar("tutor"));
        assertEquals(1, contar("pet"));
        Integer profissional = jdbc.queryForObject("SELECT id_funcionario FROM agendamento", Integer.class);
        assertEquals(2, profissional); // copiado do responsável do serviço (Ana)
    }

    @Test
    void sv02_transacaoDesfazTutorEPetSeOAgendamentoFalhar() {
        // A observação tem mais de 20 caracteres: o banco de teste recusa na ÚLTIMA gravação.
        // Sem @Transactional, tutor e pet ficariam gravados. Com a transação, nada fica.
        String obsLonga = "observação com bem mais de vinte caracteres";
        assertThrows(DataAccessException.class, () -> service.criar(pedido("99977776666", "Bob", 1, "11:00", obsLonga)));
        assertEquals(0, contar("tutor"));
        assertEquals(0, contar("pet"));
        assertEquals(0, contar("agendamento"));
    }

    @Test
    void sv03_mesmoProfissionalMesmoHorarioDaConflito() {
        service.criar(pedido("99911110001", "A", 1, "10:00", ""));                      // Banho com a Ana
        RegraNegocioException e = assertThrows(RegraNegocioException.class,
                () -> service.criar(pedido("99911110002", "B", 4, "09:30", "")));       // Banho + Tosa 09:30-10:45, também Ana
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void sv04_profissionaisDiferentesNoMesmoHorarioNaoConflitam() {
        service.criar(pedido("99911110001", "A", 1, "10:00", ""));                      // Banho com a Ana
        Map<String, Object> r = service.criar(pedido("99911110003", "C", 3, "10:00", "")); // Consulta com a Dra. Helena
        assertEquals("Dra. Helena", r.get("responsavel"));
        assertEquals(2, contar("agendamento"));
    }

    @Test
    void sv05_bloqueioDoPetShopTiraOHorarioDeTodos() {
        jdbc.update("INSERT INTO bloqueio (inicio, fim, motivo) VALUES (?, ?, 'Feriado')",
                dia.atTime(8, 0), dia.atTime(12, 0));
        List<String> livresBanho = service.horariosLivres(dia, 1);
        List<String> livresConsulta = service.horariosLivres(dia, 3);
        assertFalse(livresBanho.contains("11:30"));
        assertFalse(livresConsulta.contains("08:00"));
        assertTrue(livresBanho.contains("12:00"));
        assertThrows(RegraNegocioException.class, () -> service.criar(pedido("99911110004", "D", 3, "09:00", "")));
    }

    @Test
    void sv06_folgaDaAnaNaoAfetaAsConsultas() {
        jdbc.update("INSERT INTO bloqueio (inicio, fim, motivo, id_funcionario) VALUES (?, ?, 'Folga', 2)",
                dia.atTime(8, 0), dia.atTime(18, 0));
        assertTrue(service.horariosLivres(dia, 1).isEmpty());     // banho (Ana): nenhum horário
        assertEquals(20, service.horariosLivres(dia, 3).size());  // consulta (Dra. Helena): todos os 20
    }

    @Test
    void sv07_servicoDesativadoNaoPodeSerAgendado() {
        RegraNegocioException e = assertThrows(RegraNegocioException.class,
                () -> service.criar(pedido("99911110005", "E", 7, "10:00", "")));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }

    @Test
    void sv08_idadeDoPetEhGravadaQuandoInformada() {
        Map<String, String> d = pedido("99911110006", "Mel", 1, "14:00", "");
        d.put("idade", "4");
        service.criar(d);
        assertEquals(4, jdbc.queryForObject("SELECT idade FROM pet", Integer.class));
    }
}
