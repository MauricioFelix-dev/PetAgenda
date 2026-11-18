# Roteiro dos testes manuais (CT-06, CT-09, CT-12, CT-13)

Estes casos não podem ser conferidos só pelo script: precisam de olho humano e de print.

**Antes de começar**
1. XAMPP com o **MySQL** ligado (o Tomcat do XAMPP desligado).
2. Sistema rodando: `.\mvnw.cmd spring-boot:run` e esperar `Started ApiApplication`.
3. Rodar o roteiro automático (`java testes/TestesCT.java`) — ele cria os agendamentos
   usados no CT-06 e no CT-12. Anote o **dia escolhido** que aparece no início da saída
   (ex.: `Dia escolhido para os testes (agenda vazia): 2026-11-24`).
4. Criar uma pasta para os prints, por exemplo `testes/resultados/prints/`.

---

## CT-06 — Faixa vermelha de restrição de saúde

1. Abrir http://localhost:8080/login.html e entrar com **admin / admin123**.
2. No painel, no campo **Data**, escolher o dia anotado no passo 3.
3. Procurar o card da tutora **Eva Teste** / pet **Nina** (16:00).
4. **Esperado:** aparece uma faixa vermelha com
   `⚠️ Restrição de saúde: alergia a shampoo perfumado`.
5. Tirar print mostrando o card (Win + Shift + S) e salvar como `CT-06.png`.

**Passou** se a faixa aparece em destaque. **Falhou** se a restrição não aparece
ou aparece como texto comum, sem destaque.

---

## CT-09 — Senha guardada criptografada

1. Abrir http://localhost/phpmyadmin.
2. À esquerda, clicar no banco **petagenda** e depois na tabela **funcionario**.
3. Olhar a coluna **senha** da linha do usuário `admin`.
4. **Esperado:** o valor começa com `$2a$` e tem 60 caracteres
   (ex.: `$2a$10$...`). Para contar, rode na aba **SQL**:
   ```sql
   SELECT login, LEFT(senha, 4) AS inicio, LENGTH(senha) AS tamanho FROM funcionario;
   ```
   Deve mostrar `inicio = $2a$` e `tamanho = 60`.
5. Tirar print da tabela ou do resultado da consulta e salvar como `CT-09.png`.

**Passou** se não dá para ler `admin123` em lugar nenhum. **Falhou** se a senha
aparece em texto puro.

---

## CT-12 — Texto malicioso (XSS) não executa

1. Logado no painel, com o mesmo dia do CT-06 selecionado no campo **Data**.
2. Procurar o card do tutor **Gil Teste** / pet **Zeca** (17:00).
3. **Esperado:** na caixa de observação aparece escrito literalmente
   `📝 <img src=x onerror=alert(1)>` e **nenhuma janela de alerta abre**.
4. Recarregar a página (F5) uma vez e confirmar que continua sem alerta.
5. Tirar print do card e salvar como `CT-12.png`.

**Passou** se o texto aparece escrito e nada é executado. **Falhou** se abrir uma
janela "1", se aparecer um ícone de imagem quebrada no lugar do texto, ou se o
texto sumir.

---

## CT-13 — Responsividade (celular)

**Opção A — no próprio PC (mais fácil)**
1. Abrir http://localhost:8080 no Chrome ou Edge.
2. Apertar **F12** e depois **Ctrl + Shift + M** (modo dispositivo).
3. No topo, escolher um celular (ex.: **iPhone SE** ou **Galaxy S20**).
4. Conferir na **página inicial**: textos legíveis sem zoom, botões inteiros,
   formulário de agendamento usável, **nenhuma barra de rolagem lateral**.
5. Repetir em http://localhost:8080/login.html e, depois de entrar, no **painel**
   (cards um embaixo do outro, campo de data e botões acessíveis).
6. Tirar prints: `CT-13-inicial.png`, `CT-13-login.png`, `CT-13-painel.png`.

**Opção B — no celular de verdade** (celular e PC na mesma rede Wi-Fi)
1. No PC, abrir o PowerShell e rodar `ipconfig`; anotar o **Endereço IPv4**
   (ex.: `192.168.0.15`).
2. No celular, abrir `http://192.168.0.15:8080`.
3. Se não abrir, o Firewall do Windows está bloqueando: permitir o Java
   quando o aviso aparecer, ou liberar a porta 8080 nas configurações do Firewall.
4. Fazer as mesmas conferências da opção A e tirar prints pelo celular.

**Passou** se as três telas funcionam sem rolagem lateral e sem elementos cortados.

---

## Registrar o resultado

No relatório gerado em `testes/resultados/resultado-AAAA-MM-DD_HH-MM.md`, trocar
`[MANUAL]` por `[PASSOU]` (ou `[FALHOU]` com o motivo) e citar o nome do print
de cada caso.
