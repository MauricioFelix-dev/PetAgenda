package com.petagenda.api;

import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Camada de serviço (regras de negócio) do agendamento.
 *
 * MOTOR ANTICHOQUE (Etapa 3):
 * cada serviço tem um profissional responsável (ex.: Ana faz banho e tosa, a
 * veterinária faz as consultas). Dois atendimentos só entram em conflito quando
 * são do MESMO profissional e os horários se sobrepõem, considerando a duração.
 * Assim, um banho com a Ana e uma consulta com a veterinária podem acontecer no
 * mesmo horário. Serviços sem responsável definido formam um grupo único
 * ("equipe"), que é o comportamento da Etapa 2.
 *
 * Também respeita os bloqueios de agenda (UC10): do pet shop inteiro ou de um
 * profissional.
 */
@Service
public class AgendamentoService {

    /** Expediente do pet shop. Os horários oferecidos vão de 30 em 30 minutos. */
    static final LocalTime ABERTURA = LocalTime.of(8, 0);
    static final LocalTime FECHAMENTO = LocalTime.of(18, 0);
    static final int INTERVALO_MINUTOS = 30;

    /** Tamanho máximo dos textos livres (igual ao maxlength dos campos em index.html). */
    static final int LIMITE_RESTRICOES = 500;
    static final int LIMITE_OBS = 1000;

    /** Letras e números sem os que confundem (0/O, 1/I/L). */
    private static final String ALFABETO_CODIGO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom SORTEIO = new SecureRandom();

    private final JdbcTemplate jdbcTemplate;

    /**
     * Quantos atendimentos cada profissional pode fazer ao mesmo tempo
     * (padrão 1). Configurado em application.properties.
     */
    private final int capacidadeSimultanea;

    /** Fuso do pet shop, para saber "que horas são agora" (correção de 14/11). */
    private final ZoneId fusoHorario;

    @Autowired
    public AgendamentoService(JdbcTemplate jdbcTemplate,
                              @Value("${petagenda.capacidade-simultanea:1}") int capacidadeSimultanea,
                              @Value("${petagenda.fuso-horario:America/Sao_Paulo}") String fusoHorario) {
        this.jdbcTemplate = jdbcTemplate;
        this.capacidadeSimultanea = capacidadeSimultanea;
        this.fusoHorario = ZoneId.of(fusoHorario);
    }

    public int getCapacidadeSimultanea() {
        return capacidadeSimultanea;
    }

    public LocalDateTime agora() {
        return LocalDateTime.now(fusoHorario);
    }

    // =====================================================================
    // CONSULTA DE HORÁRIOS LIVRES (UC02)
    // =====================================================================

    public List<String> horariosLivres(LocalDate dia, int idServico) {
        Map<String, Object> servico = buscarServicoAtivo(idServico);
        int duracao = ((Number) servico.get("duracaoMinutos")).intValue();
        Integer responsavel = (Integer) servico.get("idResponsavel");

        List<Map<String, Object>> ocupados = buscarAgendamentosAtivosDoDia(dia);
        List<Map<String, Object>> bloqueios = buscarBloqueiosDoDia(dia);
        LocalDateTime agora = agora();

        List<String> livres = new ArrayList<>();
        for (LocalTime t = ABERTURA; !t.plusMinutes(duracao).isAfter(FECHAMENTO); t = t.plusMinutes(INTERVALO_MINUTOS)) {
            LocalDateTime inicio = LocalDateTime.of(dia, t);
            if (inicio.isBefore(agora)) {
                continue; // não oferece horário que já passou
            }
            if (estaLivre(inicio, duracao, responsavel, ocupados, bloqueios)) {
                livres.add(t.toString()); // formato HH:mm
            }
        }
        return livres;
    }

    // =====================================================================
    // CRIAÇÃO DO AGENDAMENTO (UC01) – TRANSAÇÃO
    // =====================================================================

