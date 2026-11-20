# 14. Polimento visual para apresentações (madrugada de 18/11/2026)

Objetivo pedido pela dupla: deixar o site "apresentável e vendável" para as apresentações presenciais, sem mudar as funções.

## O que mudou

| Onde | Antes | Depois | Por quê |
|---|---|---|---|
| Topo da página inicial | O cartão de exemplo à direita ficava baixo demais e era cortado em telas baixas (ex.: projetor 1366×768). Causa: a animação de entrada sobrescrevia o `translateY(-50%)` que centralizava o cartão | O topo virou um grid de duas colunas; o cartão fica centralizado em qualquer altura de tela | **Bug visual** encontrado no teste da noite |
| Cartão de exemplo | Mostrava só serviço e status | Mostra também "com Ana" e um código de exemplo, que são as novidades da Etapa 3. Continua com a etiqueta "Exemplo ilustrativo" | Mostrar na apresentação o que o sistema faz de diferente |
| Patinhas | Emoji 🐾 (azul em alguns sistemas, fora da paleta) | Desenho próprio (SVG) na cor âmbar da marca | Identidade visual |
| Serviços | 4 colunas: 6 cards viravam 4 + 2 | 3 colunas (3 + 3); ícone dentro de um selo branco; preço sempre alinhado no fim do card | Equilíbrio visual |
| Seção "Agende um horário" | Coluna da esquerda vazia abaixo da lista | Caixa com o horário de atendimento e o atalho para Meus agendamentos; a coluna acompanha a rolagem | Aproveitar o espaço com informação útil |
| Menu no celular | Os links sumiam abaixo de 900 px (pendência da auditoria) | Botão "Menu" que abre a lista; fecha ao escolher um link ou com Esc; usa `aria-expanded` | Usabilidade no celular (Nielsen: controle e liberdade) |
| Ícone da aba (favicon) | Não tinha | Patinha âmbar sobre verde em todas as páginas | Aparência profissional na aba do navegador |
| Página inicial | Sem descrição | `meta description` e `theme-color` | Aparência ao compartilhar o link e na barra do celular |
| Login | Botão com fonte padrão do navegador e cantos diferentes do resto | Mesma fonte e botão arredondado do site; fundo com o mesmo degradê do topo | Consistência (Nielsen: consistência e padrões) |
| Painel | "Serviço mais procurado" quebrava em três linhas; lista vazia era só um texto | O card ocupa duas colunas; o aviso de dia vazio orienta a usar Anterior/Próximo | Leitura e orientação |

## Verificação

- axe-core (WCAG 2.1 A/AA e boas práticas): **0 problemas** na página inicial (computador e celular), login e painel.
- Sem rolagem lateral em 390 px.
- Roteiro automático: 19 passaram, 0 falharam. Guia de tela: 26 de 26 passos passaram com o banco recém-criado.

Prints em `auditoria/prints/polimento/` (antes e depois do topo, menu no celular, serviços, painel).
