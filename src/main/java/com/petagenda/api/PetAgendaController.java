package com.petagenda.api;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;

/**
 * API do PetAgenda – tutor e agenda do dia.
 * (As telas do administrador – serviços, funcionários e bloqueios – estão em AdminController.)
 *
 * Rotas públicas (landing page e "Meus agendamentos"):
 *   GET  /api/servicos                     -> serviços ativos com preço, duração e responsável
 *   GET  /api/horarios-disponiveis         -> horários livres para um serviço em uma data (UC02)
 *   POST /api/agendamento                  -> cria agendamento com validação antichoque (UC01)
 *   POST /api/meus-agendamentos            -> histórico do tutor por telefone + código (UC04)
 *   POST /api/meus-agendamentos/cancelar   -> tutor cancela um agendamento (UC03)
 *   POST /api/login | POST /api/logout | GET /api/eu
 *
 * Rotas restritas (funcionário logado):
 *   GET  /api/agendamentos?data= | ?telefone=  -> agenda do dia (UC05/UC12) ou histórico do tutor
 *   PUT  /api/agendamento/{id}/status           -> muda o status seguindo RegraStatus (UC06)
 *   GET  /api/indicadores                       -> indicadores do dia e do mês (UC11)
 */
@RestController
@RequestMapping("/api")
public class PetAgendaController {

    private final JdbcTemplate jdbcTemplate;
    private final AgendamentoService agendamentoService;
    private final ControleAcesso acesso;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public PetAgendaController(JdbcTemplate jdbcTemplate, AgendamentoService agendamentoService, ControleAcesso acesso) {
        this.jdbcTemplate = jdbcTemplate;
        this.agendamentoService = agendamentoService;
        this.acesso = acesso;
    }

    // =====================================================================
    // ROTAS PÚBLICAS
    // =====================================================================

    @GetMapping("/servicos")
    public List<Map<String, Object>> listarServicos() {
        return jdbcTemplate.queryForList(
                "SELECT s.id_servico AS id, s.descricao, s.detalhes, s.icone, s.preco, s.duracaoMinutos, "
                        + "f.nome AS responsavel FROM servico s "
                        + "LEFT JOIN funcionario f ON f.id_funcionario = s.id_responsavel "
                        + "WHERE s.ativo = 1 ORDER BY s.id_servico");
    }

    @GetMapping("/horarios-disponiveis")
    public List<String> horariosDisponiveis(@RequestParam String data, @RequestParam int servico) {
        return agendamentoService.horariosLivres(lerData(data), servico);
    }

    /**
     * Cria um agendamento. "synchronized" impede que dois pedidos ao mesmo tempo
     * passem juntos pela verificação de conflito (suficiente para um único
     * servidor, que é o cenário do MVP). A transação fica dentro do serviço.
     */
    @PostMapping("/agendamento")
    public synchronized Map<String, Object> criarAgendamento(@RequestBody Map<String, String> dados) {
        return agendamentoService.criar(dados);
    }

    /**
     * UC04 – Histórico do tutor. Pede telefone E o código de um agendamento:
     * só o telefone deixaria qualquer pessoa ver a agenda de outra.
     */
    @PostMapping("/meus-agendamentos")
    public Map<String, Object> meusAgendamentos(@RequestBody Map<String, String> dados) {
        int idTutor = identificarTutor(dados.get("telefone"), dados.get("codigo"));
        LocalDateTime agora = agendamentoService.agora();

        List<Map<String, Object>> lista = jdbcTemplate.queryForList(
                "SELECT a.codigo, DATE_FORMAT(a.dataHora, '%Y-%m-%dT%H:%i') AS dataHora, a.dataHora AS inicio, "
                        + "a.status, s.descricao AS servico, s.duracaoMinutos, p.nome AS pet, f.nome AS responsavel "
                        + "FROM agendamento a JOIN servico s ON s.id_servico = a.id_servico "
                        + "JOIN pet p ON p.id_pet = a.id_pet "
                        + "LEFT JOIN funcionario f ON f.id_funcionario = a.id_funcionario "
                        + "WHERE a.id_tutor = ? ORDER BY a.dataHora DESC", idTutor);
        for (Map<String, Object> ag : lista) {
            LocalDateTime inicio = AgendamentoService.paraLocalDateTime(ag.remove("inicio"));
            ag.put("podeCancelar", RegraStatus.tutorPodeCancelar((String) ag.get("status")) && inicio.isAfter(agora));
        }
        String nome = jdbcTemplate.queryForObject("SELECT nome FROM tutor WHERE id_tutor = ?", String.class, idTutor);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("tutor", nome);
        resposta.put("agendamentos", lista);
        return resposta;
    }

