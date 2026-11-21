# 16. Nova interface com Bootstrap (21/11/2026)

## O que foi feito

A interface das cinco páginas foi refeita com **Bootstrap 5.3**, como previa o projeto original do TCC
(HTML5, CSS3, JavaScript e Bootstrap na apresentação; Java/Spring Boot no negócio; MySQL por JDBC).
O back-end (Java, banco e API) **não mudou**: nenhuma linha de Java nem de SQL foi alterada.

A estrutura e o fluxo da página inicial seguem os padrões de páginas comerciais de pet shop
(referência de estrutura: a página de criação de sites para pet shop da Crivos). Nenhum texto,
imagem ou marca foi copiado. A **paleta de cores** é a mesma do PetAgenda desde a Etapa 2.

### Arquivos novos
| Arquivo | Para que serve |
|---|---|
| `static/vendor/bootstrap/` | Bootstrap 5.3.3 (CSS e JS, licença MIT) |
| `static/vendor/bootstrap-icons/` | Bootstrap Icons 1.11.3 (licença MIT) |
| `static/vendor/fonts/` | Fontes Nunito e DM Sans (licença OFL) |
| `static/css/petagenda.css` | Tema: troca as cores do Bootstrap pela paleta do PetAgenda e define os componentes próprios |
| `static/js/petagenda.js` | Funções comuns das páginas públicas (menu, animação ao rolar, aviso, datas, preços) |
| `static/img/favicon.svg` | Ícone da aba |

Tudo é servido pelo próprio sistema, **sem CDN e sem Google Fonts**: o site abre igual numa
apresentação sem internet.

### Página inicial: nova estrutura
1. Barra de informações (cidade, horário, link da área da equipe)
2. Menu fixo com o botão "Agendar agora" (vira menu sanduíche no celular, componente *navbar*)
3. Topo em duas colunas: título, chamada, lista de vantagens e um celular ilustrativo com um pedido de exemplo
4. "Por que agendar online": 3 cartões
5. Serviços (vêm do banco), agora com botão "Agendar" em cada cartão, que já escolhe o serviço no formulário
6. Faixa verde "Muito além da agenda": 4 recursos (alerta de saúde, sem choque, dados protegidos, celular)
7. Comparativo "Agenda de papel e WhatsApp" × "Com o PetAgenda" (o AS-IS × TO-BE do TCC)
8. Números verdadeiros: quantidade de serviços (do banco), 08h–18h, 30 min, 24 h
9. Como funciona: 4 passos
10. Formulário dividido em 4 etapas (seus dados, pet, serviço e horário, privacidade) com a coluna "Antes de agendar"
11. Perguntas frequentes (componente *accordion*): inclui "Posso escolher o profissional?" e "Pago pelo site?"
12. Chamada final e rodapé em 4 colunas
13. Botões flutuantes "Agendar" e "Voltar ao topo"

### Outras páginas
- **Meus agendamentos:** faixa de título com trilha de navegação (*breadcrumb*), formulário ao lado da lista, cancelamento em *modal* do Bootstrap.
- **Login:** tela dividida (lado verde com o que o painel faz, lado do formulário), botão de mostrar a senha.
- **Privacidade:** mesmo texto, organizado em blocos.
- **Painel:** menu lateral (vira *offcanvas* no celular), topo com o usuário e menu suspenso (trocar senha, sair), indicadores com ícones, cartões da agenda, tabelas do Bootstrap, *modal* de confirmação e *toast* para os avisos.

### Regras preservadas
- O tutor **não escolhe** o profissional; cada serviço mostra o responsável (cartão, formulário e confirmação).
- Pagamento continua fora do sistema (a FAQ diz que se paga no pet shop).
- Nenhum número inventado: o celular do topo tem a etiqueta "Exemplo ilustrativo".
- Todos os textos que vêm do banco continuam com `textContent` (proteção contra XSS).
- Todos os IDs usados pelos testes e pelo guia continuam os mesmos.

