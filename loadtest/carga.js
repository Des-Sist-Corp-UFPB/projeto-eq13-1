import http from 'k6/http';
import { check, sleep, group } from 'k6';

// ─────────────────────────────────────────────────────────────────────────────
// Teste de carga e performance — k6  (RadarTech PB)
//
// IMPORTANTE: rode contra o SEU AMBIENTE LOCAL (suba o projeto com
// docker-compose antes). NÃO aponte para https://eqNN.dsc.rodrigor.com — o
// servidor e o PostgreSQL são compartilhados com as outras equipes.
// ─────────────────────────────────────────────────────────────────────────────

// URL base do seu ambiente local. Ajuste a PORTA conforme o seu docker-compose.
const BASE = __ENV.BASE_URL || 'http://localhost:8080';

// Nº de usuários virtuais simultâneos. Sobrescreva pela linha de comando:
//   k6 run -e VUS=20 -e BASE_URL=http://localhost:8080 loadtest/carga.js
const VUS = Number(__ENV.VUS || 10);

export const options = {
  stages: [
    { duration: '30s', target: VUS },   // sobe a carga gradualmente
    { duration: '1m',  target: VUS },   // mantém a carga
    { duration: '20s', target: 0 },     // desaquece
  ],
  thresholds: {
    http_req_failed:   ['rate<0.01'],   // meta: menos de 1% de falhas
    http_req_duration: ['p(95)<500'],   // meta: 95% das respostas < 500 ms
  },
};

export default function () {
  // 1. Health check
  group('healthcheck', () => {
    const res = http.get(`${BASE}/ping`);
    check(res, { 'ping status 200': (r) => r.status === 200 });
  });

  // 2. Página inicial
  group('pagina-inicial', () => {
    const res = http.get(`${BASE}/`);
    check(res, { 'home status 200': (r) => r.status === 200 });
  });

  // 3. Listagem pública de vagas
  group('listagem-vagas', () => {
    const res = http.get(`${BASE}/vagas`);
    check(res, { 'vagas status 200': (r) => r.status === 200 });
  });

  // 4. Busca de vagas com filtro
  group('busca-vagas', () => {
    const res = http.get(`${BASE}/vagas?q=estagio&location=REMOTE`);
    check(res, { 'busca status 200': (r) => r.status === 200 });
  });

  // 5. Página de login
  group('login-page', () => {
    const res = http.get(`${BASE}/login`);
    check(res, { 'login status 200': (r) => r.status === 200 });
  });

  // 6. Página de cadastro
  group('cadastro-page', () => {
    const res = http.get(`${BASE}/cadastro`);
    check(res, { 'cadastro status 200': (r) => r.status === 200 });
  });

  // 7. Formulário de divulgação de vagas
  group('divulgar-page', () => {
    const res = http.get(`${BASE}/divulgar`);
    check(res, { 'divulgar status 200': (r) => r.status === 200 });
  });

  sleep(1);
}
