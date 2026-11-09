CREATE TABLE IF NOT EXISTS bus_stops (
  node_id TEXT PRIMARY KEY,
  node_no TEXT,
  node_nm TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS bus_routes (
  route_id TEXT PRIMARY KEY,
  route_no TEXT NOT NULL,
  route_type INTEGER
);
