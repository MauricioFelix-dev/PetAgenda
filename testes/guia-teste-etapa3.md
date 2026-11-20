# Guia de teste da Etapa 3 (PR #3) no seu PC

Tempo estimado: **1 hora a 1 hora e meia**, contando os prints.
Você vai precisar de: XAMPP (só o MySQL ligado), VS Code com o projeto aberto e um navegador.

Crie uma pasta para os prints, por exemplo `testes/resultados/prints-etapa3/`. O nome de cada print está indicado em **📸** ao longo do guia.

---

## Parte 1: preparar (15 min)

### 1.1 Baixar a versão nova do código

O código novo ainda não está na `main`: ele está na branch do PR #3. No VS Code, abra o terminal (**Terminal > Novo Terminal**) e rode, uma linha de cada vez:

```
git fetch origin
git checkout claude/project-thread-pl75ii
git pull
```

Para conferir, rode `git log --oneline -1`. Deve aparecer `Docs da etapa 3...` ou um commit mais novo.

> Se você não usa git: no GitHub, abra o PR #3, clique no nome da branch `claude/project-thread-pl75ii`, depois em **Code > Download ZIP**, e abra a pasta extraída no VS Code.

### 1.2 Recriar o banco

Use o script completo, porque ele já cria a Ana, a Dra. Helena e os responsáveis de cada serviço, e é com eles que os testes funcionam.

1. Abra o XAMPP e ligue **só o MySQL** (o Tomcat precisa ficar desligado).
2. Entre em http://localhost/phpmyadmin, clique na aba **SQL**.
3. Abra o arquivo `banco/petagenda.sql` no VS Code, copie tudo, cole no phpMyAdmin e clique em **Executar**.

> ⚠️ Esse script **apaga os agendamentos de teste antigos**. Os prints de 17/11 já estão salvos, então não tem problema.
> Existe também o `banco/migracao-etapa3.sql`, que mantém os dados, mas com ele a Ana não é criada e os serviços ficam sem responsável. Para este teste, use o `petagenda.sql`.

### 1.3 Ligar o sistema

Se o sistema antigo estiver rodando, pare com **Ctrl + C** no terminal dele. Depois rode:

```
.\mvnw.cmd spring-boot:run
```

Espere aparecer `Started ApiApplication`. Deixe esse terminal aberto.

**Usuários de teste:**

| Login | Senha | Quem é | Atende |
|---|---|---|---|
| admin | admin123 | Dra. Helena, veterinária e dona (Administrador) | Consulta Veterinária |
| ana | ana12345 | Ana (Atendente) | Banho, Tosa, Banho + Tosa, Hidratação, Escovação |

---

## Parte 2: roteiro automático (5 min)

Abra **um segundo terminal** (o primeiro continua com o sistema ligado) e rode:

```
testes\rodar-testes.bat
```

**Esperado:** no fim aparece `RESUMO: 19 passou | 0 falhou | 2 verificar manualmente`.
O relatório fica salvo em `testes/resultados/resultado-AAAA-MM-DD_HH-mm.md`.

📸 `01-roteiro-automatico.png`: a tela do terminal com o resumo.

> O roteiro cria agendamentos fictícios num dia futuro vazio. Se ele falhar em algum caso, me mande o arquivo de resultado.

---

## Parte 3: testes na tela (40 min)

Faça na ordem: cada passo usa o anterior.

### CT-01 e CT-15. Agendar e ver quem vai atender

1. Abra http://localhost:8080
2. Em **Serviços**, confira que cada card mostra "Atendimento com Ana" ou "Atendimento com Dra. Helena".
3. No formulário, preencha seu nome, um telefone, o nome do pet e escolha **Banho**. Logo abaixo deve aparecer **"Quem vai atender: Ana"**.
4. Escolha uma data (amanhã ou depois), o horário **08:00**, preencha a idade do pet (campo novo, opcional), marque o aceite de privacidade e envie.

**Esperado:** o formulário some e aparece a caixa **"Pedido registrado!"** com a Ana como responsável e um **código de 6 letras e números**.

**Anote o código e o telefone**, você vai usar no CT-18.

📸 `02-cards-com-responsavel.png` · 📸 `03-confirmacao-com-codigo.png`

5. Clique em **Fazer outro agendamento**. Agora escolha **Consulta Veterinária**, na **mesma data**, e veja a lista de horários.

**Esperado:** o **08:00 aparece livre**, porque a consulta é com a Dra. Helena e não choca com o banho da Ana. Agende às 08:00.

6. Faça mais um agendamento de **Banho** na mesma data e veja a lista.

**Esperado:** o **08:00 não aparece**, porque a Ana já está ocupada.

📸 `04-consulta-0800-livre.png` (lista de horários da consulta com 08:00)

### CT-18. Meus agendamentos (o tutor consulta e cancela)

1. Clique em **Meus agendamentos** no menu do topo.
2. Digite o telefone e um código **errado** (ex.: `ZZZZZZ`) e clique em **Ver meus agendamentos**.
   **Esperado:** mensagem vermelha "Não encontramos agendamento com esse telefone e código".
3. Agora digite o **código certo** (pode ser em minúsculas).
   **Esperado:** aparece "Olá, [seu nome]!" e a lista com os seus agendamentos.
4. No banho, clique em **Cancelar este agendamento** e depois em **Sim, cancelar**.
   **Esperado:** mensagem "Agendamento cancelado" e o card fica com status **Cancelado**, sem botão.

