# Auditoria 05 – Front-end: design, usabilidade e acessibilidade

- **Data:** 17/11/2026
- **Base:** branch `main`, commit `df374cd` (14/11/2026)
- **Arquivos analisados:** `src/main/resources/static/index.html` (landing + formulário), `login.html`, `dashboard.html`
- **Modo:** somente leitura (nenhum arquivo do sistema foi alterado)

Legenda: **[Certo]** = vi no código ou medi; **[Provável]** = inferência forte; **[Suposição]** = não verificado.
Severidade: Crítica, Alta, Média, Baixa (ver escala de Nielsen na seção 3).

## Como a análise foi feita (método)

1. **Leitura do código** das três telas, linha a linha.
2. **Avaliação heurística** com as 10 heurísticas de Nielsen (1994; NIELSEN; MOLICH, 1990), aplicada pelos avaliadores (a dupla, com apoio do Claude Code como ferramenta de revisão).
3. **Medição de contraste** pela fórmula de luminância relativa da WCAG 2.1 (critérios 1.4.3 e 1.4.11), calculada para cada par de cores do `:root`.
4. **Teste automático de acessibilidade** com a biblioteca axe-core 4 (regras WCAG 2.0/2.1 A e AA) no navegador Chromium, com a API simulada (dados fictícios), porque o MySQL não estava disponível no ambiente da análise.
5. **Teste de responsividade** real (não "mental"): prints de página inteira nas larguras 360 px (celular), 768 px (tablet) e 1280 px (computador). Os prints de 360 e 1280 px estão em `auditoria/prints/` (arquivos `antes-*`).
6. **Teste de teclado:** navegação com Tab nas três telas, verificando se o foco fica visível.

> Limitação do método: não houve teste com usuários reais (tutores ou funcionários). A avaliação heurística é um método de **inspeção** feito por avaliadores, e Nielsen recomenda de 3 a 5 avaliadores para encontrar a maior parte dos problemas (NIELSEN, 1992). Isso deve ser declarado no TCC.

---

## 1. Bugs funcionais da interface

| # | Problema | Onde | Severidade | Grau |
|---|---|---|---|---|
| F1 | **Menu some no celular e não há alternativa.** Abaixo de 900 px, `.nav-links { display: none; }`. Medido: em 360 px e 768 px, 0 links visíveis no topo. O botão "Área Restrita" (login) só existe nesse menu, então **o funcionário não acha o login pelo celular** (precisa digitar `/login.html`). | `index.html:545`, `:554-559` | Alta | [Certo] |
| F2 | **Preços dos cards escritos à mão no HTML**, enquanto o formulário usa os preços do banco (`GET /api/servicos`). Hoje os valores batem com `banco/petagenda.sql:84-90`, mas, se o preço mudar no banco, a página mostra dois preços diferentes. | `index.html:616-651` × `:825-836` | Média | [Certo] |
| F3 | **Painel usa `confirm()` e `alert()` do navegador.** A caixa nativa não segue o visual do sistema, não diz qual agendamento será alterado (só o status) e o `alert('Erro ao atualizar o status.')` não mostra o motivo que a API envia. Pedir confirmação para "Sair da conta" é excessivo. | `dashboard.html:268` e `:277` (`mudarStatus`), `:286` (`fazerLogout`) | Média | [Certo] |
| F4 | **Erro de servidor no painel fica invisível.** Se a API falhar, o erro vai só para o console (`console.error`) e a tela continua em "⏳ Carregando dados do servidor..." para sempre. | `dashboard.html:204`, `:220` | Média | [Certo] |
| F5 | **Login mostra "Login ou senha incorretos!" para qualquer falha**, inclusive servidor desligado (o `catch` trata tudo igual). A mensagem também não some quando o usuário tenta de novo. | `login.html:142-159` | Média | [Certo] |
| F6 | **Botão "Solicitar Agendamento" não mostra que está enviando** e pode ser clicado duas vezes. O antichoque do servidor impede duplicidade (o 2º pedido recebe 409), mas o tutor vê uma mensagem de erro confusa logo depois do sucesso. | `index.html:868-902` | Baixa | [Certo] (código) / [Provável] (efeito) |
| F7 | **Mensagem de sucesso/erro (toast) some em 4 segundos** e não é anunciada por leitor de tela (falta `role="status"`/`aria-live`). | `index.html:788`, `:815-821` | Média | [Certo] |
| F8 | **Telefone sem máscara nem dica de formato.** O servidor rejeita telefone curto (CT-10), mas o tutor só descobre depois de enviar. | `index.html:714` | Baixa | [Certo] |
| F9 | **Conteúdo com classe `.reveal` começa invisível** (`opacity: 0`) e só aparece com JavaScript. Se o script falhar, seções inteiras (inclusive o formulário) ficam invisíveis. | `index.html:54-62`, `:797-807` | Baixa | [Certo] |
| F10 | **Painel no celular:** os 7 indicadores ficam empilhados antes da agenda; a tarefa principal (ver os atendimentos do dia) só aparece depois de ~2 telas de rolagem (ver `prints/dashboard-360.png`). Os botões do painel usam a fonte padrão do sistema, não a DM Sans (`button` não herda `font-family`). Valores acima de mil saem sem separador ("R$ 1500,00"). | `dashboard.html:106`, `:91-94`, `:178` (função `dinheiro`) | Baixa | [Certo] |

