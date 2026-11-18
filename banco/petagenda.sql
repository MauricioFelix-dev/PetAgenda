-- =====================================================================
-- PetAgenda - script do banco de dados (versão Etapa 3)
-- Compatível com MySQL 8 e MariaDB 10.4+ (XAMPP)
--
-- ATENÇÃO: este script APAGA as tabelas existentes e recria tudo.
-- Os dados de teste antigos serão perdidos.
--
-- Como usar no XAMPP: abra o phpMyAdmin > aba "SQL" > cole este conteúdo > Executar.
-- Já cria dois funcionários de EXEMPLO (troque nomes e senhas no painel):
--   admin / admin123  -> perfil Administrador (ex.: a proprietária, veterinária)
--   ana   / ana12345  -> perfil Atendente (ex.: banho, tosa e escovação)
-- Quem já tem dados e não quer perder: use banco/migracao-etapa3.sql.
-- =====================================================================

-- Correção: garante que o script seja lido como UTF-8. Sem esta linha, ao importar
-- pelo terminal (cliente "mysql"), acentos como "Escovação" e "Hidratação" eram
-- gravados corrompidos ("EscovaÃ§Ã£o"). No phpMyAdmin não faz diferença.
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS petagenda CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE petagenda;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS bloqueio;
DROP TABLE IF EXISTS agendamento;
DROP TABLE IF EXISTS pet;
DROP TABLE IF EXISTS tutor;
DROP TABLE IF EXISTS servico;
DROP TABLE IF EXISTS funcionario;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE tutor (
  id_tutor  INT AUTO_INCREMENT PRIMARY KEY,
  nome      VARCHAR(100) NOT NULL,
  telefone  VARCHAR(11)  NOT NULL,          -- somente dígitos (DDD + número)
  email     VARCHAR(100) DEFAULT NULL,
  endereco  VARCHAR(200) DEFAULT NULL,
  CONSTRAINT uq_tutor_telefone UNIQUE (telefone)   -- impede tutor duplicado
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pet (
  id_pet            INT AUTO_INCREMENT PRIMARY KEY,
  nome              VARCHAR(50)  NOT NULL,
  especie           VARCHAR(30)  DEFAULT NULL,  -- Cão, Gato, Outro
  raca              VARCHAR(50)  DEFAULT NULL,
  idade             INT          DEFAULT NULL,
  restricoes_saude  TEXT         DEFAULT NULL,  -- alergias, medicamentos, comportamento
  id_tutor          INT          NOT NULL,
  CONSTRAINT fk_pet_tutor FOREIGN KEY (id_tutor) REFERENCES tutor (id_tutor) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE funcionario (
  id_funcionario  INT AUTO_INCREMENT PRIMARY KEY,
  nome            VARCHAR(60)  NOT NULL,
  telefone        VARCHAR(14)  NOT NULL,
  email           VARCHAR(90)  NOT NULL,
  login           VARCHAR(50)  NOT NULL,
  senha           VARCHAR(255) NOT NULL,      -- hash BCrypt, nunca a senha pura
  tipo_perfil     VARCHAR(50)  NOT NULL DEFAULT 'Atendente',  -- Administrador ou Atendente
  ativo           TINYINT(1)   NOT NULL DEFAULT 1,            -- 0 = desligado (não entra no sistema)
  CONSTRAINT uq_funcionario_login UNIQUE (login),
  CONSTRAINT ck_funcionario_perfil CHECK (tipo_perfil IN ('Administrador', 'Atendente'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE servico (
  id_servico      INT AUTO_INCREMENT PRIMARY KEY,
  descricao       VARCHAR(100)  NOT NULL,
  detalhes        VARCHAR(255)  DEFAULT NULL,   -- texto do card na página inicial
  icone           VARCHAR(16)   DEFAULT NULL,   -- emoji do card
  preco           DECIMAL(10,2) NOT NULL,
  duracaoMinutos  INT           NOT NULL,
  ativo           TINYINT(1)    NOT NULL DEFAULT 1,  -- 0 = desativado (some da página)
  id_responsavel  INT           DEFAULT NULL,        -- profissional que realiza o serviço
  CONSTRAINT fk_servico_responsavel FOREIGN KEY (id_responsavel) REFERENCES funcionario (id_funcionario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE agendamento (
  id_agendamento  INT AUTO_INCREMENT PRIMARY KEY,
  dataHora        DATETIME    NOT NULL,
  observacoes     TEXT        DEFAULT NULL,
  status          VARCHAR(30) NOT NULL DEFAULT 'Pendente',  -- Pendente, Confirmado, Em andamento, Concluído, Cancelado
  codigo          VARCHAR(8)  NOT NULL,                     -- código que o tutor usa em "Meus agendamentos"
  id_tutor        INT         NOT NULL,
  id_pet          INT         NOT NULL,
  id_servico      INT         NOT NULL,
  id_funcionario  INT         DEFAULT NULL,                 -- profissional responsável (copiado do serviço)
  INDEX idx_agendamento_data (dataHora),
  CONSTRAINT uq_agendamento_codigo UNIQUE (codigo),
  CONSTRAINT ck_agendamento_status CHECK (status IN ('Pendente', 'Confirmado', 'Em andamento', 'Concluído', 'Cancelado')),
  CONSTRAINT fk_ag_tutor   FOREIGN KEY (id_tutor)       REFERENCES tutor (id_tutor),
  CONSTRAINT fk_ag_pet     FOREIGN KEY (id_pet)         REFERENCES pet (id_pet),
  CONSTRAINT fk_ag_servico FOREIGN KEY (id_servico)     REFERENCES servico (id_servico),
  CONSTRAINT fk_ag_func    FOREIGN KEY (id_funcionario) REFERENCES funcionario (id_funcionario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- UC10 Bloquear horários: período em que a agenda não aceita reservas
-- (feriado, folga, manutenção). id_funcionario NULL = bloqueia o pet shop inteiro;
-- preenchido = bloqueia só os serviços daquele profissional.
CREATE TABLE bloqueio (
  id_bloqueio     INT AUTO_INCREMENT PRIMARY KEY,
  inicio          DATETIME     NOT NULL,
  fim             DATETIME     NOT NULL,
  motivo          VARCHAR(100) NOT NULL,
  id_funcionario  INT          DEFAULT NULL,
  INDEX idx_bloqueio_inicio (inicio),
  CONSTRAINT ck_bloqueio_periodo CHECK (fim > inicio),
  CONSTRAINT fk_bloqueio_func FOREIGN KEY (id_funcionario) REFERENCES funcionario (id_funcionario) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Funcionários de EXEMPLO (senhas guardadas em BCrypt; troque no painel)
--   admin / admin123  e  ana / ana12345
INSERT INTO funcionario (id_funcionario, nome, telefone, email, login, senha, tipo_perfil) VALUES
  (1, 'Dra. Helena', '99900000001', 'helena@petagenda.local', 'admin',
      '$2a$10$uGY18K0sODRR0ClP/Jmju.X6iO.slUgJUxbzTVpzaa6j7FIRb9L82', 'Administrador'),
  (2, 'Ana',         '99900000002', 'ana@petagenda.local',    'ana',
      '$2a$10$w5xKx4Ou544md5FI1iK3LeBE4IuQHICjnUvpAaEZ5M2daTKmbgYfO', 'Atendente');

-- Catálogo de serviços (preço em R$, duração em minutos, responsável)
INSERT INTO servico (id_servico, descricao, detalhes, icone, preco, duracaoMinutos, id_responsavel) VALUES
  (1, 'Banho',                'Banho completo com produtos adequados ao porte e à pelagem do seu pet.',   '🛁', 40.00, 30, 2),
  (2, 'Tosa',                 'Tosa higiênica ou completa, feita com calma e cuidado.',                   '✂️', 55.00, 45, 2),
  (3, 'Consulta Veterinária', 'Consulta clínica geral para checkup, vacinas e orientações.',             '🩺', 90.00, 30, 1),
  (4, 'Banho + Tosa',         'Pacote completo de higiene, o favorito dos tutores.',                      '💆', 85.00, 75, 2),
  (5, 'Hidratação',           'Tratamento para deixar a pelagem macia e brilhante.',                      '💅', 35.00, 30, 2),
  (6, 'Escovação Dental',     'Higiene bucal para prevenir tártaro e mau hálito.',                        '🦷', 25.00, 15, 2);
