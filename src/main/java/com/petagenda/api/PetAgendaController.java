package com.petagenda.api;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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
 * API do PetAgenda.
 *
 * Rotas públicas (landing page):
 *   GET  /api/servicos                 -> lista de serviços com preço e duração
 *   GET  /api/horarios-disponiveis     -> horários livres para um serviço em uma data
 *   POST /api/agendamento              -> cria agendamento (com validação antichoque)
 *   POST /api/login | POST /api/logout
 *
 * Rotas restritas (exigem login de funcionário):
 *   GET  /api/agendamentos             -> lista agendamentos (filtro opcional por data)
 *   PUT  /api/agendamento/{id}/status  -> altera status
 *   GET  /api/indicadores              -> indicadores do dia e do mês
 */
@RestController
@RequestMapping("/api")
public class PetAgendaController {

    /** Chave usada para guardar o funcionário logado na sessão do servidor. */
    static final String SESSAO_USUARIO = "funcionarioLogado";

    /** Expediente do pet shop. Os horários oferecidos vão de 30 em 30 minutos. */
    static final LocalTime ABERTURA = LocalTime.of(8, 0);
    static final LocalTime FECHAMENTO = LocalTime.of(18, 0);
    static final int INTERVALO_MINUTOS = 30;

    static final Set<String> STATUS_VALIDOS = Set.of("Pendente", "Em andamento", "Concluído", "Cancelado");

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * Quantos atendimentos podem acontecer ao mesmo tempo
     * (ex.: 2 se houver dois banhistas). Configurado em application.properties.
     */
    private final int capacidadeSimultanea;

    public PetAgendaController(JdbcTemplate jdbcTemplate,
                               @Value("${petagenda.capacidade-simultanea:1}") int capacidadeSimultanea) {
        this.jdbcTemplate = jdbcTemplate;
        this.capacidadeSimultanea = capacidadeSimultanea;
    }

    // =====================================================================
    // ROTAS PÚBLICAS
    // =====================================================================

    @GetMapping("/servicos")
    public List<Map<String, Object>> listarServicos() {
        return jdbcTemplate.queryForList(
                "SELECT id_servico AS id, descricao, preco, duracaoMinutos FROM servico ORDER BY id_servico");
    }

    @GetMapping("/horarios-disponiveis")
    public ResponseEntity<?> horariosDisponiveis(@RequestParam String data, @RequestParam int servico) {
        LocalDate dia;
        try {
            dia = LocalDate.parse(data);
        } catch (DateTimeParseException e) {
            return erro(HttpStatus.BAD_REQUEST, "Data inválida.");
        }
        Integer duracao = buscarDuracao(servico);
        if (duracao == null) {
            return erro(HttpStatus.BAD_REQUEST, "Serviço inexistente.");
        }

        List<Map<String, Object>> ocupados = buscarAgendamentosAtivosDoDia(dia);
        List<String> livres = new ArrayList<>();
        LocalDateTime agora = LocalDateTime.now();

        for (LocalTime t = ABERTURA; !t.plusMinutes(duracao).isAfter(FECHAMENTO); t = t.plusMinutes(INTERVALO_MINUTOS)) {
            LocalDateTime inicio = LocalDateTime.of(dia, t);
            if (inicio.isBefore(agora)) {
                continue; // não oferece horário que já passou
            }
            if (contarConflitos(inicio, duracao, ocupados) < capacidadeSimultanea) {
                livres.add(t.toString()); // formato HH:mm
            }
        }
        return ResponseEntity.ok(livres);
    }

