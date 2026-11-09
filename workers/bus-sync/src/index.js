// shortcut: 팀장님 Worker(광주 전용, 키 불필요)를 원본으로 씀. 이 Worker가 중단되면 적재도 실패, TAGO 직접 호출로 되돌릴 땐 이 커밋 이전 버전 참고
const SOURCE = "https://gwangjubustaja-api.dawon1703.workers.dev";

const v = (x) => x ?? null;

async function getList(fetchImpl, path, key) {
  const res = await fetchImpl(`${SOURCE}${path}`);
  if (!res.ok) throw new Error(`${path}: HTTP ${res.status}`);
  const json = await res.json();
  if (json.status !== "ok" || !Array.isArray(json[key])) {
    throw new Error(`${path}: ${JSON.stringify(json).slice(0, 200)}`);
  }
  return json[key];
}

export async function syncAll(env, fetchImpl = fetch) {
  const stations = await getList(fetchImpl, "/stations", "stations");
  const routes = await getList(fetchImpl, "/routes", "routes");
  // 빈 응답으로 기존 데이터를 지우지 않도록 막음
  if (stations.length === 0 || routes.length === 0) {
    throw new Error(`빈 응답이라 DB를 건드리지 않음: stations=${stations.length} routes=${routes.length}`);
  }

  // D1 batch는 트랜잭션: 교체 도중 실패하면 전부 롤백
  const db = env.DB;
  await db.batch([
    db.prepare("DELETE FROM bus_stops"),
    ...stations.map((s) =>
      db.prepare("INSERT OR REPLACE INTO bus_stops VALUES (?,?,?)").bind(v(s.id), v(s.no), v(s.name))),
    db.prepare("DELETE FROM bus_routes"),
    ...routes.map((r) =>
      db.prepare("INSERT OR REPLACE INTO bus_routes VALUES (?,?,?)").bind(v(r.id), v(r.name), v(r.type))),
  ]);
  const result = { stops: stations.length, routes: routes.length };
  console.log("bus sync ok", result);
  return result;
}

export default {
  // 매일 UTC 18:00 = KST 03:00 (wrangler.toml의 crons)
  async scheduled(controller, env, ctx) {
    ctx.waitUntil(syncAll(env));
  },
};
