/* PetAgenda – funções usadas por todas as páginas públicas.
   Depende do Bootstrap (vendor/bootstrap/bootstrap.bundle.min.js). */
(function () {
  'use strict';

  // Navbar ganha sombra ao rolar; botão flutuante aparece depois do topo
  const nav = document.querySelector('.navbar-pa');
  const flutuante = document.querySelector('.flutuante-agendar');
  function aoRolar() {
    const y = window.scrollY;
    if (nav) nav.classList.toggle('rolou', y > 20);
    if (flutuante) flutuante.classList.toggle('visivel', y > 600);
  }
  window.addEventListener('scroll', aoRolar, { passive: true });
  aoRolar();

  // Fecha o menu do celular ao clicar num link (navbar collapse do Bootstrap)
  document.querySelectorAll('.navbar-pa .navbar-collapse a').forEach(a => a.addEventListener('click', () => {
    const menu = document.querySelector('.navbar-pa .navbar-collapse.show');
    if (menu && window.bootstrap) bootstrap.Collapse.getOrCreateInstance(menu).hide();
  }));

  // Revela os blocos ao rolar (sem JS, a classe "js" não existe e tudo fica visível)
  const revelar = document.querySelectorAll('.revelar');
  if ('IntersectionObserver' in window) {
    const obs = new IntersectionObserver(entradas => {
      entradas.forEach((e, i) => {
        if (e.isIntersecting) {
          setTimeout(() => e.target.classList.add('visivel'), i * 90);
          obs.unobserve(e.target);
        }
      });
    }, { threshold: 0.12 });
    revelar.forEach(el => obs.observe(el));
  } else {
    revelar.forEach(el => el.classList.add('visivel'));
  }

  // Ano no rodapé
  document.querySelectorAll('[data-ano]').forEach(el => { el.textContent = new Date().getFullYear(); });
})();

/** Cria um elemento com texto. textContent (e não innerHTML) impede XSS. */
function el(tag, classe, texto) {
  const e = document.createElement(tag);
  if (classe) e.className = classe;
  if (texto !== undefined && texto !== null) e.textContent = texto;
  return e;
}

/** Ícone do Bootstrap Icons (decorativo, escondido do leitor de tela). */
function icone(nome, extra) {
  const i = document.createElement('i');
  i.className = 'bi bi-' + nome + (extra ? ' ' + extra : '');
  i.setAttribute('aria-hidden', 'true');
  return i;
}

/**
 * Aviso flutuante (toast do Bootstrap). Erro usa role="alert" (anúncio imediato);
 * sucesso usa role="status". Fica 7 s na tela (heurística H1 de Nielsen).
 */
function mostrarToast(mensagem, ehErro) {
  const toast = document.getElementById('toast');
  if (!toast) return;
  toast.setAttribute('role', ehErro ? 'alert' : 'status');
  toast.setAttribute('aria-live', ehErro ? 'assertive' : 'polite');
  toast.className = 'toast toast-pa align-items-center ' + (ehErro ? 'erro' : 'ok');
  const corpo = toast.querySelector('.toast-body');
  corpo.replaceChildren(icone(ehErro ? 'exclamation-triangle-fill' : 'check-circle-fill', 'me-2'), document.createTextNode(mensagem));
  bootstrap.Toast.getOrCreateInstance(toast, { delay: 7000 }).show();
}

/** "2026-11-20T14:30" -> "20/11/2026 às 14:30" */
function dataHoraBR(iso) {
  const [d, h] = iso.split('T');
  return d.split('-').reverse().join('/') + (h ? ' às ' + h : '');
}

function reais(v) {
  return Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}
