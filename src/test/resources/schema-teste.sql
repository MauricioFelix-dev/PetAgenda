-- Esquema simplificado para os testes da camada de serviço (banco H2 em memória).
-- É o mesmo modelo de banco/petagenda.sql, escrito em SQL compatível com o H2.
-- Diferença proposital: observacoes tem só 20 caracteres, para o teste da
-- transação conseguir provocar um erro na ÚLTIMA gravação (do agendamento).
CREATE TABLE tutor (
  id_tutor INT AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(100) NOT NULL,
  telefone VARCHAR(11) NOT NULL UNIQUE,
  email VARCHAR(100), endereco VARCHAR(200)
);
CREATE TABLE pet (
  id_pet INT AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(50) NOT NULL, especie VARCHAR(30), raca VARCHAR(50), idade INT,
  restricoes_saude VARCHAR(1000),
  id_tutor INT NOT NULL REFERENCES tutor (id_tutor)
);
CREATE TABLE funcionario (
  id_funcionario INT AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(60) NOT NULL, telefone VARCHAR(14) NOT NULL, email VARCHAR(90) NOT NULL,
  login VARCHAR(50) NOT NULL UNIQUE, senha VARCHAR(255) NOT NULL,
  tipo_perfil VARCHAR(50) NOT NULL DEFAULT 'Atendente', ativo TINYINT NOT NULL DEFAULT 1
);
CREATE TABLE servico (
  id_servico INT AUTO_INCREMENT PRIMARY KEY,
  descricao VARCHAR(100) NOT NULL, detalhes VARCHAR(255), icone VARCHAR(16),
  preco DECIMAL(10,2) NOT NULL, duracaoMinutos INT NOT NULL,
  ativo TINYINT NOT NULL DEFAULT 1,
  id_responsavel INT REFERENCES funcionario (id_funcionario)
);
CREATE TABLE agendamento (
  id_agendamento INT AUTO_INCREMENT PRIMARY KEY,
  dataHora TIMESTAMP NOT NULL,
  observacoes VARCHAR(20),
  status VARCHAR(30) NOT NULL DEFAULT 'Pendente',
  codigo VARCHAR(8) NOT NULL UNIQUE,
  id_tutor INT NOT NULL REFERENCES tutor (id_tutor),
  id_pet INT NOT NULL REFERENCES pet (id_pet),
  id_servico INT NOT NULL REFERENCES servico (id_servico),
  id_funcionario INT REFERENCES funcionario (id_funcionario)
);
CREATE TABLE bloqueio (
  id_bloqueio INT AUTO_INCREMENT PRIMARY KEY,
  inicio TIMESTAMP NOT NULL, fim TIMESTAMP NOT NULL, motivo VARCHAR(100) NOT NULL,
  id_funcionario INT REFERENCES funcionario (id_funcionario)
);

INSERT INTO funcionario (id_funcionario, nome, telefone, email, login, senha, tipo_perfil) VALUES
  (1, 'Dra. Helena', '99900000001', 'h@x.com', 'admin', 'x', 'Administrador'),
  (2, 'Ana', '99900000002', 'a@x.com', 'ana', 'x', 'Atendente');
INSERT INTO servico (id_servico, descricao, preco, duracaoMinutos, ativo, id_responsavel) VALUES
  (1, 'Banho', 40.00, 30, 1, 2),
  (3, 'Consulta Veterinária', 90.00, 30, 1, 1),
  (4, 'Banho + Tosa', 85.00, 75, 1, 2),
  (7, 'Serviço desativado', 10.00, 30, 0, 2);
