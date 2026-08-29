create table if not exists beacons (
    id    text,
    ts    bigint,
    lat   double precision,
    lon   double precision,
    sdk   int,
    batt  int,
    model text
);

alter table beacons enable row level security;

create policy "anon insert" on beacons
    for insert to anon with check (true);

create policy "anon select" on beacons
    for select to anon using (true);

create index if not exists beacons_id_ts on beacons (id, ts desc);