    /** UC03 – O tutor cancela um agendamento seu que ainda não começou. */
    @PostMapping("/meus-agendamentos/cancelar")
    public Map<String, Object> cancelarPeloTutor(@RequestBody Map<String, String> dados) {
        int idTutor = identificarTutor(dados.get("telefone"), dados.get("codigo"));
        String alvo = AgendamentoService.limpar(dados.get("codigoCancelar")).toUpperCase();
        if (alvo.isEmpty()) {
            alvo = AgendamentoService.limpar(dados.get("codigo")).toUpperCase();
        }
        List<Map<String, Object>> r = jdbcTemplate.queryForList(
                "SELECT id_agendamento AS id, status, dataHora FROM agendamento WHERE codigo = ? AND id_tutor = ?",
                alvo, idTutor);
        if (r.isEmpty()) {
            throw new RegraNegocioException(HttpStatus.NOT_FOUND, "Agendamento não encontrado.");
        }
        Map<String, Object> ag = r.get(0);
        LocalDateTime inicio = AgendamentoService.paraLocalDateTime(ag.get("dataHora"));
        if (!RegraStatus.tutorPodeCancelar((String) ag.get("status")) || !inicio.isAfter(agendamentoService.agora())) {
            throw new RegraNegocioException(HttpStatus.CONFLICT,
                    "Este agendamento não pode mais ser cancelado pela internet. Fale com o pet shop.");
        }
        jdbcTemplate.update("UPDATE agendamento SET status = ? WHERE id_agendamento = ?",
                RegraStatus.CANCELADO, ag.get("id"));
        return Map.of("sucesso", true, "mensagem", "Agendamento cancelado. O horário foi liberado.");
    }