## 2. Honestidade do conteúdo (o que a banca pode questionar)

| # | Conteúdo | Onde | Por que é problema | Grau |
|---|---|---|---|---|
| H1 | "98% Satisfação", "0 Conflitos", "24h Online" | `index.html:590-603` | Nenhum desses números foi medido. "24h online" também não é verdade: o sistema roda em `localhost`. | [Certo] |
| H2 | "Rex – Banho & Tosa · Profissional: Ana · **Confirmado**" | `index.html:581-589` | O sistema **não escolhe profissional** (README, "Limitações") e **não existe o status "Confirmado"** (os status são Pendente, Em andamento, Concluído e Cancelado). | [Certo] |
| H3 | Lista "Por que escolher": "Histórico completo de atendimentos" e "Cancelamento com facilidade" | `index.html:696-703` | O tutor **não tem** tela de histórico nem de cancelamento (só o funcionário cancela no painel). "Sem conflitos de horários garantido" é verdade (antichoque). | [Certo] |
| H4 | Telefone do rodapé "(99) 9 9999-9999" | `index.html:778` | Placeholder que parece real. | [Certo] |
| H5 | "Profissionais qualificados", "Em menos de 2 minutos" | `index.html:612`, `:659` | Afirmações de marketing sem medição. "2 minutos" nunca foi cronometrado. | [Certo] |

**Como apresentar sem parecer dado real:** trocar o cartão do hero por uma **prévia ilustrativa** do que o sistema realmente faz (ex.: "Exemplo de agendamento · Banho · 14:00 · Pendente") com a etiqueta visível "Exemplo ilustrativo"; trocar os números por características verificáveis ("Antichoque por duração", "Só horários livres", "Alerta de saúde no painel"); remover da lista os itens não implementados; no rodapé, escrever "Telefone de exemplo" ou o contato real do pet shop parceiro. No texto do TCC, chamar a landing de **protótipo funcional**.

## 3. Avaliação heurística (Nielsen)

Escala de severidade de Nielsen (1994): **0** não é problema · **1** cosmético · **2** pequeno · **3** grave · **4** catastrófico.

