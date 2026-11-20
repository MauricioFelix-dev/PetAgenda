-- =====================================================================
-- PetAgenda - migração da Etapa 2 para a Etapa 3 SEM apagar dados
--
-- Use este arquivo se você já tem agendamentos de teste e não quer perdê-los.
-- Para começar do zero, use banco/petagenda.sql (que apaga tudo e já cria
-- os funcionários e serviços de exemplo).
--
-- Rode UMA vez: phpMyAdmin > banco petagenda > aba "SQL" > colar > Executar.
-- Depois, no painel (login admin), defina o responsável de cada serviço.
-- =====================================================================
SET NAMES utf8mb4;
USE petagenda;

-- Funcionário: perfil obrigatório e opção de desligar sem apagar o histórico
UPDATE funcionario SET tipo_perfil = 'Atendente' WHERE tipo_perfil IS NULL OR tipo_perfil NOT IN ('Administrador', 'Atendente');
ALTER TABLE funcionario
  MODIFY tipo_perfil VARCHAR(50) NOT NULL DEFAULT 'Atendente',
  ADD COLUMN ativo TINYINT(1) NOT NULL DEFAULT 1,
  ADD CONSTRAINT ck_funcionario_perfil CHECK (tipo_perfil IN ('Administrador', 'Atendente'));

-- Serviço: texto e ícone do card, ativo/desativado e profissional responsável
ALTER TABLE servico
  ADD COLUMN detalhes VARCHAR(255) DEFAULT NULL AFTER descricao,
  ADD COLUMN icone VARCHAR(16) DEFAULT NULL AFTER detalhes,
  ADD COLUMN ativo TINYINT(1) NOT NULL DEFAULT 1,
  ADD COLUMN id_responsavel INT DEFAULT NULL,
  ADD CONSTRAINT fk_servico_responsavel FOREIGN KEY (id_responsavel) REFERENCES funcionario (id_funcionario);

UPDATE servico SET icone = '🛁', detalhes = 'Banho completo com produtos adequados ao porte e à pelagem do seu pet.' WHERE id_servico = 1;
UPDATE servico SET icone = '✂️', detalhes = 'Tosa higiênica ou completa, feita com calma e cuidado.' WHERE id_servico = 2;
UPDATE servico SET icone = '🩺', detalhes = 'Consulta clínica geral para checkup, vacinas e orientações.' WHERE id_servico = 3;
UPDATE servico SET icone = '💆', detalhes = 'Pacote completo de higiene, o favorito dos tutores.' WHERE id_servico = 4;
UPDATE servico SET icone = '💅', detalhes = 'Tratamento para deixar a pelagem macia e brilhante.' WHERE id_servico = 5;
UPDATE servico SET icone = '🦷', detalhes = 'Higiene bucal para prevenir tártaro e mau hálito.' WHERE id_servico = 6;

-- Agendamento: código para o tutor consultar/cancelar e status Confirmado
ALTER TABLE agendamento ADD COLUMN codigo VARCHAR(8) DEFAULT NULL AFTER status;
UPDATE agendamento SET codigo = UPPER(SUBSTRING(MD5(CONCAT(id_agendamento, '-', RAND())), 1, 6)) WHERE codigo IS NULL;
ALTER TABLE agendamento
  MODIFY codigo VARCHAR(8) NOT NULL,
  ADD CONSTRAINT uq_agendamento_codigo UNIQUE (codigo),
  ADD CONSTRAINT ck_agendamento_status CHECK (status IN ('Pendente', 'Confirmado', 'Em andamento', 'Concluído', 'Cancelado'));

-- Bloqueio de horários (UC10)
CREATE TABLE IF NOT EXISTS bloqueio (
  id_bloqueio     INT AUTO_INCREMENT PRIMARY KEY,
  inicio          DATETIME     NOT NULL,
  fim             DATETIME     NOT NULL,
  motivo          VARCHAR(100) NOT NULL,
  id_funcionario  INT          DEFAULT NULL,
  INDEX idx_bloqueio_inicio (inicio),
  CONSTRAINT ck_bloqueio_periodo CHECK (fim > inicio),
  CONSTRAINT fk_bloqueio_func FOREIGN KEY (id_funcionario) REFERENCES funcionario (id_funcionario) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