## Testes (21/11/2026, servidor Linux na nuvem, MariaDB 10.11)
| Teste | Resultado |
|---|---|
| Testes de unidade (JUnit) | 26 passaram, 0 falharam |
| Roteiro `testes/TestesCT.java` (CT-01 a CT-19) | 19 passaram, 0 falharam, 2 manuais |
| Fluxo de tela automatizado (agendar, código, horário some, Meus agendamentos, modal, login errado, alerta de saúde, mudar status, histórico, serviço, bloqueio, senha, sair, perfil Atendente, menu do celular) | 18 de 18 passos, sem erro de JavaScript |
| axe-core 4.14 (WCAG 2.0/2.1 A e AA) em todas as páginas e abas do painel | 0 problemas |
| Celular (390 px) | sem rolagem lateral em nenhuma página |

Prints em `auditoria/prints/bootstrap/` (01 a 24, computador 1366×768 e celular 390 px).
O campo de data aparece como mm/dd/aaaa nos prints porque o navegador automático estava em inglês;
num navegador em português ele aparece como dd/mm/aaaa.

## O que atualizar no texto do TCC (entrega de 27/11)

### Trechos
| Onde (no `tcc.md` / .docx) | O que diz hoje | O que passa a dizer |
|---|---|---|
| Resumo | "interface em HTML5, CSS3 e JavaScript, sem framework de interface" | "interface em HTML5, CSS3, JavaScript e o framework Bootstrap" |
| Abstract | "without an interface framework" | "and the Bootstrap framework" |
| Resumo e Abstract | "25 testes de unidade" | "26 testes de unidade" (conferir a contagem com a rodada no PC) |
| Seção 3, camadas, item a) apresentação | "folhas de estilo próprias e consultas de mídia [...] sem uso de framework de interface" | "Bootstrap 5, com seu sistema de grade responsiva e componentes (menu, modal, accordion, toast), e uma folha de estilo própria que aplica a paleta do PetAgenda" |
| Quadro "Previsto × implementado", linha Apresentação | "HTML5, CSS3 e JavaScript, sem framework" | "HTML5, CSS3, JavaScript e Bootstrap 5.3, como previsto" |
| Parágrafo logo abaixo do quadro | "A troca do Bootstrap por estilos próprios e do Spring Data pelo JdbcTemplate..." | Tirar o Bootstrap da frase: só a troca do Spring Data pelo JdbcTemplate continua valendo |
| Seção 4.3, testes de unidade | "25 testes de unidade" | "26" |
| Quadro de usabilidade / Apêndice C (axe-core) | "0 em todas as páginas [...] também depois do polimento" | Acrescentar: "e 0 na nova interface com Bootstrap (21/11/2026)" |
| Considerações finais / trabalhos futuros | Se citar "adotar um framework de interface" | Remover |

Observação: o `pom.xml` usa o `spring-boot-starter-data-jdbc` (Spring Data JDBC), mas o código acessa o
banco pelo `JdbcTemplate`. Se a orientadora perguntar do Spring Data, dá para dizer que ele está no
projeto como dependência e que a persistência foi feita com o JdbcTemplate.

### Prints do Apêndice E
| Figura | Print atual | Trocar por |
|---|---|---|
| E.1 Página inicial (computador) | `depois-topo-1366x768.png` | `01-inicio-topo.png` |
| E.2 Menu recolhível no celular | `depois-menu-celular.png` | `20-celular-menu.png` |
| E.3 Confirmação com o código | `03-confirmacao-com-codigo.png` | `08-confirmacao-codigo.png` |
| E.4 Meus agendamentos | `05-meus-agendamentos-lista.png` | `09-meus-agendamentos.png` |
| E.5 Painel do administrador | `depois-painel.png` | `13-painel-agenda.png` |
| E.6 Horários a partir das 12:00 | `14-horarios-a-partir-12h.png` | `23-horarios-com-bloqueio.png` |
| E.7 Painel do Atendente | `15-painel-atendente-2-abas.png` | `24-painel-atendente.png` |

Sugestões de figuras novas, se couber: `04-comparativo-numeros.png` (o AS-IS × TO-BE na própria página),
`10-dialogo-cancelar.png`, `12-login.png` e `22-celular-painel-menu.png`.

Os prints antigos de antes e depois da auditoria (17/11) continuam válidos para o Apêndice C, porque
mostram a evolução. Só as telas "atuais" do Apêndice E precisam ser trocadas.