    /**
     * Cria um agendamento. É "synchronized" para que duas solicitações ao mesmo
     * tempo não passem juntas pela verificação de conflito (suficiente para um
     * único servidor, que é o cenário do MVP).
     */
    @PostMapping("/agendamento")
    public synchronized ResponseEntity<Map<String, Object>> criarAgendamento(@RequestBody Map<String, String> dados) {
        // 1. Validação de entrada
        String nomeTutor = limpar(dados.get("tutor"));
        String telefone = somenteDigitos(dados.get("telefone"));
        String nomePet = limpar(dados.get("pet"));
        String especie = limpar(dados.get("especie"));
        String raca = limpar(dados.get("raca"));
        String restricoes = limpar(dados.get("restricoes"));
        String obs = limpar(dados.get("obs"));

        if (nomeTutor.isEmpty() || nomePet.isEmpty()) {
            return erro(HttpStatus.BAD_REQUEST, "Informe o nome do tutor e do pet.");
        }
        if (telefone.length() < 10 || telefone.length() > 11) {
            return erro(HttpStatus.BAD_REQUEST, "Telefone inválido. Use DDD + número.");
        }

        int idServico;
        LocalDateTime inicio;
        try {
            idServico = Integer.parseInt(dados.get("servico"));
            inicio = LocalDateTime.of(LocalDate.parse(dados.get("data")), LocalTime.parse(dados.get("horario")));
        } catch (NumberFormatException | DateTimeParseException | NullPointerException e) {
            return erro(HttpStatus.BAD_REQUEST, "Serviço, data ou horário inválidos.");
        }

        Integer duracao = buscarDuracao(idServico);
        if (duracao == null) {
            return erro(HttpStatus.BAD_REQUEST, "Serviço inexistente.");
        }
        if (inicio.isBefore(LocalDateTime.now())) {
            return erro(HttpStatus.BAD_REQUEST, "Não é possível agendar em data ou horário que já passou.");
        }
        LocalTime horaInicio = inicio.toLocalTime();
        if (horaInicio.isBefore(ABERTURA) || horaInicio.plusMinutes(duracao).isAfter(FECHAMENTO)) {
            return erro(HttpStatus.BAD_REQUEST, "Horário fora do expediente (08:00 às 18:00).");
        }

        try {
            // 2. MOTOR ANTICHOQUE: verifica sobreposição com os agendamentos ativos do dia
            List<Map<String, Object>> ocupados = buscarAgendamentosAtivosDoDia(inicio.toLocalDate());
            if (contarConflitos(inicio, duracao, ocupados) >= capacidadeSimultanea) {
                return erro(HttpStatus.CONFLICT,
                        "Este horário já está ocupado. Escolha outro horário disponível.");
            }

            // 3. Reaproveita ou cria o tutor (telefone é único)
            int idTutor = buscarOuCriarTutor(nomeTutor, telefone);

            // 4. Reaproveita ou cria o pet (mesmo nome para o mesmo tutor)
            int idPet = buscarOuCriarPet(nomePet, especie, raca, restricoes, idTutor);

            // 5. Grava o agendamento
            jdbcTemplate.update(
                    "INSERT INTO agendamento (dataHora, observacoes, status, id_tutor, id_pet, id_servico) "
                            + "VALUES (?, ?, 'Pendente', ?, ?, ?)",
                    Timestamp.valueOf(inicio), obs, idTutor, idPet, idServico);

            Map<String, Object> resposta = new LinkedHashMap<>();
            resposta.put("sucesso", true);
            resposta.put("mensagem", "Solicitação registrada para " + inicio.toLocalDate() + " às " + horaInicio + ".");
            return ResponseEntity.ok(resposta);

        } catch (DataAccessException e) {
            return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao gravar no banco de dados.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> fazerLogin(@RequestBody Map<String, String> credenciais, HttpSession sessao) {
        String login = limpar(credenciais.get("login"));
        String senha = credenciais.get("senha") == null ? "" : credenciais.get("senha");

        List<Map<String, Object>> encontrados = jdbcTemplate.queryForList(
                "SELECT nome, tipo_perfil, senha FROM funcionario WHERE login = ?", login);

        // A senha no banco está criptografada (BCrypt); "matches" compara com a senha digitada
        if (encontrados.isEmpty() || !encoder.matches(senha, (String) encontrados.get(0).get("senha"))) {
            return erro(HttpStatus.UNAUTHORIZED, "Login ou senha incorretos.");
        }

        Map<String, Object> usuario = new LinkedHashMap<>();
        usuario.put("nome", encontrados.get(0).get("nome"));
        usuario.put("perfil", encontrados.get(0).get("tipo_perfil"));
        sessao.setAttribute(SESSAO_USUARIO, usuario);
        return ResponseEntity.ok(usuario);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> fazerLogout(HttpSession sessao) {
        sessao.invalidate();
        return ResponseEntity.noContent().build();
    }

    // =====================================================================
    // ROTAS RESTRITAS (somente funcionário logado)
    // =====================================================================

    @GetMapping("/agendamentos")
    public ResponseEntity<?> listarAgendamentos(@RequestParam(required = false) String data, HttpSession sessao) {
        if (!estaLogado(sessao)) {
            return erro(HttpStatus.UNAUTHORIZED, "Faça login para acessar.");
        }
        String sql = "SELECT a.id_agendamento AS id, t.nome AS tutor, t.telefone, p.nome AS pet, "
                + "p.especie, p.raca, p.restricoes_saude AS restricoes, s.descricao AS servico, "
                + "s.duracaoMinutos, DATE_FORMAT(a.dataHora, '%Y-%m-%dT%H:%i') AS dataHora, a.status, a.observacoes "
                + "FROM agendamento a "
                + "JOIN tutor t ON a.id_tutor = t.id_tutor "
                + "JOIN pet p ON a.id_pet = p.id_pet "
                + "JOIN servico s ON a.id_servico = s.id_servico ";
        try {
            if (data != null && !data.isBlank()) {
                LocalDate dia = LocalDate.parse(data);
                return ResponseEntity.ok(jdbcTemplate.queryForList(
                        sql + "WHERE DATE(a.dataHora) = ? ORDER BY a.dataHora", dia.toString()));
            }
            return ResponseEntity.ok(jdbcTemplate.queryForList(sql + "ORDER BY a.dataHora"));
        } catch (DateTimeParseException e) {
            return erro(HttpStatus.BAD_REQUEST, "Data inválida.");
        }
    }

    @PutMapping("/agendamento/{id}/status")
    public ResponseEntity<Map<String, Object>> atualizarStatus(@PathVariable int id,
                                                               @RequestBody Map<String, String> dados,
                                                               HttpSession sessao) {
        if (!estaLogado(sessao)) {
            return erro(HttpStatus.UNAUTHORIZED, "Faça login para acessar.");
        }
        String novoStatus = dados.get("status");
        if (novoStatus == null || !STATUS_VALIDOS.contains(novoStatus)) {
            return erro(HttpStatus.BAD_REQUEST, "Status inválido.");
        }
        int linhas = jdbcTemplate.update("UPDATE agendamento SET status = ? WHERE id_agendamento = ?", novoStatus, id);
        if (linhas == 0) {
            return erro(HttpStatus.NOT_FOUND, "Agendamento não encontrado.");
        }
        return ResponseEntity.ok(Map.of("sucesso", true));
    }

    /**
     * Indicadores simples para o gestor (RF05).
     * - atendimentos do dia (sem cancelados) e concluídos
     * - taxa de ocupação da agenda no dia = minutos agendados / minutos disponíveis
     * - faturamento do dia (somente concluídos) e do mês
     * - serviço mais procurado no mês
     */
    @GetMapping("/indicadores")
    public ResponseEntity<?> indicadores(@RequestParam(required = false) String data, HttpSession sessao) {
        if (!estaLogado(sessao)) {
            return erro(HttpStatus.UNAUTHORIZED, "Faça login para acessar.");
        }
        LocalDate dia;
        try {
            dia = (data == null || data.isBlank()) ? LocalDate.now() : LocalDate.parse(data);
        } catch (DateTimeParseException e) {
            return erro(HttpStatus.BAD_REQUEST, "Data inválida.");
        }

        Map<String, Object> diaInfo = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS total, "
                        + "COALESCE(SUM(a.status = 'Concluído'), 0) AS concluidos, "
                        + "COALESCE(SUM(s.duracaoMinutos), 0) AS minutosOcupados, "
                        + "COALESCE(SUM(CASE WHEN a.status = 'Concluído' THEN s.preco ELSE 0 END), 0) AS faturamentoDia "
                        + "FROM agendamento a JOIN servico s ON a.id_servico = s.id_servico "
                        + "WHERE DATE(a.dataHora) = ? AND a.status <> 'Cancelado'",
                dia.toString());

        Map<String, Object> mesInfo = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS totalMes, "
                        + "COALESCE(SUM(CASE WHEN a.status = 'Concluído' THEN s.preco ELSE 0 END), 0) AS faturamentoMes "
                        + "FROM agendamento a JOIN servico s ON a.id_servico = s.id_servico "
                        + "WHERE YEAR(a.dataHora) = ? AND MONTH(a.dataHora) = ? AND a.status <> 'Cancelado'",
                dia.getYear(), dia.getMonthValue());

        List<Map<String, Object>> maisProcurado = jdbcTemplate.queryForList(
                "SELECT s.descricao, COUNT(*) AS quantidade "
                        + "FROM agendamento a JOIN servico s ON a.id_servico = s.id_servico "
                        + "WHERE YEAR(a.dataHora) = ? AND MONTH(a.dataHora) = ? AND a.status <> 'Cancelado' "
                        + "GROUP BY s.descricao ORDER BY quantidade DESC LIMIT 1",
                dia.getYear(), dia.getMonthValue());

        long minutosDisponiveis = (long) java.time.Duration.between(ABERTURA, FECHAMENTO).toMinutes() * capacidadeSimultanea;
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
        return ResponseEntity.ok(resposta);
    }

    // =====================================================================
    // MÉTODOS AUXILIARES
    // =====================================================================

    private boolean estaLogado(HttpSession sessao) {
        return sessao.getAttribute(SESSAO_USUARIO) != null;
    }

    private Integer buscarDuracao(int idServico) {
        List<Integer> r = jdbcTemplate.queryForList(
                "SELECT duracaoMinutos FROM servico WHERE id_servico = ?", Integer.class, idServico);
        return r.isEmpty() ? null : r.get(0);
    }

    /** Agendamentos não cancelados do dia, com início e duração. */
    private List<Map<String, Object>> buscarAgendamentosAtivosDoDia(LocalDate dia) {
        return jdbcTemplate.queryForList(
                "SELECT a.dataHora, s.duracaoMinutos FROM agendamento a "
                        + "JOIN servico s ON a.id_servico = s.id_servico "
                        + "WHERE DATE(a.dataHora) = ? AND a.status <> 'Cancelado'",
                dia.toString());
    }

    /** Conta quantos agendamentos existentes se sobrepõem ao intervalo pedido. */
    private int contarConflitos(LocalDateTime inicio, int duracao, List<Map<String, Object>> ocupados) {
        int conflitos = 0;
        for (Map<String, Object> ag : ocupados) {
            LocalDateTime inicioExistente = paraLocalDateTime(ag.get("dataHora"));
            int duracaoExistente = ((Number) ag.get("duracaoMinutos")).intValue();
            if (RegraConflito.haSobreposicao(inicio, duracao, inicioExistente, duracaoExistente)) {
                conflitos++;
            }
        }
        return conflitos;
    }

    /** O driver do MySQL pode devolver Timestamp ou LocalDateTime, conforme a versão. */
    private LocalDateTime paraLocalDateTime(Object valor) {
        if (valor instanceof LocalDateTime ldt) {
            return ldt;
        }
        return ((Timestamp) valor).toLocalDateTime();
    }

    private int buscarOuCriarTutor(String nome, String telefone) {
        List<Integer> existentes = jdbcTemplate.queryForList(
                "SELECT id_tutor FROM tutor WHERE telefone = ?", Integer.class, telefone);
        if (!existentes.isEmpty()) {
            return existentes.get(0);
        }
        return inserirERetornarId("INSERT INTO tutor (nome, telefone) VALUES (?, ?)", nome, telefone);
    }

    private int buscarOuCriarPet(String nome, String especie, String raca, String restricoes, int idTutor) {
        List<Integer> existentes = jdbcTemplate.queryForList(
                "SELECT id_pet FROM pet WHERE nome = ? AND id_tutor = ?", Integer.class, nome, idTutor);
        if (!existentes.isEmpty()) {
            int idPet = existentes.get(0);
            // Se o tutor informou novas restrições de saúde, atualiza o cadastro do pet
            if (!restricoes.isEmpty()) {
                jdbcTemplate.update("UPDATE pet SET restricoes_saude = ? WHERE id_pet = ?", restricoes, idPet);
            }
            return idPet;
        }
        return inserirERetornarId(
                "INSERT INTO pet (nome, especie, raca, restricoes_saude, id_tutor) VALUES (?, ?, ?, ?, ?)",
                nome, vazioParaNulo(especie), vazioParaNulo(raca), vazioParaNulo(restricoes), idTutor);
    }

    private int inserirERetornarId(String sql, Object... parametros) {
        KeyHolder chave = new GeneratedKeyHolder();
        jdbcTemplate.update(conexao -> {
            PreparedStatement ps = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            return ps;
        }, chave);
        return chave.getKey().intValue();
    }

    private static String limpar(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private static String vazioParaNulo(String texto) {
        return texto.isEmpty() ? null : texto;
    }

    /**
     * Deixa só os dígitos do telefone, para que "(99) 98888-7777" e "99988887777"
     * virem o mesmo número (e o tutor não seja duplicado).
     * Correção (teste CT-05): se a pessoa digitar com o código do Brasil
     * (ex.: "+55 99 98888-7777", comum ao copiar do WhatsApp), o número ficava com
     * 12 ou 13 dígitos e era recusado. Agora o "55" do início é removido nesse caso.
     */
    private static String somenteDigitos(String texto) {
        String digitos = texto == null ? "" : texto.replaceAll("\\D", "");
        if ((digitos.length() == 12 || digitos.length() == 13) && digitos.startsWith("55")) {
            digitos = digitos.substring(2);
        }
        return digitos;
    }

    private static <T> ResponseEntity<T> erro(HttpStatus status, String mensagem) {
        @SuppressWarnings("unchecked")
        T corpo = (T) Map.of("erro", mensagem);
        return ResponseEntity.status(status).body(corpo);
    }
}
