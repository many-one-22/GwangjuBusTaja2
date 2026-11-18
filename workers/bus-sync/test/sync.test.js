import { test } from "node:test";
import assert from "node:assert/strict";
import { syncAll } from "../src/index.js";

const json = (obj, status = 200) => new Response(JSON.stringify(obj), { status });

function fakeApi({ stations, routes }) {
  return async (url) => {
    const path = new URL(url).pathname;
    if (path === "/stations") return json({ status: "ok", stations });
    if (path === "/routes") return json({ status: "ok", routes });
    throw new Error(`unexpected ${path}`);
  };
}

function fakeDb() {
  const batches = [];
  const db = {
    prepare: (sql) => ({ sql, bind: (...args) => ({ sql, args }) }),
    batch: async (stmts) => { batches.push(stmts); return []; },
  };
  return { db, batches };
}

const stationRows = (n) => Array.from({ length: n }, (_, i) => ({ id: `KJB${i}`, name: `정류소${i}`, no: i % 4 === 0 ? undefined : String(5000 + i) }));

test("정류소·노선 목록을 받아 한 batch로 교체", async () => {
  const { db, batches } = fakeDb();
  const routes = [{ id: "KJB188", name: "72", type: 3 }, { id: "KJB189", name: "73", type: -1 }];

  const result = await syncAll({ DB: db }, fakeApi({ stations: stationRows(4064), routes }));

  assert.deepEqual(result, { stops: 4064, routes: 2 });
  assert.equal(batches.length, 1);
  const stmts = batches[0];
  assert.equal(stmts.filter((s) => s.sql.startsWith("DELETE")).length, 2);
  assert.equal(stmts.filter((s) => s.sql.startsWith("INSERT")).length, 4066);
  // no가 없는 정류소도 null로 들어가야 함
  assert.ok(stmts.some((s) => s.args && s.args[1] === null));
});

test("빈 응답이면 DB를 건드리지 않음", async () => {
  const { db, batches } = fakeDb();
  await assert.rejects(
    syncAll({ DB: db }, fakeApi({ stations: [], routes: [] })),
    /DB를 건드리지 않음/,
  );
  assert.equal(batches.length, 0);
});

test("status가 ok가 아니면 실패", async () => {
  const { db, batches } = fakeDb();
  const fail = async () => json({ status: "error", reason: "UNKNOWN", message: "not found" });
  await assert.rejects(syncAll({ DB: db }, fail), /not found/);
  assert.equal(batches.length, 0);
});