    /**
     * Cria o agendamento dentro de uma TRANSAÇÃO (@Transactional): tutor, pet e
     * agendamento são gravados juntos ou nada é gravado. Se qualquer passo
     * falhar (erro do banco ou RegraNegocioException), o banco desfaz tudo
     * (rollback). Correção (auditoria 17/11/2026): antes, se a gravação do
     * agendamento falhasse, o tutor e o pet ficavam gravados mesmo assim.
     *
     * Quem chama (o controller) é "synchronized": dois pedidos ao mesmo tempo
     * não passam juntos pela verificação de conflito. A ordem importa: a trava
     * fica por fora e a transação por dentro, para que a transação termine
     * (commit) antes de o próximo pedido conferir a agenda.
     */
    @Transactional
    public Map<String, Object> criar(Map<String, String> dados) {
        // 1. Validação de entrada
        String nomeTutor = limpar(dados.get("tutor"));
        String telefone = somenteDigitos(dados.get("telefone"));
        String nomePet = limpar(dados.get("pet"));
        String especie = limpar(dados.get("especie"));
        String raca = limpar(dados.get("raca"));
        String restricoes = limpar(dados.get("restricoes"));
        String obs = limpar(dados.get("obs"));
        Integer idade = lerIdade(dados.get("idade"));

        if (nomeTutor.isEmpty() || nomePet.isEmpty()) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Informe o nome do tutor e do pet.");
        }
        // Mesmos limites das colunas do banco: sem isto, um texto longo virava
        // "Falha ao gravar no banco" (achado da exploração de 18/11/2026).
        if (nomeTutor.length() > 100 || nomePet.length() > 50 || raca.length() > 50 || especie.length() > 30) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST,
                    "Texto longo demais: nome do tutor até 100 caracteres; nome do pet e raça até 50; espécie até 30.");
        }
        // Mesmos limites dos campos da página (revisão de 21/11/2026): antes, um texto
        // enorme enviado direto à API passava daqui e só falhava no banco (erro 500).
        if (restricoes.length() > LIMITE_RESTRICOES || obs.length() > LIMITE_OBS) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST,
                    "Texto longo demais: restrições de saúde até " + LIMITE_RESTRICOES
                            + " caracteres e observações até " + LIMITE_OBS + ".");
        }
        if (telefone.length() < 10 || telefone.length() > 11) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Telefone inválido. Use DDD + número.");
        }

        int idServico;
        LocalDateTime inicio;
        try {
            idServico = Integer.parseInt(dados.get("servico"));
            inicio = LocalDateTime.of(LocalDate.parse(dados.get("data")), LocalTime.parse(dados.get("horario")));
        } catch (NumberFormatException | DateTimeParseException | NullPointerException e) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Serviço, data ou horário inválidos.");
        }

        Map<String, Object> servico = buscarServicoAtivo(idServico);
        int duracao = ((Number) servico.get("duracaoMinutos")).intValue();
        Integer responsavel = (Integer) servico.get("idResponsavel");

        if (inicio.isBefore(agora())) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Não é possível agendar em data ou horário que já passou.");
        }
        LocalTime horaInicio = inicio.toLocalTime();
        if (horaInicio.isBefore(ABERTURA) || horaInicio.plusMinutes(duracao).isAfter(FECHAMENTO)) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Horário fora do expediente (08:00 às 18:00).");
        }

        // 2. MOTOR ANTICHOQUE: mesmo profissional + sobreposição de horário, e bloqueios
        LocalDate dia = inicio.toLocalDate();
        if (!estaLivre(inicio, duracao, responsavel, buscarAgendamentosAtivosDoDia(dia), buscarBloqueiosDoDia(dia))) {
            throw new RegraNegocioException(HttpStatus.CONFLICT,
                    "Este horário já está ocupado. Escolha outro horário disponível.");
        }

        // 3. Reaproveita ou cria o tutor (telefone é único)
        int idTutor = buscarOuCriarTutor(nomeTutor, telefone);

        // 4. Reaproveita ou cria o pet (mesmo nome para o mesmo tutor)
        int idPet = buscarOuCriarPet(nomePet, especie, raca, restricoes, idade, idTutor);

        // 5. Grava o agendamento com o profissional responsável e um código para o tutor
        // (o horário vai como LocalDateTime, sem conversão de fuso – correção CT-02/CT-03 de 14/11)
        String codigo = gerarCodigoUnico();
        jdbcTemplate.update(
                "INSERT INTO agendamento (dataHora, observacoes, status, codigo, id_tutor, id_pet, id_servico, id_funcionario) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                inicio, vazioParaNulo(obs), RegraStatus.PENDENTE, codigo, idTutor, idPet, idServico, responsavel);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("sucesso", true);
        resposta.put("mensagem", "Solicitação registrada para " + dia + " às " + horaInicio + ".");
        resposta.put("codigo", codigo);
        resposta.put("responsavel", servico.get("responsavel"));
        resposta.put("servico", servico.get("descricao"));
        return resposta;
    }

    // =====================================================================
    // REGRAS AUXILIARES
    // =====================================================================

    /**
     * Um horário está livre quando:
     *  (a) não cai em nenhum bloqueio do pet shop nem do profissional; e
     *  (b) o profissional tem menos atendimentos sobrepostos que a capacidade.
     */
    boolean estaLivre(LocalDateTime inicio, int duracao, Integer responsavel,
                      List<Map<String, Object>> ocupados, List<Map<String, Object>> bloqueios) {
        LocalDateTime fim = inicio.plusMinutes(duracao);
        for (Map<String, Object> b : bloqueios) {
            Object dono = b.get("idFuncionario");
            boolean seAplica = dono == null || (responsavel != null && responsavel.equals(((Number) dono).intValue()));
            if (seAplica && RegraConflito.intervalosSobrepoem(inicio, fim,
                    paraLocalDateTime(b.get("inicio")), paraLocalDateTime(b.get("fim")))) {
                return false;
            }
        }
        int conflitos = 0;
        for (Map<String, Object> ag : ocupados) {
            Integer profissional = ag.get("idFuncionario") == null ? null : ((Number) ag.get("idFuncionario")).intValue();
            if (!Objects.equals(profissional, responsavel)) {
                continue; // outro profissional: não disputa o mesmo horário
            }
            if (RegraConflito.haSobreposicao(inicio, duracao,
                    paraLocalDateTime(ag.get("dataHora")), ((Number) ag.get("duracaoMinutos")).intValue())) {
                conflitos++;
            }
        }
        return conflitos < capacidadeSimultanea;
    }

    /** Serviço ativo com duração e responsável; 400 se não existir ou estiver desativado. */
    Map<String, Object> buscarServicoAtivo(int idServico) {
        List<Map<String, Object>> r = jdbcTemplate.queryForList(
                "SELECT s.descricao, s.duracaoMinutos, s.id_responsavel AS idResponsavel, f.nome AS responsavel "
                        + "FROM servico s LEFT JOIN funcionario f ON f.id_funcionario = s.id_responsavel "
                        + "WHERE s.id_servico = ? AND s.ativo = 1", idServico);
        if (r.isEmpty()) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Serviço inexistente ou indisponível.");
        }
        Map<String, Object> s = r.get(0);
        if (s.get("idResponsavel") != null) {
            s.put("idResponsavel", ((Number) s.get("idResponsavel")).intValue());
        }
        return s;
    }

    /** Agendamentos não cancelados do dia, com início, duração e profissional. */
    List<Map<String, Object>> buscarAgendamentosAtivosDoDia(LocalDate dia) {
        return jdbcTemplate.queryForList(
                "SELECT a.dataHora, s.duracaoMinutos, a.id_funcionario AS idFuncionario FROM agendamento a "
                        + "JOIN servico s ON a.id_servico = s.id_servico "
                        + "WHERE a.dataHora >= ? AND a.dataHora < ? AND a.status <> ?",
                dia.atStartOfDay(), dia.plusDays(1).atStartOfDay(), RegraStatus.CANCELADO);
    }

    /** Bloqueios que tocam o dia (podem começar antes ou terminar depois). */
    List<Map<String, Object>> buscarBloqueiosDoDia(LocalDate dia) {
        return jdbcTemplate.queryForList(
                "SELECT inicio, fim, id_funcionario AS idFuncionario FROM bloqueio WHERE inicio < ? AND fim > ?",
                dia.plusDays(1).atStartOfDay(), dia.atStartOfDay());
    }

    private String gerarCodigoUnico() {
        for (int tentativa = 0; tentativa < 20; tentativa++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(ALFABETO_CODIGO.charAt(SORTEIO.nextInt(ALFABETO_CODIGO.length())));
            }
            String codigo = sb.toString();
            Integer existe = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM agendamento WHERE codigo = ?", Integer.class, codigo);
            if (existe != null && existe == 0) {
                return codigo;
            }
        }
        throw new IllegalStateException("Não foi possível gerar um código único.");
    }

    private int buscarOuCriarTutor(String nome, String telefone) {
        List<Integer> existentes = jdbcTemplate.queryForList(
                "SELECT id_tutor FROM tutor WHERE telefone = ?", Integer.class, telefone);
        if (!existentes.isEmpty()) {
            return existentes.get(0);
        }
        return inserirERetornarId("INSERT INTO tutor (nome, telefone) VALUES (?, ?)", nome, telefone);
    }

    private int buscarOuCriarPet(String nome, String especie, String raca, String restricoes, Integer idade, int idTutor) {
        List<Integer> existentes = jdbcTemplate.queryForList(
                "SELECT id_pet FROM pet WHERE nome = ? AND id_tutor = ?", Integer.class, nome, idTutor);
        if (!existentes.isEmpty()) {
            int idPet = existentes.get(0);
            // Se o tutor informou novas restrições de saúde ou a idade, atualiza o cadastro do pet
            if (!restricoes.isEmpty()) {
                jdbcTemplate.update("UPDATE pet SET restricoes_saude = ? WHERE id_pet = ?", restricoes, idPet);
            }
            if (idade != null) {
                jdbcTemplate.update("UPDATE pet SET idade = ? WHERE id_pet = ?", idade, idPet);
            }
            return idPet;
        }
        return inserirERetornarId(
                "INSERT INTO pet (nome, especie, raca, idade, restricoes_saude, id_tutor) VALUES (?, ?, ?, ?, ?, ?)",
                nome, vazioParaNulo(especie), vazioParaNulo(raca), idade, vazioParaNulo(restricoes), idTutor);
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

    /** Idade do pet é opcional; se vier, precisa ser um número entre 0 e 40. */
    private static Integer lerIdade(String texto) {
        String t = limpar(texto);
        if (t.isEmpty()) {
            return null;
        }
        try {
            int idade = Integer.parseInt(t);
            if (idade >= 0 && idade <= 40) {
                return idade;
            }
        } catch (NumberFormatException e) {
            // cai no erro abaixo
        }
        throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "Idade do pet inválida (use anos, de 0 a 40).");
    }

    /** O driver do MySQL pode devolver Timestamp ou LocalDateTime, conforme a versão. */
    static LocalDateTime paraLocalDateTime(Object valor) {
        if (valor instanceof LocalDateTime ldt) {
            return ldt;
        }
        return ((Timestamp) valor).toLocalDateTime();
    }

    static String limpar(String texto) {
        return texto == null ? "" : texto.trim();
    }

    static String vazioParaNulo(String texto) {
        return texto == null || texto.isEmpty() ? null : texto;
    }

    /**
     * Deixa só os dígitos do telefone, para que "(99) 98888-7777" e "99988887777"
     * virem o mesmo número (e o tutor não seja duplicado). Remove o "55" do Brasil
     * quando o número vem com 12 ou 13 dígitos (correção CT-05).
     */
    static String somenteDigitos(String texto) {
        String digitos = texto == null ? "" : texto.replaceAll("\\D", "");
        if ((digitos.length() == 12 || digitos.length() == 13) && digitos.startsWith("55")) {
            digitos = digitos.substring(2);
        }
        return digitos;
    }
}