    @PostMapping("/login")
    public Map<String, Object> fazerLogin(@RequestBody Map<String, String> credenciais, HttpSession sessao) {
        String login = AgendamentoService.limpar(credenciais.get("login"));
        String senha = credenciais.get("senha") == null ? "" : credenciais.get("senha");

        List<Map<String, Object>> encontrados = jdbcTemplate.queryForList(
                "SELECT id_funcionario AS id, nome, tipo_perfil AS perfil, senha FROM funcionario "
                        + "WHERE login = ? AND ativo = 1", login);

        // A senha no banco está criptografada (BCrypt); "matches" compara com a senha digitada
        if (encontrados.isEmpty() || !encoder.matches(senha, (String) encontrados.get(0).get("senha"))) {
            throw new RegraNegocioException(HttpStatus.UNAUTHORIZED, "Login ou senha incorretos.");
        }
        Map<String, Object> f = encontrados.get(0);
        sessao.setAttribute(ControleAcesso.SESSAO_ID, f.get("id"));

        Map<String, Object> usuario = new LinkedHashMap<>();
        usuario.put("nome", f.get("nome"));
        usuario.put("perfil", f.get("perfil"));
        return usuario;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> fazerLogout(HttpSession sessao) {
        sessao.invalidate();
        return ResponseEntity.noContent().build();
    }

    /** Quem está logado (o painel usa para mostrar ou esconder as telas do administrador). */
    @GetMapping("/eu")
    public Map<String, Object> eu(HttpSession sessao) {
        return acesso.exigirLogin(sessao);
    }

    // =====================================================================
    // ROTAS RESTRITAS (funcionário logado)
    // =====================================================================

    /**
     * Agenda de um dia (?data=AAAA-MM-DD) ou histórico de um tutor (?telefone=...).
     * Sem filtro, devolve tudo.
     */
    @GetMapping("/agendamentos")
    public List<Map<String, Object>> listarAgendamentos(@RequestParam(required = false) String data,
                                                        @RequestParam(required = false) String telefone,
                                                        HttpSession sessao) {
        acesso.exigirLogin(sessao);
        String sql = "SELECT a.id_agendamento AS id, a.codigo, t.nome AS tutor, t.telefone, p.nome AS pet, "
                + "p.especie, p.raca, p.idade, p.restricoes_saude AS restricoes, s.descricao AS servico, "
                + "s.duracaoMinutos, DATE_FORMAT(a.dataHora, '%Y-%m-%dT%H:%i') AS dataHora, a.status, a.observacoes, "
                + "f.nome AS responsavel "
                + "FROM agendamento a "
                + "JOIN tutor t ON a.id_tutor = t.id_tutor "
                + "JOIN pet p ON a.id_pet = p.id_pet "
                + "JOIN servico s ON a.id_servico = s.id_servico "
                + "LEFT JOIN funcionario f ON a.id_funcionario = f.id_funcionario ";
        if (telefone != null && !telefone.isBlank()) {
            return jdbcTemplate.queryForList(sql + "WHERE t.telefone = ? ORDER BY a.dataHora DESC",
                    AgendamentoService.somenteDigitos(telefone));
        }
        if (data != null && !data.isBlank()) {
            LocalDate dia = lerData(data);
            return jdbcTemplate.queryForList(sql + "WHERE a.dataHora >= ? AND a.dataHora < ? ORDER BY a.dataHora",
                    dia.atStartOfDay(), dia.plusDays(1).atStartOfDay());
        }
        return jdbcTemplate.queryForList(sql + "ORDER BY a.dataHora");
    }

    /** UC06 – Atualizar status, respeitando o ciclo de vida (RegraStatus). */
    @PutMapping("/agendamento/{id}/status")
    public Map<String, Object> atualizarStatus(@PathVariable int id, @RequestBody Map<String, String> dados,
                                               HttpSession sessao) {
        acesso.exigirLogin(sessao);
        String novoStatus = dados.get("status");
        if (!RegraStatus.existe(novoStatus)) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Status inválido.");
        }
        List<String> atual = jdbcTemplate.queryForList(
                "SELECT status FROM agendamento WHERE id_agendamento = ?", String.class, id);
        if (atual.isEmpty()) {
            throw new RegraNegocioException(HttpStatus.NOT_FOUND, "Agendamento não encontrado.");
        }
        if (!RegraStatus.podeMudar(atual.get(0), novoStatus)) {
            throw new RegraNegocioException(HttpStatus.CONFLICT,
                    "Não é possível mudar de \"" + atual.get(0) + "\" para \"" + novoStatus + "\".");
        }
        jdbcTemplate.update("UPDATE agendamento SET status = ? WHERE id_agendamento = ?", novoStatus, id);
        return Map.of("sucesso", true);
    }

    /**
     * UC11 – Indicadores simples para o gestor.
     * - atendimentos do dia (sem cancelados) e concluídos
     * - ocupação da agenda = minutos agendados / minutos disponíveis
     *   (expediente × profissionais com serviço ativo × capacidade de cada um)
     * - faturamento do dia (somente concluídos) e do mês
     * - serviço mais procurado no mês
     */
    @GetMapping("/indicadores")
    public Map<String, Object> indicadores(@RequestParam(required = false) String data, HttpSession sessao) {
        acesso.exigirLogin(sessao);
        LocalDate dia = (data == null || data.isBlank()) ? agendamentoService.agora().toLocalDate() : lerData(data);
        LocalDate inicioMes = dia.withDayOfMonth(1);

        Map<String, Object> diaInfo = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS total, "
                        + "COALESCE(SUM(a.status = 'Concluído'), 0) AS concluidos, "
                        + "COALESCE(SUM(s.duracaoMinutos), 0) AS minutosOcupados, "
                        + "COALESCE(SUM(CASE WHEN a.status = 'Concluído' THEN s.preco ELSE 0 END), 0) AS faturamentoDia "
                        + "FROM agendamento a JOIN servico s ON a.id_servico = s.id_servico "
                        + "WHERE a.dataHora >= ? AND a.dataHora < ? AND a.status <> 'Cancelado'",
                dia.atStartOfDay(), dia.plusDays(1).atStartOfDay());

