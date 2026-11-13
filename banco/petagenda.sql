-- =====================================================================
-- PetAgenda - script do banco de dados (versão Etapa 2)
-- Compatível com MySQL 8 e MariaDB 10.4+ (XAMPP)
--
-- ATENÇÃO: este script APAGA as tabelas existentes e recria tudo.
-- Os dados de teste antigos serão perdidos.
--
-- Como usar no XAMPP: abra o phpMyAdmin > aba "SQL" > cole este conteúdo > Executar.
-- O usuário admin é criado automaticamente pelo sistema na primeira execução
-- (login: admin / senha: admin123).
-- =====================================================================

CREATE DATABASE IF NOT EXISTS petagenda CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE petagenda;

SET FOREIGN_KEY_CHECKS = 0;
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

CREATE TABLE servico (
  id_servico      INT AUTO_INCREMENT PRIMARY KEY,
  descricao       VARCHAR(100)  NOT NULL,
  preco           DECIMAL(10,2) NOT NULL,
  duracaoMinutos  INT           NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE funcionario (
  id_funcionario  INT AUTO_INCREMENT PRIMARY KEY,
  nome            VARCHAR(60)  NOT NULL,
  telefone        VARCHAR(14)  NOT NULL,
  email           VARCHAR(90)  NOT NULL,
  login           VARCHAR(50)  NOT NULL,
  senha           VARCHAR(255) NOT NULL,      -- hash BCrypt, nunca a senha pura
  tipo_perfil     VARCHAR(50)  DEFAULT 'Atendente',
  CONSTRAINT uq_funcionario_login UNIQUE (login)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE agendamento (
  id_agendamento  INT AUTO_INCREMENT PRIMARY KEY,
  dataHora        DATETIME    NOT NULL,
  observacoes     TEXT        DEFAULT NULL,
  status          VARCHAR(30) NOT NULL DEFAULT 'Pendente',  -- Pendente, Em andamento, Concluído, Cancelado
  id_tutor        INT         NOT NULL,
  id_pet          INT         NOT NULL,
  id_servico      INT         NOT NULL,
  id_funcionario  INT         DEFAULT NULL,
  INDEX idx_agendamento_data (dataHora),
  CONSTRAINT fk_ag_tutor   FOREIGN KEY (id_tutor)       REFERENCES tutor (id_tutor),
  CONSTRAINT fk_ag_pet     FOREIGN KEY (id_pet)         REFERENCES pet (id_pet),
  CONSTRAINT fk_ag_servico FOREIGN KEY (id_servico)     REFERENCES servico (id_servico),
  CONSTRAINT fk_ag_func    FOREIGN KEY (id_funcionario) REFERENCES funcionario (id_funcionario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Catálogo de serviços (preço em R$, duração em minutos)
INSERT INTO servico (id_servico, descricao, preco, duracaoMinutos) VALUES
  (1, 'Banho',                40.00, 30),
  (2, 'Tosa',                 55.00, 45),
  (3, 'Consulta Veterinária', 90.00, 30),
  (4, 'Banho + Tosa',         85.00, 75),
  (5, 'Hidratação',           35.00, 30),
  (6, 'Escovação Dental',     25.00, 15);