📸 `05-meus-agendamentos-lista.png` · 📸 `06-dialogo-cancelar.png` · 📸 `07-cancelado.png`

5. Volte à página inicial e escolha Banho na mesma data: o **08:00 voltou** a aparecer (CT-04).

### CT-14 e CT-11. Painel e regra de status

1. Abra http://localhost:8080/login.html e entre com **admin / admin123**.
2. No topo deve aparecer "Olá, Dra. Helena (Administrador)" e **5 abas**: Agenda do dia, Histórico do tutor, Serviços, Funcionários, Bloqueios.
3. Na Agenda, use as setas **Anterior / Próximo** ou o calendário para ir até a data dos seus agendamentos.
4. Na consulta (Pendente), clique em **Confirmar**, depois **Iniciar** e depois **Concluir**. Cada clique pede confirmação numa janela do próprio site.
   **Esperado:** o status muda a cada passo e o faturamento do dia aumenta.
5. Confira que o card **Concluído** e o card **Cancelado** **não têm botões**: esses status são finais e não podem ser alterados (o roteiro automático confirma que o servidor recusa com erro 409).

📸 `08-painel-admin-5-abas.png` · 📸 `09-concluido-sem-botoes.png`

### Histórico do tutor

1. Aba **Histórico do tutor**, digite o telefone que você usou e clique em buscar.
   **Esperado:** aparece seu nome e a lista de todos os atendimentos, com responsável e status.

📸 `10-historico-do-tutor.png`

### CT-19. Cadastrar serviço

1. Aba **Serviços**. No formulário, cadastre: nome "Corte de unhas", descrição curta, preço 20, duração 15, um ícone e responsável **Ana**. Salve.
2. Abra a página inicial numa nova aba (ou recarregue): o card **Corte de unhas** aparece.
3. Volte ao painel, clique em **Editar** no Corte de unhas, desmarque **Ativo** e salve.
4. Recarregue a página inicial: o card **sumiu**.

📸 `11-servico-cadastrado.png` · 📸 `12-card-novo-na-pagina.png`

### CT-16. Bloquear horários

1. Aba **Bloqueios**. Escolha uma data futura, horário **08:00 às 12:00**, "Pet shop inteiro", motivo "Feriado". Salve.
2. Na página inicial, escolha Banho nessa data.
   **Esperado:** a lista começa em **12:00** (a manhã sumiu).
3. Volte ao painel, clique em **Remover** no bloqueio. Na página inicial, a manhã volta.
4. (Opcional) Faça um bloqueio de **dia inteiro só para a Ana** ("Folga"). Na página inicial, Banho fica sem horários nesse dia, mas a **Consulta continua com todos**.

📸 `13-bloqueio-cadastrado.png` · 📸 `14-horarios-a-partir-12h.png`

### CT-17. Perfis (Atendente não vê telas de administrador)

1. Aba **Funcionários**: confira que a Ana e a Dra. Helena estão na lista. A senha **não aparece** em lugar nenhum.
2. Clique em **Sair** e entre com **ana / ana12345**.
   **Esperado:** "Olá, Ana (Atendente)" e só **2 abas** (Agenda do dia e Histórico do tutor).
3. Ainda logado como Ana, abra http://localhost:8080/api/admin/servicos
   **Esperado:** a mensagem `{"erro":"Somente o administrador pode fazer isso."}` (erro 403).

📸 `15-painel-atendente-2-abas.png` · 📸 `16-api-admin-403.png`

### Trocar a senha

1. Logado como Ana, clique em **Trocar minha senha**. Digite a senha atual errada: aparece erro.
2. Digite a senha atual certa (`ana12345`) e uma nova, por exemplo `ana99999`. Saia e entre de novo com a senha nova.
3. Troque de volta para `ana12345`, para não esquecer.

📸 `17-trocar-senha.png`

### CT-13. Celular

Abra a página inicial, **Meus agendamentos** e o painel no celular (mesma rede Wi-Fi, endereço `http://IP-DO-PC:8080`) ou com **F12 > ícone de celular** no navegador.

**Esperado:** nada corta na lateral; no painel, a agenda aparece antes dos indicadores.

📸 `18-celular-meus-agendamentos.jpg` · 📸 `19-celular-painel.jpg`

### CT-09. Senha criptografada

No phpMyAdmin, abra a tabela `funcionario`: a coluna `senha` das duas pessoas começa com `$2a$`.

📸 `20-senhas-bcrypt.png`

---

## Parte 4: me avise

Quando terminar, mande aqui:
- o arquivo `resultado-...md` gerado pelo roteiro;
- os prints (ou só os que deram algo diferente do esperado);
- qualquer coisa que pareceu estranha, mesmo que pequena.

Com isso eu registro os resultados no relatório (como fiz com o CT-01 e CT-13) e seguimos para o polimento visual.

## Se algo der errado

| O que aparece | O que fazer |
|---|---|
| `Port 8080 was already in use` | O sistema antigo ainda está ligado, ou o Tomcat do XAMPP. Feche o terminal antigo ou clique em **Stop** no Tomcat |
| Login dá "usuário ou senha inválidos" com admin/admin123 | O banco não foi recriado. Repita o passo 1.2 |
| A página inicial está igual à antiga | O navegador guardou a versão velha. Aperte **Ctrl + F5** |
| Erro `Unknown column 'codigo'` ou parecido no terminal | O banco ainda é o da Etapa 2. Repita o passo 1.2 |
| `ERRO: o sistema nao respondeu` no roteiro | O sistema não está ligado no outro terminal |
