package com.petagenda.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Permite que a sua página HTML comunique com o Java sem bloqueios
public class PetAgendaController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ==========================================
    // ROTA 1: CRIAR AGENDAMENTO (POST)
    // ==========================================
    @PostMapping("/agendamento")
    public ResponseEntity<String> criarAgendamento(@RequestBody Map<String, String> dados) {
        try {
            String nomeTutor = dados.get("tutor");
            String telefone = dados.get("telefone");
            String nomePet = dados.get("pet");
            String raca = dados.get("raca");
            String idServico = dados.get("servico");
            String data = dados.get("data");
            String horario = dados.get("horario");
            String obs = dados.get("obs");

            String dataHora = data + " " + horario + ":00";
            int idTutor;
            int idPet;

            // 1. Verifica ou cria o Tutor
            String sqlBuscaTutor = "SELECT id_tutor FROM tutor WHERE telefone = ? LIMIT 1";
            List<Integer> tutoresEncontrados = jdbcTemplate.queryForList(sqlBuscaTutor, Integer.class, telefone);

            if (!tutoresEncontrados.isEmpty()) {
                idTutor = tutoresEncontrados.get(0);
            } else {
                String sqlInsertTutor = "INSERT INTO tutor (nome, telefone) VALUES (?, ?)";
                KeyHolder keyTutor = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(sqlInsertTutor, Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, nomeTutor);
                    ps.setString(2, telefone);
                    return ps;
                }, keyTutor);
                idTutor = keyTutor.getKey().intValue();
            }

            // 2. Verifica ou cria o Pet
            String sqlBuscaPet = "SELECT id_pet FROM pet WHERE nome = ? AND id_tutor = ? LIMIT 1";
            List<Integer> petsEncontrados = jdbcTemplate.queryForList(sqlBuscaPet, Integer.class, nomePet, idTutor);

            if (!petsEncontrados.isEmpty()) {
                idPet = petsEncontrados.get(0);
            } else {
                String sqlInsertPet = "INSERT INTO pet (nome, raca, id_tutor) VALUES (?, ?, ?)";
                KeyHolder keyPet = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(sqlInsertPet, Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, nomePet);
                    ps.setString(2, raca);
                    ps.setInt(3, idTutor);
                    return ps;
                }, keyPet);
                idPet = keyPet.getKey().intValue();
            }

            // 3. Cria o Agendamento
            String sqlAgendamento = "INSERT INTO agendamento (dataHora, observacoes, id_tutor, id_pet, id_servico) VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.update(sqlAgendamento, dataHora, obs, idTutor, idPet, Integer.parseInt(idServico));

            return ResponseEntity.ok("{\"sucesso\": true}");

        } catch (NumberFormatException | DataAccessException e) {
            return ResponseEntity.status(500).body("{\"erro\": \"Falha ao guardar os dados.\"}");
        }
    }

    // ==========================================
    // ROTA 2: LISTAR AGENDAMENTOS (GET)
    // ==========================================
    @GetMapping("/agendamentos")
    public ResponseEntity<List<Map<String, Object>>> listarAgendamentos() {
        try {
            // O comando JOIN une as tabelas para pegarmos os nomes em vez de apenas os IDs
            String sql = "SELECT " +
                         "a.id_agendamento AS id, " +
                         "t.nome AS tutor, " +
                         "t.telefone, " +
                         "p.nome AS pet, " +
                         "s.descricao AS servico, " +
                         "a.dataHora, " +
                         "a.status, " +
                         "a.observacoes " +
                         "FROM agendamento a " +
                         "JOIN tutor t ON a.id_tutor = t.id_tutor " +
                         "JOIN pet p ON a.id_pet = p.id_pet " +
                         "JOIN servico s ON a.id_servico = s.id_servico " +
                         "ORDER BY a.dataHora ASC";

            // queryForList já retorna uma lista pronta que o Spring Boot transforma em JSON automaticamente!
            List<Map<String, Object>> listaAgendamentos = jdbcTemplate.queryForList(sql);
            
            return ResponseEntity.ok(listaAgendamentos);

        } catch (DataAccessException e) {
            return ResponseEntity.status(500).body(null);
        }
    }
    
    // ==========================================
    // ROTA 3: ATUALIZAR STATUS (PUT)
    // ==========================================
    @PutMapping("/agendamento/{id}/status")
    public ResponseEntity<String> atualizarStatus(@PathVariable int id, @RequestBody Map<String, String> dados) {
        try {
            // Pega o novo status enviado pelo Front-end
            String novoStatus = dados.get("status");
            
            // O comando UPDATE modifica apenas a linha onde o id_agendamento for igual ao ID passado
            String sql = "UPDATE agendamento SET status = ? WHERE id_agendamento = ?";
            jdbcTemplate.update(sql, novoStatus, id);

            return ResponseEntity.ok("{\"sucesso\": true}");

        } catch (DataAccessException e) {
            return ResponseEntity.status(500).body("{\"erro\": \"Falha ao atualizar o status.\"}");
        }
    }

    // ==========================================
    // ROTA 4: LOGIN DE FUNCIONÁRIO (POST)
    // ==========================================
    @PostMapping("/login")
    public ResponseEntity<?> fazerLogin(@RequestBody Map<String, String> credenciais) {
        try {
            String loginDigitado = credenciais.get("login");
            String senhaDigitada = credenciais.get("senha");

            // Busca no banco um funcionário que tenha EXATAMENTE esse login e essa senha
            String sql = "SELECT nome, tipo_perfil FROM funcionario WHERE login = ? AND senha = ?";
            List<Map<String, Object>> usuariosEncontrados = jdbcTemplate.queryForList(sql, loginDigitado, senhaDigitada);

            if (!usuariosEncontrados.isEmpty()) {
                // Se encontrou, devolvemos os dados do usuário (com status 200 OK)
                return ResponseEntity.ok(usuariosEncontrados.get(0));
            } else {
                // Se a lista for vazia, as credenciais estão erradas (status 401 Unauthorized)
                return ResponseEntity.status(401).body("{\"erro\": \"Login ou senha incorretos.\"}");
            }

        } catch (DataAccessException e) {
            return ResponseEntity.status(500).body("{\"erro\": \"Erro de comunicação com o banco de dados.\"}");
        }
    }
}