| Heurística | Problema encontrado | Onde | Sev. | Correção sugerida |
|---|---|---|---|---|
| **H1 Visibilidade do status do sistema** | Envio do formulário sem indicador de "enviando"; toast some em 4 s; erros do painel não aparecem (fica "Carregando..." para sempre). | F4, F6, F7 | 3 | Desabilitar o botão e trocar o texto para "Enviando..."; toast com `role="status"` e tempo maior; mensagem de erro visível no painel. |
| **H2 Correspondência com o mundo real** | "Data/Horário **preferido**" sugere que o horário pode mudar, mas o sistema reserva exatamente o horário escolhido. Hero mostra status "Confirmado", que não existe. | `index.html:754-758`, H2 | 2 | Usar "Data" e "Horário disponível"; usar os mesmos nomes de status do painel. |
| **H3 Controle e liberdade do usuário** | Tutor não consegue cancelar nem rever o pedido, embora a página prometa "Cancelamento com facilidade". | H3 | 2 | Retirar a promessa e orientar: "Para cancelar, ligue para (telefone)". Cancelamento pelo tutor fica como trabalho futuro. |
| **H4 Consistência e padrões** | Três telas com estilos diferentes: botões arredondados (50 px) na landing e retos (8 px/6 px) no login e painel; o painel não usa a fonte de títulos (Playfair); dois vermelhos de erro (`#b3261e` e `#dc3545`); confirmações nativas no painel e toast na landing. | seção 8 | 2 | Um conjunto único de variáveis CSS (cores, raio, fontes) copiado nas três telas; mesmo componente de aviso (toast) em todas. |
| **H5 Prevenção de erros** | Telefone sem máscara/dica; botão pode ser clicado duas vezes. (Ponto positivo: só são oferecidos horários livres e a data mínima é hoje.) | F6, F8 | 2 | `inputmode="tel"`, dica "DDD + número", máscara simples em JS; desabilitar o botão durante o envio. |
| **H6 Reconhecer em vez de lembrar** | Ponto positivo: serviço mostra preço e duração no próprio select. No painel, o funcionário precisa lembrar o que cada cor de borda significa (o badge com texto resolve). | — | 1 | Manter. |
| **H7 Flexibilidade e eficiência** | No painel não há botões "dia anterior / próximo dia"; o funcionário precisa abrir o calendário toda vez. | `dashboard.html:128-132` | 1 | Botões "◀ Anterior" e "Próximo ▶" ao lado de "Hoje". |
| **H8 Estética e design minimalista** | Cartão do hero com números fictícios; no celular, 7 indicadores empurram a agenda para baixo. | H1, F10 | 2 | Hero honesto; no celular, agenda primeiro (ou indicadores recolhíveis). |
| **H9 Ajudar a reconhecer, diagnosticar e recuperar erros** | Login com mensagem igual para senha errada e servidor fora do ar; `alert` genérico no painel. (Ponto positivo: a landing mostra a mensagem da API, ex. "Este horário já está ocupado", e recarrega os horários após o 409.) | F3, F5 | 3 | Mensagens específicas: "Servidor indisponível, tente em instantes" × "Login ou senha incorretos". Mostrar `corpo.erro` da API. |
| **H10 Ajuda e documentação** | Não há contato real nem orientação sobre o que acontece depois do pedido (quem confirma, como). | H4 | 1 | Texto curto após o envio: "Seu pedido ficou **Pendente**. O pet shop confirma pelo telefone informado." |

## 4. Acessibilidade (WCAG 2.1, nível AA)

### 4.1 Contraste de cores (calculado)

Regra: texto normal precisa de **4,5:1**; texto grande (≥ 24 px, ou ≥ 18,66 px em negrito) precisa de **3:1** (critério 1.4.3). Bordas de campos e componentes precisam de **3:1** (critério 1.4.11).

