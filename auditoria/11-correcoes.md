# Auditoria 11 – Correções aplicadas na interface

- **Data:** 17/11/2026
- **Aprovação:** a dupla escolheu as "7 melhorias baratas" da seção 7 de `05-frontend.md`.
- **Arquivos alterados:** `src/main/resources/static/index.html`, `login.html`, `dashboard.html`. **Arquivo novo:** `privacidade.html`.
- **Back-end:** não foi alterado (nenhum arquivo Java, SQL ou `application.properties`).

## O que mudou (em linguagem simples)

| # | Melhoria | O que foi feito | Achados resolvidos |
|---|---|---|---|
| 1 | Hero honesto | O cartão agora tem a etiqueta "Exemplo ilustrativo", mostra o status real "Pendente" e a duração do serviço, sem profissional. Os números "98%", "0" e "24h" deram lugar a três funções que o sistema tem de fato: bloqueio por duração, só horários livres e alerta de saúde. Saíram da lista "Histórico completo", "Cancelamento com facilidade", "24 horas" e "Profissionais qualificados", e "Em menos de 2 minutos" virou "Em poucos passos". O rodapé diz "Protótipo acadêmico (TCC)" no lugar do telefone falso. | H1 a H5; Nielsen H2 e H8 |
| 2 | Aviso LGPD | Aviso curto antes do botão de envio (para que servem os dados), caixa obrigatória "Li o aviso de privacidade e concordo..." e página `privacidade.html` (dados coletados, finalidade, quem vê, tempo de guarda, direitos do art. 18). Link "Privacidade" no rodapé. | Seção 6 |
| 3 | Contraste | Novo laranja para textos (`--amber-texto: #8A5A00`), botão "Solicitar" em `#9A6200`, laranja-claro `#F2B860` sobre verde, rótulos do hero em `#6B5D4D`, bordas dos campos mais fortes, placeholder mais visível, botão "Concluir" em `#1E7E34`. Contorno de foco de 3 px nos campos e botões. | 10 falhas da tabela 4.1 |
| 4 | Mensagens claras e acessíveis | O toast é anunciado pelo leitor de tela (`role="status"` no sucesso, `role="alert"` no erro), fica 7 s na tela (antes 4 s), mostra a data no formato brasileiro e explica o próximo passo ("Status: Pendente. O pet shop confirma pelo telefone informado."). O botão fica desabilitado e mostra "Enviando..." durante o envio. O login separa "senha incorreta", "erro do servidor" e "servidor desligado", apaga a mensagem antiga a cada tentativa e ganhou o link "Voltar para a página inicial". | F5, F6, F7; Nielsen H1, H5, H9; WCAG 4.1.3 |
| 5 | Preços da API | Os cards mostram preço e duração vindos de `GET /api/servicos`, a mesma fonte do formulário. Se o serviço não existir no banco, o card mostra "Indisponível no momento" em vez de um preço inventado. | F2 |
| 6 | Movimento reduzido | `@media (prefers-reduced-motion: reduce)` desliga o pulso, as patas, as transições e a rolagem suave. O conteúdo com animação de entrada (`.reveal`) só fica escondido se o JavaScript estiver ativo. | F9; WCAG 2.2.2 / 2.3.3 |
| 7 | Semântica | `<main>` nas 4 telas, `<h1>` no login, `<label for>` no login, `<h3>` nos passos (antes era `<h4>`), emojis decorativos com `aria-hidden="true"`, `aria-label` no menu e `autocomplete` nos campos de nome, telefone, usuário e senha. Rótulos "Data" e "Horário disponível" no lugar de "preferido", e "Telefone (com DDD)". | Seção 4.2; Nielsen H2 |

## Resultado dos testes

| Teste | Antes | Depois | Como foi medido |
|---|---|---|---|
| axe-core 4 (WCAG 2.0/2.1 A e AA + boas práticas), 1280 px | Landing: 24 ocorrências (11 de contraste, 1 de ordem de títulos, 12 de regiões). Login: 6 (sem `<main>`, sem `<h1>`, regiões). Painel: 22 (3 de contraste, 1 sem `<main>`, 18 de regiões) | **0 ocorrências** nas 4 telas | Chromium + axe-core, API simulada, movimento reduzido ligado para medir o texto parado |
| Rolagem horizontal em 360, 768 e 1280 px | Nenhuma | Nenhuma (4 telas) | `scrollWidth = clientWidth` |
| Envio sem marcar a caixa LGPD | (não existia) | Bloqueado pelo navegador; 0 pedidos enviados | Teste automatizado no Chromium |
| Clique duplo em "Solicitar" | Podia enviar 2 pedidos | 1 pedido; botão "Enviando..." desabilitado durante o envio | Teste automatizado (API com 0,8 s de atraso) |
| Mensagem de sucesso | Data em formato ISO, 4 s, sem anúncio | "Solicitação registrada para 20/11/2026 às 08:00. Status: Pendente..." com `role="status"` | Teste automatizado |
| Login com senha errada / servidor desligado | A mesma mensagem nos dois casos | Mensagens diferentes | Teste automatizado |
| `RegraConflitoTest` (JUnit) | 6 passaram | 6 passaram, 0 falharam | `mvn test -Dtest=RegraConflitoTest` |

Prints: `auditoria/prints/antes-*.png` e `auditoria/prints/depois-*.png` (360 e 1280 px).

**O que ainda não foi testado:** o roteiro `testes/TestesCT.java` (CT-01 a CT-13) e a tela com o banco de verdade, porque o ambiente da análise não tinha MySQL. Antes de entregar, rodem o sistema no PC (XAMPP), refaçam o CT-01 e o CT-13 e tirem os prints com dados reais. O back-end não mudou, então os testes da API não devem ser afetados.

## Limitações que continuam (declarar no TCC)

- A caixa de ciência da LGPD fica só na tela. O sistema **não grava** no banco que o tutor marcou a caixa, nem quando. Gravar isso exigiria mudar o banco e a API.
- O texto do aviso de privacidade é um modelo. Um pet shop real precisa colocar o seu contato e revisar o texto.
- Continuam pendentes as melhorias 3 (menu no celular), 6 (diálogo próprio no lugar de `confirm/alert` no painel) e 10 (painel no celular e navegação por dia), que ficaram fora desta rodada.
- Nenhum teste foi feito com usuários reais.