        Map<String, Object> mesInfo = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS totalMes, "
                        + "COALESCE(SUM(CASE WHEN a.status = 'Concluído' THEN s.preco ELSE 0 END), 0) AS faturamentoMes "
                        + "FROM agendamento a JOIN servico s ON a.id_servico = s.id_servico "
                        + "WHERE a.dataHora >= ? AND a.dataHora < ? AND a.status <> 'Cancelado'",
                inicioMes.atStartOfDay(), inicioMes.plusMonths(1).atStartOfDay());

        List<Map<String, Object>> maisProcurado = jdbcTemplate.queryForList(
                "SELECT s.descricao, COUNT(*) AS quantidade "
                        + "FROM agendamento a JOIN servico s ON a.id_servico = s.id_servico "
                        + "WHERE a.dataHora >= ? AND a.dataHora < ? AND a.status <> 'Cancelado' "
                        + "GROUP BY s.descricao ORDER BY quantidade DESC LIMIT 1",
                inicioMes.atStartOfDay(), inicioMes.plusMonths(1).atStartOfDay());

        // Cada profissional com serviço ativo é uma "agenda"; serviços sem responsável contam como uma agenda só
        Integer agendas = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT COALESCE(id_responsavel, 0)) FROM servico WHERE ativo = 1", Integer.class);
        long minutosDisponiveis = Duration.between(AgendamentoService.ABERTURA, AgendamentoService.FECHAMENTO).toMinutes()
                * Math.max(1, agendas == null ? 1 : agendas) * agendamentoService.getCapacidadeSimultanea();
        long minutosOcupados = ((Number) diaInfo.get("minutosOcupados")).longValue();

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("data", dia.toString());
        resposta.put("atendimentosDia", diaInfo.get("total"));
        resposta.put("concluidosDia", diaInfo.get("concluidos"));
        resposta.put("ocupacaoPercentual", Math.round(minutosOcupados * 100.0 / minutosDisponiveis));
        resposta.put("faturamentoDia", diaInfo.get("faturamentoDia"));
        resposta.put("atendimentosMes", mesInfo.get("totalMes"));
        resposta.put("faturamentoMes", mesInfo.get("faturamentoMes"));
        resposta.put("servicoMaisProcurado", maisProcurado.isEmpty() ? "-" : maisProcurado.get(0).get("descricao"));
        return resposta;
    }

    // =====================================================================
    // AUXILIARES
    // =====================================================================

    /** Confere telefone + código e devolve o id do tutor. Mensagem única para não revelar qual dos dois errou. */
    private int identificarTutor(String telefone, String codigo) {
        String tel = AgendamentoService.somenteDigitos(telefone);
        String cod = AgendamentoService.limpar(codigo).toUpperCase();
        List<Integer> r = jdbcTemplate.queryForList(
                "SELECT a.id_tutor FROM agendamento a JOIN tutor t ON t.id_tutor = a.id_tutor "
                        + "WHERE a.codigo = ? AND t.telefone = ?", Integer.class, cod, tel);
        if (r.isEmpty()) {
            throw new RegraNegocioException(HttpStatus.NOT_FOUND,
                    "Não encontramos agendamento com esse telefone e código. Confira e tente de novo.");
        }
        return r.get(0);
    }

    static LocalDate lerData(String data) {
        try {
            return LocalDate.parse(data);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Data inválida.");
        }
    }
}