| Uso | Cores | Razão | Exigido | Resultado |
|---|---|---|---|---|
| Texto principal sobre creme | `#2A2318` / `#FAF6EF` | 14,42:1 | 4,5 | Passa |
| Títulos verdes sobre creme | `#2D5016` / `#FAF6EF` | 8,59:1 | 4,5 | Passa |
| Texto `--muted` sobre creme | `#7A6A58` / `#FAF6EF` | 4,84:1 | 4,5 | Passa |
| Texto `--muted` sobre `--warm` (rótulos dos números do hero) | `#7A6A58` / `#F2E8D9` | 4,30:1 | 4,5 | **Falha** |
| Etiquetas laranja (`.section-label`, 12 px) sobre creme/branco | `#C8861A` / `#FAF6EF` | 2,83:1 | 4,5 | **Falha** |
| Selo "Pet Shop em Timbiras" (`.badge`) | `#C8861A` / `#F2E8D9` | 2,52:1 | 4,5 | **Falha** |
| Preço dos cards (≈21 px negrito = texto grande) | `#C8861A` / `#FAF6EF` | 2,83:1 | 3,0 | **Falha** |
| Botão "Solicitar Agendamento" (branco sobre laranja, 16 px) | `#FFFFFF` / `#C8861A` | 3,05:1 | 4,5 | **Falha** |
| Etiqueta laranja-claro sobre verde (seção Agendamento) e "Olá, Administrador" no painel | `#E8A030` / `#2D5016` | 4,18:1 | 4,5 | **Falha** (por pouco) |
| Rótulos do formulário (branco 75%) sobre verde | ≈`#CBD3C5` / `#2D5016` | 6,01:1 | 4,5 | Passa |
| Placeholder dos campos (branco 35%) | — | 2,25:1 | 4,5 | **Falha** |
| Borda dos campos do formulário (branco 20%) sobre verde | — | 1,74:1 | 3,0 | **Falha** (1.4.11) |
| Borda dos campos do login e filtro do painel | `#DDDDDD` / `#FFFFFF` | 1,36:1 | 3,0 | **Falha** (1.4.11) |
| Botão "Concluir" do painel | `#FFFFFF` / `#28A745` | 3,13:1 | 4,5 | **Falha** |
| Botões "Cancelar" e "Iniciar" do painel | branco / `#DC3545` · `#1F6FEB` | 4,53 · 4,63:1 | 4,5 | Passa |
| Rodapé (branco 60%) sobre `#2A2318` | — | 6,48:1 | 4,5 | Passa |

**Correção sugerida (baixo esforço), razões já calculadas:** criar um laranja escuro só para texto, `--amber-texto: #8A5A00` (5,50:1 sobre creme; 4,89:1 sobre `--warm`; 5,93:1 sobre branco) e usar `--amber` só em fundos e detalhes; botão do formulário com fundo `#9A6200` (branco = 5,10:1) ou texto escuro `#2A2318` sobre o laranja atual (5,09:1); laranja-claro sobre verde trocado por `#F2B860` (5,20:1); botão "Concluir" em `#1E7E34` (5,14:1); bordas dos campos `#8C8C8C` sobre branco (3,36:1) e branco 55% sobre verde (4,03:1); placeholder branco 60% (3,66:1, aceitável por ser dica, mas o ideal é 4,5:1).

### 4.2 Outros critérios

| Critério WCAG 2.1 | Situação | Onde | Grau |
|---|---|---|---|
| 1.1.1 Conteúdo não textual | Emojis usados como ícones (🔒, 🐾, 🛁, ✂️...) são lidos em voz alta pelo leitor de tela ("cadeado fechado", "pegadas"...). Devem ficar com `aria-hidden="true"` quando forem só decoração. | `index.html:557`, `:565`, `:579-583`, `:617-649`, `:770` | [Certo] |
| 1.3.1 Informação e relações | No login, `<label>` não está ligado ao campo (falta `for="login"` / `for="senha"`); o leitor de tela anuncia só o placeholder. Na landing, os rótulos envolvem o campo (correto). | `login.html:115-121` | [Certo] |
| 1.3.1 / boas práticas | Sem `<main>` nas três telas; na landing os passos usam `<h4>` logo após `<h2>` (pula nível); o login não tem `<h1>`. (Apontado pelo axe-core.) | — | [Certo] |
| 2.2.2 Pausar, parar, ocultar (nível A) | Animações infinitas: botão "Agendar agora" pulsando (`pulse 2.8s infinite`) e patas flutuando (`floatPaw 3s infinite`), sem forma de parar. | `index.html:203`, `:242` | [Provável] (depende de interpretar o pulso como "conteúdo em movimento") |
| 2.3.3 Animação por interação (AAA) / boa prática | Não existe `@media (prefers-reduced-motion: reduce)` nas páginas: quem configurou o sistema para "reduzir movimento" continua vendo todas as animações e a rolagem suave. | busca nos 3 HTML: 0 ocorrências | [Certo] |
| 2.4.1 Ignorar blocos | Não há link "Pular para o formulário". Baixo impacto porque o menu é curto. | — | [Certo] |
| 2.4.7 Foco visível | Links e botões mostram o contorno padrão do navegador (passa). Campos usam `outline: none` e indicam o foco só mudando a cor da borda — visível, mas fraco, sobretudo no login (`#ddd` → laranja). | `index.html:471`, `:475-478`; `login.html:75`, `:80` | [Certo] |
| 3.3.1 / 3.3.3 Identificação e sugestão de erro | Erros chegam só no toast, não junto do campo com problema. | F7, F8 | [Certo] |
| 4.1.3 Mensagens de status (AA) | Toast de sucesso/erro e mensagem de login sem `role="status"` ou `role="alert"`: o leitor de tela não anuncia. | `index.html:788`; `login.html:125` | [Certo] |

**Resultado do axe-core (1280 px, API simulada):** landing com violações de contraste (`color-contrast`, grave), ordem de títulos e ausência de regiões (`region`, moderado); login sem `<main>` e sem `<h1>`; painel com contraste do nome do usuário (4,18:1) e do botão Concluir (3,13:1). O axe não substitui a análise manual: parte do texto estava em animação durante o teste e foi medida à parte (tabela 4.1).

## 5. Responsividade (testada em 360, 768 e 1280 px)

| Largura | Landing | Login | Painel |
|---|---|---|---|
| 360 px | Sem rolagem horizontal (a correção do CT-13 funciona). **Menu inteiro some** (F1). Cartão do hero escondido. Formulário em 1 coluna, legível. | OK | Sem rolagem horizontal. Cabeçalho quebra em 2 linhas. **7 indicadores antes da agenda** (F10). Três botões de ação cabem lado a lado. |
| 768 px | Mesmo comportamento do celular (menu some até 900 px). Formulário em 1 coluna. | OK | Indicadores em 3-4 colunas; agenda em 2 colunas. OK. |
| 1280 px | OK. | OK | OK. |

Medido com `document.documentElement.scrollWidth = clientWidth` em todas as larguras (nenhuma rolagem horizontal). Prints em `auditoria/prints/` (arquivos `antes-*`).

## 6. LGPD na interface

O formulário coleta **nome e telefone do tutor** (dados pessoais, Lei 13.709/2018, art. 5º, I), além de dados do pet (espécie, raça, restrições de saúde do animal). Não há nenhum aviso de privacidade, link para política nem informação sobre finalidade (busca por "LGPD", "privacidade" e "consentimento" no `index.html`: 0 ocorrências). [Certo]

- **Transparência:** a lei dá ao titular direito a informações claras sobre o tratamento (art. 6º, VI, e art. 9º). Falta um aviso curto junto do botão de envio.
- **Base legal:** para marcar um atendimento pedido pelo próprio tutor, a base mais adequada tende a ser a "execução de contrato ou de procedimentos preliminares relacionados a contrato, a pedido do titular" (art. 7º, V), e não necessariamente o consentimento (art. 7º, I). [Provável — interpretação; conferir com a orientadora.] Mesmo assim, uma caixa "Li e concordo com o aviso de privacidade" é um jeito simples de **registrar a ciência** do tutor e é comum em formulários.
- **Restrição de saúde do pet:** dado sensível, na LGPD, é o dado de saúde **de pessoa natural** (art. 5º, II). A saúde do animal não entra nessa categoria. [Provável — interpretação do texto da lei.] Isso é útil para o TCC não exagerar o risco.
- **Direitos do titular:** a página deve dizer como o tutor pede acesso, correção ou exclusão dos seus dados (art. 18), ex.: "pelo telefone do pet shop".

Sugestão de HTML (só texto, não aplicado):

```html
<label class="consentimento">
  <input type="checkbox" id="aceitePrivacidade" required>
  Li o <a href="privacidade.html" target="_blank">aviso de privacidade</a> e concordo
  que o pet shop use meu nome e telefone para confirmar este agendamento.
</label>
```

## 7. Melhorias priorizadas (máximo 10)

| Ordem | Melhoria | Resolve | Esforço | Trecho sugerido |
|---|---|---|---|---|
| 1 | **Hero honesto:** trocar números e "Profissional: Ana" por exemplo ilustrativo com status real ("Pendente") e características verificáveis; remover promessas não implementadas da lista. | H1-H5, Nielsen H2/H8 | Baixo | `<span class="tag-exemplo">Exemplo ilustrativo</span>` + `stat-num` com "Por duração", "Só livres", "Alerta de saúde" |
| 2 | **Aviso de privacidade + caixa de ciência (LGPD)** e página `privacidade.html` curta (quem trata, para quê, por quanto tempo, como pedir exclusão). | Seção 6 | Baixo | ver seção 6 |
| 3 | **Menu no celular:** botão "☰ Menu" (`aria-expanded`) que abre os links, mantendo o acesso à Área Restrita. | F1, Nielsen H4 | Médio | `<button class="menu-toggle" aria-expanded="false" aria-controls="menu">` |
| 4 | **Contraste:** `--amber-texto` para textos laranja, botão do formulário com fundo mais escuro, bordas de campos mais fortes, Concluir em `#1E7E34` (valores na seção 4.1). | 4.1 | Baixo | `--amber-texto: #8A5A00;` |
| 5 | **Mensagens acessíveis e claras:** toast com `role="status"` (sucesso) / `role="alert"` (erro), duração maior; botão "Enviando..." desabilitado; login distinguindo servidor fora × senha errada. | F5-F7, Nielsen H1/H9, WCAG 4.1.3 | Baixo | `toast.setAttribute('role', ehErro ? 'alert' : 'status')` |
| 6 | **Painel sem `alert/confirm`:** diálogo próprio (`<dialog>`) que mostra tutor, pet e horário antes de cancelar; sair sem confirmação; erro da API visível na tela. | F3, F4, Nielsen H3/H9 | Médio | `<dialog id="confirmacao">` |
| 7 | **Preços dos cards vindos da API**, usando o mesmo `fetch('/api/servicos')` que já existe. | F2 | Baixo | gerar os cards com `document.createElement` e `textContent` |
| 8 | **`prefers-reduced-motion`** desligando pulso, patas, `reveal` e rolagem suave; conteúdo `.reveal` visível sem JS. | WCAG 2.2.2/2.3.3, F9 | Baixo | `@media (prefers-reduced-motion: reduce) { *{animation:none!important;transition:none!important} html{scroll-behavior:auto} .reveal{opacity:1;transform:none} }` |
| 9 | **Semântica:** `<main>`, `<h1>` no login, `for` nos rótulos do login, `aria-hidden` nos emojis decorativos, `<h3>` nos passos. | 4.2 | Baixo | `<label for="login">` |
| 10 | **Painel no celular e navegação por dia:** agenda antes dos indicadores em telas pequenas; botões "Anterior/Próximo"; telefone formatado; `font-family: inherit` nos botões. | F10, Nielsen H7 | Médio | `@media (max-width:600px){ .container{display:flex;flex-direction:column} #agenda{order:-1} }` |

Itens 1, 2, 4, 5, 7, 8 e 9 são baratos e cabem antes de 27/11. Itens 3, 6 e 10 também cabem, mas exigem mais teste.

## 8. Consistência visual entre as telas

| Elemento | Landing | Login | Painel |
|---|---|---|---|
| Fonte de títulos | Playfair Display | Playfair (só no logo) | **não carrega Playfair** (título em DM Sans) |
| Raio dos botões | 50 px (pílula) | 8 px | 6-8 px |
| Cor de erro | `#b3261e` (toast) | `#dc3545` | `#dc3545` |
| Variáveis `:root` | 10 cores | 5 cores (`--muted` repetido como `#7A6A58` fixo) | 6 cores (`#E8A030` fixo no código) |
| Avisos | toast próprio | texto vermelho | `alert`/`confirm` nativos |

**Sugestão:** extrair as variáveis e os botões para um arquivo `estilo-base.css` usado pelas três telas. Isso também facilita explicar no TCC a ideia de **consistência** (heurística H4) e de **identidade visual** do produto.
